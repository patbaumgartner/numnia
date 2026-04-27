package ch.numnia.progress.service;

import ch.numnia.learning.domain.AnswerOutcome;
import ch.numnia.learning.domain.LearningProgress;
import ch.numnia.learning.domain.MasteryStatus;
import ch.numnia.learning.domain.MathTask;
import ch.numnia.learning.domain.Operation;
import ch.numnia.learning.domain.TrainingSession;
import ch.numnia.learning.infra.InMemoryLearningProgressRepository;
import ch.numnia.learning.infra.InMemoryTrainingSessionRepository;
import ch.numnia.progress.domain.ColorPalette;
import ch.numnia.progress.domain.OperationProgress;
import ch.numnia.progress.domain.ProgressOverview;
import ch.numnia.progress.infra.InMemoryAccessibilityPreferencesRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProgressServiceTest {

    private InMemoryLearningProgressRepository progressRepo;
    private InMemoryTrainingSessionRepository sessionRepo;
    private InMemoryAccessibilityPreferencesRepository accessibilityRepo;
    private ProgressService service;
    private UUID childA;
    private UUID childB;

    @BeforeEach
    void setUp() {
        progressRepo = new InMemoryLearningProgressRepository();
        sessionRepo = new InMemoryTrainingSessionRepository();
        accessibilityRepo = new InMemoryAccessibilityPreferencesRepository();
        service = new ProgressService(progressRepo, sessionRepo, accessibilityRepo);
        childA = UUID.randomUUID();
        childB = UUID.randomUUID();
    }

    @Test
    void getOverview_withNoData_returnsEmptyOverviewAndDefaultPalette_alt1a() {
        ProgressOverview overview = service.getOverview(childA);

        assertThat(overview.childId()).isEqualTo(childA);
        assertThat(overview.palette()).isEqualTo(ColorPalette.DEFAULT);
        assertThat(overview.empty()).isTrue();
        assertThat(overview.entries()).isEmpty();
    }

    @Test
    void getOverview_withThreeCompletedSessionsOnAddition_returnsAggregatedEntry_main2() {
        progressRepo.save(new LearningProgress(childA, Operation.ADDITION, 2, 2));
        recordSession(childA, Operation.ADDITION, 5, 4);
        recordSession(childA, Operation.ADDITION, 5, 5);
        recordSession(childA, Operation.ADDITION, 5, 3);

        ProgressOverview overview = service.getOverview(childA);

        assertThat(overview.entries()).hasSize(1);
        OperationProgress entry = overview.entries().get(0);
        assertThat(entry.operation()).isEqualTo(Operation.ADDITION);
        assertThat(entry.totalSessions()).isEqualTo(3);
        assertThat(entry.totalTasks()).isEqualTo(15);
        assertThat(entry.correctTasks()).isEqualTo(12);
        assertThat(entry.accuracy()).isEqualTo(12.0 / 15.0);
        assertThat(entry.masteryStatus()).isEqualTo(MasteryStatus.NOT_STARTED);
        assertThat(overview.empty()).isFalse();
    }

    @Test
    void getOverview_withFourOperations_returnsOneEntryPerOperation_main2() {
        progressRepo.save(new LearningProgress(childA, Operation.ADDITION, 1, 2));
        progressRepo.save(new LearningProgress(childA, Operation.SUBTRACTION, 1, 2));
        progressRepo.save(new LearningProgress(childA, Operation.MULTIPLICATION, 1, 2));
        progressRepo.save(new LearningProgress(childA, Operation.DIVISION, 1, 2));

        ProgressOverview overview = service.getOverview(childA);

        assertThat(overview.entries()).extracting(OperationProgress::operation)
                .containsExactly(Operation.ADDITION, Operation.SUBTRACTION,
                        Operation.MULTIPLICATION, Operation.DIVISION);
    }

    @Test
    void getOverview_includesMasteryStatusMarker_main2() {
        LearningProgress p = progressRepo.save(new LearningProgress(childA, Operation.ADDITION, 2, 2));
        p.setMasteryStatus(MasteryStatus.MASTERED);
        progressRepo.save(p);

        ProgressOverview overview = service.getOverview(childA);

        assertThat(overview.entries()).hasSize(1);
        assertThat(overview.entries().get(0).masteryStatus()).isEqualTo(MasteryStatus.MASTERED);
    }

    @Test
    void getOverview_returnsOnlyOwnData_BR001() {
        progressRepo.save(new LearningProgress(childA, Operation.ADDITION, 1, 2));
        progressRepo.save(new LearningProgress(childB, Operation.ADDITION, 3, 2));
        recordSession(childA, Operation.ADDITION, 4, 3);
        recordSession(childB, Operation.ADDITION, 10, 9);

        ProgressOverview overview = service.getOverview(childA);

        assertThat(overview.childId()).isEqualTo(childA);
        assertThat(overview.entries()).hasSize(1);
        OperationProgress entry = overview.entries().get(0);
        assertThat(entry.totalSessions()).isEqualTo(1);
        assertThat(entry.totalTasks()).isEqualTo(4);
        assertThat(entry.correctTasks()).isEqualTo(3);
        assertThat(entry.currentDifficulty()).isEqualTo(1);
    }

    @Test
    void overview_doesNotExposeLeaderboardOrPeerComparisons_BR002() {
        // Reflective check: the domain types intentionally have no fields
        // that compare against other children.
        var overviewFields = ProgressOverview.class.getRecordComponents();
        var entryFields = OperationProgress.class.getRecordComponents();
        assertThat(overviewFields).extracting(f -> f.getName())
                .doesNotContain("rank", "leaderboard", "peerAverage", "peerRanking", "globalRanking");
        assertThat(entryFields).extracting(f -> f.getName())
                .doesNotContain("rank", "leaderboard", "peerAverage", "peerRanking", "globalRanking");
    }

    @Test
    void setPalette_storesDeuteranopia_alt3a() {
        service.setPalette(childA, ColorPalette.DEUTERANOPIA);

        ProgressOverview overview = service.getOverview(childA);

        assertThat(overview.palette()).isEqualTo(ColorPalette.DEUTERANOPIA);
    }

    @Test
    void setPalette_perChild_isIsolated_BR001() {
        service.setPalette(childA, ColorPalette.DEUTERANOPIA);
        service.setPalette(childB, ColorPalette.TRITANOPIA);

        assertThat(service.getOverview(childA).palette()).isEqualTo(ColorPalette.DEUTERANOPIA);
        assertThat(service.getOverview(childB).palette()).isEqualTo(ColorPalette.TRITANOPIA);
    }

    @Test
    void getOverview_withNullChildId_throws() {
        assertThatThrownBy(() -> service.getOverview(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void setPalette_withNullPalette_throws() {
        assertThatThrownBy(() -> service.setPalette(childA, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getOverview_withOnlyProgressEntryAndNoEndedSessions_reportsZeroSessions_main2() {
        progressRepo.save(new LearningProgress(childA, Operation.MULTIPLICATION, 1, 2));

        ProgressOverview overview = service.getOverview(childA);

        assertThat(overview.entries()).hasSize(1);
        OperationProgress entry = overview.entries().get(0);
        assertThat(entry.totalSessions()).isZero();
        assertThat(entry.totalTasks()).isZero();
        assertThat(entry.accuracy()).isZero();
        assertThat(overview.empty()).isTrue();
    }

    /**
     * Helper: persist a completed (ended) training session with the given
     * total/correct task counts.
     */
    private void recordSession(UUID childId, Operation op, int total, int correct) {
        TrainingSession s = new TrainingSession(UUID.randomUUID(), childId, op,
                1, 2, false, Instant.now());
        // Record `total` task outcomes; the first `correct` are CORRECT.
        for (int i = 0; i < total; i++) {
            s.recordTask(new MathTask(UUID.randomUUID(), op, 1, 1, 2, 1, 2));
            s.recordOutcome(i < correct ? AnswerOutcome.CORRECT : AnswerOutcome.WRONG);
        }
        s.end(Instant.now());
        sessionRepo.save(s);
    }
}
