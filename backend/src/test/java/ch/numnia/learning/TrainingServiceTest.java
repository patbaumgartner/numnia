package ch.numnia.learning;

import ch.numnia.learning.domain.*;
import ch.numnia.learning.infra.*;
import ch.numnia.learning.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.Random;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/** Unit-level integration test wiring all in-memory collaborators. */
class TrainingServiceTest {

    private InMemoryLearningProgressRepository progressRepo;
    private InMemoryTrainingSessionRepository sessionRepo;
    private InMemoryStarPointsRepository starRepo;
    private InMemoryTaskPoolRepository pools;
    private InMemoryLearningAuditRepository audit;
    private TrainingService service;

    private final UUID childId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        progressRepo = new InMemoryLearningProgressRepository();
        sessionRepo = new InMemoryTrainingSessionRepository();
        starRepo = new InMemoryStarPointsRepository();
        pools = new InMemoryTaskPoolRepository();
        audit = new InMemoryLearningAuditRepository();
        service = new TrainingService(
                progressRepo, sessionRepo, new TaskGenerator(),
                new AdaptiveEngine(), new MasteryTracker(),
                new StarPointsService(starRepo),
                pools, audit,
                Clock.fixed(Instant.parse("2026-05-01T10:00:00Z"), ZoneOffset.UTC),
                new Random(123));
    }

    @Test
    void startSession_unconfiguredPool_throwsAndAudits() {
        pools.clear(InMemoryTaskPoolRepository.DEFAULT_WORLD);

        assertThatThrownBy(() -> service.startSession(childId, Operation.ADDITION,
                InMemoryTaskPoolRepository.DEFAULT_WORLD))
                .isInstanceOf(TaskPoolNotConfiguredException.class);
        assertThat(audit.findByChildRef(childId))
                .anyMatch(e -> e.action() == LearningAuditAction.TASK_POOL_EMPTY);
    }

    @Test
    void submitAnswer_threeWrongAnswersInARow_downgradesSpeedAndSuggestsAccuracy() {
        progressRepo.save(new LearningProgress(childId, Operation.MULTIPLICATION, 3, 3));
        var session = service.startSession(childId, Operation.MULTIPLICATION,
                InMemoryTaskPoolRepository.DEFAULT_WORLD);

        AnswerResult r = null;
        for (int i = 0; i < 3; i++) {
            service.nextTask(session.id());
            r = service.submitAnswer(session.id(), -1, 1000);
        }

        assertThat(r.outcome()).isEqualTo(AnswerOutcome.WRONG);
        assertThat(r.currentSpeed()).isEqualTo(2);
        assertThat(r.modeSuggestion()).isEqualTo(ModeSuggestion.ACCURACY);
    }

    @Test
    void nextTask_addition_resultIsWithinRangeUpTo1Million() {
        progressRepo.save(new LearningProgress(childId, Operation.ADDITION, 6, 2));
        var session = service.startSession(childId, Operation.ADDITION,
                InMemoryTaskPoolRepository.DEFAULT_WORLD);

        for (int i = 0; i < 25; i++) {
            MathTask task = service.nextTask(session.id());
            assertThat(task.expectedAnswer()).isBetween(0, 1_000_000);
        }
    }

    @Test
    void wrongAnswer_doesNotChangeStarPointsBalance_brNoPenalty() {
        starRepo.setBalance(childId, 12);
        progressRepo.save(new LearningProgress(childId, Operation.ADDITION, 1, 2));
        var session = service.startSession(childId, Operation.ADDITION,
                InMemoryTaskPoolRepository.DEFAULT_WORLD);
        service.nextTask(session.id());

        AnswerResult r = service.submitAnswer(session.id(), Integer.MIN_VALUE, 500);

        assertThat(r.outcome()).isEqualTo(AnswerOutcome.WRONG);
        assertThat(r.starPointsBalance()).isEqualTo(12);
        assertThat(starRepo.balanceOf(childId)).isEqualTo(12);
    }

    @Test
    void endSession_singleQualifyingDay_keepsMasteryInConsolidation() {
        progressRepo.save(new LearningProgress(childId, Operation.ADDITION, 2, 2));
        var session = qualifyingSession(service);

        SessionSummary summary = service.endSession(session.id());

        assertThat(summary.masteryStatus()).isEqualTo(MasteryStatus.IN_CONSOLIDATION);
    }

    @Test
    void endSession_secondQualifyingSessionLaterDay_promotesToMastered() {
        progressRepo.save(new LearningProgress(childId, Operation.ADDITION, 2, 2));
        var firstSession = qualifyingSession(service);
        service.endSession(firstSession.id());

        TrainingService dayTwo = new TrainingService(
                progressRepo, sessionRepo, new TaskGenerator(),
                new AdaptiveEngine(), new MasteryTracker(),
                new StarPointsService(starRepo),
                pools, audit,
                Clock.fixed(Instant.parse("2026-05-02T10:00:00Z"), ZoneOffset.UTC),
                new Random(123));
        var secondSession = qualifyingSession(dayTwo);
        SessionSummary summary = dayTwo.endSession(secondSession.id());

        assertThat(summary.masteryStatus()).isEqualTo(MasteryStatus.MASTERED);
    }

    private TrainingSession qualifyingSession(TrainingService svc) {
        var session = svc.startSession(childId, Operation.ADDITION,
                InMemoryTaskPoolRepository.DEFAULT_WORLD);
        for (int i = 0; i < 5; i++) {
            MathTask t = svc.nextTask(session.id());
            svc.submitAnswer(session.id(), t.expectedAnswer(), 500);
        }
        return session;
    }
}
