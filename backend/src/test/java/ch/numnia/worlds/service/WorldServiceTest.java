package ch.numnia.worlds.service;

import ch.numnia.learning.domain.LearningProgress;
import ch.numnia.learning.domain.Operation;
import ch.numnia.learning.infra.InMemoryTaskPoolRepository;
import ch.numnia.learning.spi.LearningProgressRepository;
import ch.numnia.worlds.domain.PortalEntry;
import ch.numnia.worlds.domain.PortalType;
import ch.numnia.worlds.domain.WorldAuditAction;
import ch.numnia.worlds.infra.InMemoryChildPreferencesRepository;
import ch.numnia.worlds.infra.InMemoryWorldAuditRepository;
import ch.numnia.worlds.infra.StaticWorldCatalog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class WorldServiceTest {

    private LearningProgressRepository progressRepo;
    private InMemoryTaskPoolRepository taskPools;
    private InMemoryChildPreferencesRepository prefs;
    private InMemoryWorldAuditRepository audit;
    private StaticWorldCatalog catalog;
    private WorldService service;
    private UUID child;

    @BeforeEach
    void setUp() {
        progressRepo = Mockito.mock(LearningProgressRepository.class);
        Mockito.when(progressRepo.findByChildAndOperation(Mockito.any(), Mockito.any()))
                .thenReturn(Optional.empty());
        taskPools = new InMemoryTaskPoolRepository();
        prefs = new InMemoryChildPreferencesRepository();
        audit = new InMemoryWorldAuditRepository();
        catalog = new StaticWorldCatalog();
        service = new WorldService(catalog, prefs, audit, progressRepo, taskPools,
                Clock.fixed(Instant.parse("2026-04-27T18:00:00Z"), ZoneOffset.UTC));
        child = UUID.randomUUID();
    }

    @Test
    void listWorlds_returnsExactlyThreeWorldsInRelease1_BR001() {
        assertThat(service.listWorlds()).hasSize(3)
                .extracting("id")
                .containsExactly("mushroom-jungle", "crystal-cave", "cloud-island");
    }

    @Test
    void openPortal_trainingWithSatisfiedRules_opensAndAuditsPortalOpened() {
        Mockito.when(progressRepo.findByChildAndOperation(child, Operation.ADDITION))
                .thenReturn(Optional.of(new LearningProgress(child, Operation.ADDITION, 1, 1)));

        PortalEntry entry = service.openPortal(child, "mushroom-jungle", PortalType.TRAINING);

        assertThat(entry.locked()).isFalse();
        assertThat(entry.target()).isEqualTo(PortalEntry.TARGET_PRACTICE_STAGE);
        assertThat(audit.actionsFor(child)).contains(WorldAuditAction.PORTAL_OPENED);
    }

    @Test
    void openPortal_duelInRelease1_isLockedWithComingLaterCode_BR002() {
        PortalEntry entry = service.openPortal(child, "mushroom-jungle", PortalType.DUEL);

        assertThat(entry.locked()).isTrue();
        assertThat(entry.target()).isNull();
        assertThat(entry.messageCode()).isEqualTo(PortalEntry.CODE_COMING_LATER);
        assertThat(audit.actionsFor(child)).contains(WorldAuditAction.PORTAL_LOCKED_RELEASE_RULE);
    }

    @Test
    void openPortal_levelTooLow_isLockedWithLevelMessage_BR002() {
        PortalEntry entry = service.openPortal(child, "crystal-cave", PortalType.TRAINING);

        assertThat(entry.locked()).isTrue();
        assertThat(entry.messageCode()).isEqualTo(PortalEntry.CODE_LEVEL_TOO_LOW);
        assertThat(audit.actionsFor(child)).contains(WorldAuditAction.PORTAL_LOCKED_LEVEL_RULE);
    }

    @Test
    void openPortal_taskPoolMissing_isLockedWithTaskPoolMessage_BR002() {
        taskPools.clear("mushroom-jungle");
        Mockito.when(progressRepo.findByChildAndOperation(child, Operation.ADDITION))
                .thenReturn(Optional.of(new LearningProgress(child, Operation.ADDITION, 3, 1)));

        PortalEntry entry = service.openPortal(child, "mushroom-jungle", PortalType.TRAINING);

        assertThat(entry.locked()).isTrue();
        assertThat(entry.messageCode()).isEqualTo(PortalEntry.CODE_TASK_POOL_MISSING);
        assertThat(audit.actionsFor(child))
                .contains(WorldAuditAction.PORTAL_LOCKED_TASK_POOL_MISSING);
    }

    @Test
    void openPortal_reducedMotionEnabled_setsFlagAndAuditsApplied_NfrA11y003() {
        prefs.setReducedMotion(child, true);
        Mockito.when(progressRepo.findByChildAndOperation(child, Operation.ADDITION))
                .thenReturn(Optional.of(new LearningProgress(child, Operation.ADDITION, 1, 1)));

        PortalEntry entry = service.openPortal(child, "mushroom-jungle", PortalType.TRAINING);

        assertThat(entry.reducedMotion()).isTrue();
        assertThat(audit.actionsFor(child)).contains(WorldAuditAction.REDUCED_MOTION_APPLIED);
    }

    @Test
    void openPortal_unknownWorld_throwsUnknownWorldException() {
        assertThatThrownBy(() -> service.openPortal(child, "atlantis", PortalType.TRAINING))
                .isInstanceOf(UnknownWorldException.class);
    }

    @Test
    void openPortal_nullChild_throwsIllegalArgument() {
        assertThatThrownBy(() -> service.openPortal(null, "mushroom-jungle", PortalType.TRAINING))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
