package ch.numnia.creatures.service;

import ch.numnia.creatures.domain.*;
import ch.numnia.creatures.spi.CompanionRepository;
import ch.numnia.creatures.spi.CreatureAuditRepository;
import ch.numnia.creatures.spi.CreatureCatalog;
import ch.numnia.creatures.spi.CreatureInventoryRepository;
import ch.numnia.learning.domain.LearningProgress;
import ch.numnia.learning.domain.MasteryStatus;
import ch.numnia.learning.spi.LearningProgressRepository;
import ch.numnia.learning.spi.StarPointsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Orchestrator for UC-006 — child unlocks a creature and picks it as companion.
 *
 * <p>Pure server-side rules:
 * <ul>
 *   <li>BR-001: unlocks are permanent (never removed; idempotent recording).</li>
 *   <li>BR-002: variable name endings accepted (validated in {@link Creature}).</li>
 *   <li>BR-003: companion swap allowed at any time (no cooldown).</li>
 *   <li>Alternative flow 1a: when the threshold is reached but all R1
 *       creatures are already unlocked, a consolation reward is granted
 *       (FR-GAM-001).</li>
 * </ul>
 *
 * <p>Audit-only logging (no PII; child identification by UUID only).
 */
@Service
public class CreatureService {

    /** Consolation star points when threshold is reached but inventory is full (alt 1a). */
    public static final int CONSOLATION_STAR_POINTS = 50;

    private static final Logger log = LoggerFactory.getLogger(CreatureService.class);

    private final CreatureCatalog catalog;
    private final CreatureInventoryRepository inventory;
    private final CompanionRepository companions;
    private final CreatureAuditRepository audit;
    private final LearningProgressRepository progressRepo;
    private final StarPointsRepository starPoints;
    private final Clock clock;

    @Autowired
    public CreatureService(CreatureCatalog catalog,
                           CreatureInventoryRepository inventory,
                           CompanionRepository companions,
                           CreatureAuditRepository audit,
                           LearningProgressRepository progressRepo,
                           StarPointsRepository starPoints) {
        this(catalog, inventory, companions, audit, progressRepo, starPoints, Clock.systemUTC());
    }

    public CreatureService(CreatureCatalog catalog,
                           CreatureInventoryRepository inventory,
                           CompanionRepository companions,
                           CreatureAuditRepository audit,
                           LearningProgressRepository progressRepo,
                           StarPointsRepository starPoints,
                           Clock clock) {
        this.catalog = catalog;
        this.inventory = inventory;
        this.companions = companions;
        this.audit = audit;
        this.progressRepo = progressRepo;
        this.starPoints = starPoints;
        this.clock = clock;
    }

    /**
     * Process unlocks for the given child based on current learning progress.
     * Idempotent: re-running after a unlock yields an empty result.
     */
    public CreatureUnlockResult processUnlocks(UUID childId) {
        requireChildId(childId);
        Set<String> already = inventory.unlockedSet(childId);
        List<Creature> newly = new ArrayList<>();
        boolean thresholdMet = false;

        for (Creature creature : catalog.listAll()) {
            var progressOpt = progressRepo.findByChildAndOperation(childId, creature.operation());
            if (progressOpt.isEmpty()) {
                continue;
            }
            LearningProgress progress = progressOpt.get();
            if (progress.masteryStatus() != MasteryStatus.MASTERED) {
                continue;
            }
            thresholdMet = true;
            if (already.contains(creature.id())) {
                continue;
            }
            CreatureUnlock unlock = new CreatureUnlock(childId, creature.id(), clock.instant());
            inventory.recordUnlock(unlock);
            audit.append(new CreatureAuditEntry(
                    childId, CreatureAuditAction.CREATURE_UNLOCKED,
                    creature.id(), clock.instant()));
            newly.add(creature);
        }

        boolean consolation = false;
        int starsAwarded = 0;
        if (newly.isEmpty() && thresholdMet
                && inventory.unlockedSet(childId).size() >= catalog.listAll().size()) {
            // Alt 1a: all creatures unlocked but mastery still firing → consolation.
            starPoints.addPoints(childId, CONSOLATION_STAR_POINTS);
            audit.append(new CreatureAuditEntry(
                    childId, CreatureAuditAction.CREATURE_UNLOCK_CONSOLATION_AWARDED,
                    null, clock.instant()));
            consolation = true;
            starsAwarded = CONSOLATION_STAR_POINTS;
        }
        if (!newly.isEmpty()) {
            log.info("child={} unlocked {} creature(s)", childId, newly.size());
        }
        return new CreatureUnlockResult(newly, consolation, starsAwarded);
    }

    /** UC-006 step 4: gallery view (locked + unlocked, with companion flag). */
    public List<GalleryEntry> listGallery(UUID childId) {
        requireChildId(childId);
        Set<String> unlocked = inventory.unlockedSet(childId);
        String companion = companions.findCompanion(childId).orElse(null);
        return catalog.listAll().stream()
                .map(c -> new GalleryEntry(
                        c,
                        unlocked.contains(c.id()),
                        c.id().equals(companion)))
                .toList();
    }

    /** UC-006 step 5: pick a creature as the active companion (BR-003). */
    public void pickCompanion(UUID childId, String creatureId) {
        requireChildId(childId);
        Creature creature = catalog.findById(creatureId)
                .orElseThrow(() -> new UnknownCreatureException(creatureId));
        if (!inventory.unlockedSet(childId).contains(creature.id())) {
            audit.append(new CreatureAuditEntry(
                    childId, CreatureAuditAction.COMPANION_PICK_REJECTED_NOT_UNLOCKED,
                    creature.id(), clock.instant()));
            throw new CompanionNotUnlockedException(creature.id());
        }
        companions.setCompanion(childId, creature.id());
        audit.append(new CreatureAuditEntry(
                childId, CreatureAuditAction.COMPANION_CHANGED,
                creature.id(), clock.instant()));
    }

    public java.util.Optional<String> currentCompanion(UUID childId) {
        requireChildId(childId);
        return companions.findCompanion(childId);
    }

    private static void requireChildId(UUID childId) {
        if (childId == null) {
            throw new IllegalArgumentException("childId must not be null");
        }
    }
}
