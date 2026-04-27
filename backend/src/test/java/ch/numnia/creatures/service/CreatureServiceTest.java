package ch.numnia.creatures.service;

import ch.numnia.creatures.domain.Creature;
import ch.numnia.creatures.domain.CreatureAuditAction;
import ch.numnia.creatures.domain.CreatureUnlockResult;
import ch.numnia.creatures.infra.InMemoryCompanionRepository;
import ch.numnia.creatures.infra.InMemoryCreatureAuditRepository;
import ch.numnia.creatures.infra.InMemoryCreatureInventoryRepository;
import ch.numnia.creatures.infra.StaticCreatureCatalog;
import ch.numnia.learning.domain.LearningProgress;
import ch.numnia.learning.domain.MasteryStatus;
import ch.numnia.learning.domain.Operation;
import ch.numnia.learning.infra.InMemoryLearningProgressRepository;
import ch.numnia.learning.infra.InMemoryStarPointsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CreatureServiceTest {

    private StaticCreatureCatalog catalog;
    private InMemoryCreatureInventoryRepository inventory;
    private InMemoryCompanionRepository companions;
    private InMemoryCreatureAuditRepository audit;
    private InMemoryLearningProgressRepository progressRepo;
    private InMemoryStarPointsRepository starPoints;
    private CreatureService service;
    private UUID child;

    @BeforeEach
    void setUp() {
        catalog = new StaticCreatureCatalog();
        inventory = new InMemoryCreatureInventoryRepository();
        companions = new InMemoryCompanionRepository();
        audit = new InMemoryCreatureAuditRepository();
        progressRepo = new InMemoryLearningProgressRepository();
        starPoints = new InMemoryStarPointsRepository();
        service = new CreatureService(catalog, inventory, companions, audit,
                progressRepo, starPoints,
                Clock.fixed(Instant.parse("2026-04-28T10:00:00Z"), ZoneOffset.UTC));
        child = UUID.randomUUID();
    }

    private void mastered(Operation op) {
        LearningProgress p = new LearningProgress(child, op, 3, 3);
        p.setMasteryStatus(MasteryStatus.MASTERED);
        progressRepo.save(p);
    }

    @Test
    void processUnlocks_withMasteredAddition_unlocksPilzar_BR001() {
        mastered(Operation.ADDITION);

        CreatureUnlockResult result = service.processUnlocks(child);

        assertThat(result.newlyUnlocked()).extracting(Creature::id)
                .containsExactly(StaticCreatureCatalog.PILZAR_ID);
        assertThat(inventory.unlockedSet(child)).contains(StaticCreatureCatalog.PILZAR_ID);
        assertThat(audit.actionsFor(child)).contains(CreatureAuditAction.CREATURE_UNLOCKED);
    }

    @Test
    void processUnlocks_isIdempotent_doesNotDuplicate_BR001() {
        mastered(Operation.ADDITION);

        service.processUnlocks(child);
        CreatureUnlockResult second = service.processUnlocks(child);

        assertThat(second.newlyUnlocked()).isEmpty();
        assertThat(inventory.unlockedCreatureIds(child))
                .containsExactly(StaticCreatureCatalog.PILZAR_ID);
    }

    @Test
    void processUnlocks_withoutMastery_unlocksNothing() {
        progressRepo.save(new LearningProgress(child, Operation.ADDITION, 1, 1));

        CreatureUnlockResult result = service.processUnlocks(child);

        assertThat(result.newlyUnlocked()).isEmpty();
        assertThat(result.consolationAwarded()).isFalse();
        assertThat(inventory.unlockedSet(child)).isEmpty();
    }

    @Test
    void processUnlocks_allCreaturesAlreadyUnlocked_grantsConsolationStarPoints_alt1a() {
        mastered(Operation.ADDITION);
        mastered(Operation.SUBTRACTION);
        mastered(Operation.MULTIPLICATION);
        // First call unlocks all 3.
        CreatureUnlockResult first = service.processUnlocks(child);
        assertThat(first.newlyUnlocked()).hasSize(3);

        // Second call: threshold still met but inventory full → consolation.
        int balanceBefore = starPoints.balanceOf(child);
        CreatureUnlockResult second = service.processUnlocks(child);

        assertThat(second.newlyUnlocked()).isEmpty();
        assertThat(second.consolationAwarded()).isTrue();
        assertThat(second.starPointsAwarded())
                .isEqualTo(CreatureService.CONSOLATION_STAR_POINTS);
        assertThat(starPoints.balanceOf(child) - balanceBefore)
                .isEqualTo(CreatureService.CONSOLATION_STAR_POINTS);
        assertThat(audit.actionsFor(child))
                .contains(CreatureAuditAction.CREATURE_UNLOCK_CONSOLATION_AWARDED);
    }

    @Test
    void pickCompanion_withUnlockedCreature_setsCompanion_BR003() {
        mastered(Operation.ADDITION);
        service.processUnlocks(child);

        service.pickCompanion(child, StaticCreatureCatalog.PILZAR_ID);

        assertThat(companions.findCompanion(child)).contains(StaticCreatureCatalog.PILZAR_ID);
        assertThat(audit.actionsFor(child)).contains(CreatureAuditAction.COMPANION_CHANGED);
    }

    @Test
    void pickCompanion_canSwapAtAnyTime_BR003() {
        mastered(Operation.ADDITION);
        mastered(Operation.SUBTRACTION);
        service.processUnlocks(child);

        service.pickCompanion(child, StaticCreatureCatalog.PILZAR_ID);
        service.pickCompanion(child, StaticCreatureCatalog.ZACKA_ID);

        assertThat(companions.findCompanion(child)).contains(StaticCreatureCatalog.ZACKA_ID);
    }

    @Test
    void pickCompanion_withLockedCreature_throwsCompanionNotUnlocked_409() {
        // No mastery anywhere — Welleno is locked.
        assertThatThrownBy(() -> service.pickCompanion(child, StaticCreatureCatalog.WELLENO_ID))
                .isInstanceOf(CompanionNotUnlockedException.class);

        assertThat(companions.findCompanion(child)).isEmpty();
        assertThat(audit.actionsFor(child))
                .contains(CreatureAuditAction.COMPANION_PICK_REJECTED_NOT_UNLOCKED);
    }

    @Test
    void pickCompanion_withUnknownCreature_throwsUnknownCreatureException_404() {
        assertThatThrownBy(() -> service.pickCompanion(child, "does-not-exist"))
                .isInstanceOf(UnknownCreatureException.class);
    }

    @Test
    void pickCompanion_doesNotChangeCompanionWhenLockedAttempted() {
        mastered(Operation.ADDITION);
        service.processUnlocks(child);
        service.pickCompanion(child, StaticCreatureCatalog.PILZAR_ID);

        try {
            service.pickCompanion(child, StaticCreatureCatalog.WELLENO_ID);
        } catch (CompanionNotUnlockedException ignored) {
            // expected
        }

        assertThat(companions.findCompanion(child)).contains(StaticCreatureCatalog.PILZAR_ID);
    }

    @Test
    void listGallery_returnsAllThreeWithUnlockedAndCompanionFlags() {
        mastered(Operation.ADDITION);
        service.processUnlocks(child);
        service.pickCompanion(child, StaticCreatureCatalog.PILZAR_ID);

        var gallery = service.listGallery(child);

        assertThat(gallery).hasSize(3);
        var pilzar = gallery.stream()
                .filter(e -> e.creature().id().equals(StaticCreatureCatalog.PILZAR_ID))
                .findFirst().orElseThrow();
        assertThat(pilzar.unlocked()).isTrue();
        assertThat(pilzar.isCompanion()).isTrue();
        var welleno = gallery.stream()
                .filter(e -> e.creature().id().equals(StaticCreatureCatalog.WELLENO_ID))
                .findFirst().orElseThrow();
        assertThat(welleno.unlocked()).isFalse();
        assertThat(welleno.isCompanion()).isFalse();
    }

    @Test
    void creatureNames_acceptVariableEndings_BR002() {
        // Names with three different endings, none with trailing 'i'.
        new Creature("pilzar", "Pilzar", Operation.ADDITION, "mushroom-jungle");
        new Creature("welleno", "Welleno", Operation.MULTIPLICATION, "cloud-island");
        new Creature("zacka", "Zacka", Operation.SUBTRACTION, "crystal-cave");
        // No exception means BR-002 holds.
    }

    @Test
    void creatureName_blank_isRejected() {
        assertThatThrownBy(() ->
                new Creature("x", "  ", Operation.ADDITION, "mushroom-jungle"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void creatureName_withSharpS_isRejected_NFRI18N004() {
        assertThatThrownBy(() ->
                new Creature("x", "Süß", Operation.ADDITION, "mushroom-jungle"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void processUnlocks_withNullChildId_isRejected() {
        assertThatThrownBy(() -> service.processUnlocks(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
