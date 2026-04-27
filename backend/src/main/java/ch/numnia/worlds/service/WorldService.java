package ch.numnia.worlds.service;

import ch.numnia.learning.domain.LearningProgress;
import ch.numnia.learning.domain.Operation;
import ch.numnia.learning.spi.LearningProgressRepository;
import ch.numnia.learning.spi.TaskPoolRepository;
import ch.numnia.worlds.domain.PortalEntry;
import ch.numnia.worlds.domain.PortalType;
import ch.numnia.worlds.domain.World;
import ch.numnia.worlds.domain.WorldAuditAction;
import ch.numnia.worlds.domain.WorldAuditEntry;
import ch.numnia.worlds.spi.ChildPreferencesRepository;
import ch.numnia.worlds.spi.WorldAuditRepository;
import ch.numnia.worlds.spi.WorldCatalog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

/**
 * Orchestrator for UC-005 — child enters a world through a portal.
 *
 * <p>Pure server-side rule check: only {@link PortalType#TRAINING} is
 * openable in Release 1 (BR-001). Each attempt is audited regardless of
 * outcome. The reduced-motion accessibility flag (NFR-A11Y-003) is read
 * from {@link ChildPreferencesRepository} and surfaced verbatim on the
 * {@link PortalEntry} so the React shell can lower particle effects.
 */
@Service
public class WorldService {

    private static final Logger log = LoggerFactory.getLogger(WorldService.class);

    private final WorldCatalog catalog;
    private final ChildPreferencesRepository prefs;
    private final WorldAuditRepository auditRepo;
    private final LearningProgressRepository progressRepo;
    private final TaskPoolRepository taskPools;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public WorldService(WorldCatalog catalog,
                        ChildPreferencesRepository prefs,
                        WorldAuditRepository auditRepo,
                        LearningProgressRepository progressRepo,
                        TaskPoolRepository taskPools) {
        this(catalog, prefs, auditRepo, progressRepo, taskPools, Clock.systemUTC());
    }

    public WorldService(WorldCatalog catalog,
                        ChildPreferencesRepository prefs,
                        WorldAuditRepository auditRepo,
                        LearningProgressRepository progressRepo,
                        TaskPoolRepository taskPools,
                        Clock clock) {
        this.catalog = catalog;
        this.prefs = prefs;
        this.auditRepo = auditRepo;
        this.progressRepo = progressRepo;
        this.taskPools = taskPools;
        this.clock = clock;
    }

    /** R1 BR-001: list the (exactly three) worlds available in this release. */
    public List<World> listWorlds() {
        return catalog.listAll();
    }

    /**
     * UC-005 main flow: attempt to enter a portal.
     *
     * <p>Always returns a {@link PortalEntry} (never throws on rule
     * violations); caller inspects {@link PortalEntry#locked()}.
     */
    public PortalEntry openPortal(UUID childId, String worldId, PortalType type) {
        if (childId == null) {
            throw new IllegalArgumentException("childId must not be null");
        }
        World world = catalog.findById(worldId)
                .orElseThrow(() -> new UnknownWorldException(worldId));

        boolean reducedMotion = prefs.isReducedMotion(childId);
        if (reducedMotion) {
            audit(childId, worldId, type, WorldAuditAction.REDUCED_MOTION_APPLIED);
        }

        if (!type.availableInRelease1()) {
            audit(childId, worldId, type, WorldAuditAction.PORTAL_LOCKED_RELEASE_RULE);
            log.info("portal.locked release-rule childRef={} world={} portal={}",
                    childId, worldId, type);
            return PortalEntry.locked(worldId, type, PortalEntry.CODE_COMING_LATER, reducedMotion);
        }

        if (!isAnyTaskPoolConfigured(worldId)) {
            audit(childId, worldId, type, WorldAuditAction.PORTAL_LOCKED_TASK_POOL_MISSING);
            log.info("portal.locked task-pool-missing childRef={} world={}", childId, worldId);
            return PortalEntry.locked(worldId, type, PortalEntry.CODE_TASK_POOL_MISSING, reducedMotion);
        }

        int childLevel = highestLevelOf(childId);
        if (childLevel < world.requiredLevel()) {
            audit(childId, worldId, type, WorldAuditAction.PORTAL_LOCKED_LEVEL_RULE);
            log.info("portal.locked level-rule childRef={} world={} required=S{} actual=S{}",
                    childId, worldId, world.requiredLevel(), childLevel);
            return PortalEntry.locked(worldId, type, PortalEntry.CODE_LEVEL_TOO_LOW, reducedMotion);
        }

        audit(childId, worldId, type, WorldAuditAction.PORTAL_OPENED);
        log.info("portal.opened childRef={} world={} portal={}", childId, worldId, type);
        return PortalEntry.opened(worldId, type, reducedMotion);
    }

    public void setReducedMotion(UUID childId, boolean enabled) {
        prefs.setReducedMotion(childId, enabled);
    }

    private int highestLevelOf(UUID childId) {
        int max = 1;
        for (Operation op : Operation.values()) {
            var found = progressRepo.findByChildAndOperation(childId, op);
            if (found.isPresent()) {
                max = Math.max(max, found.get().currentDifficulty());
            }
        }
        return max;
    }

    /** A world is enterable if any of its operation pools is configured. */
    private boolean isAnyTaskPoolConfigured(String worldId) {
        for (Operation op : Operation.values()) {
            if (taskPools.isConfigured(worldId, op)) {
                return true;
            }
        }
        return false;
    }

    private void audit(UUID childId, String worldId, PortalType type, WorldAuditAction action) {
        auditRepo.append(new WorldAuditEntry(childId, worldId, type, action, clock.instant()));
    }
}
