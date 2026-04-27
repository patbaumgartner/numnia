package ch.numnia.learning.service;

import ch.numnia.learning.domain.*;
import ch.numnia.learning.spi.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Random;
import java.util.UUID;

/**
 * Orchestrates the training-mode loop (UC-003 main flow).
 *
 * <p>Logging discipline (NFR-PRIV-001): only UUIDs and pseudonyms are logged;
 * never plaintext PII or expected answers.
 */
@Service
public class TrainingService {

    private static final Logger log = LoggerFactory.getLogger(TrainingService.class);

    static final double MASTERY_ACCURACY_THRESHOLD = 0.9;
    static final int MASTERY_TASK_THRESHOLD = 5;

    static final int DEFAULT_DIFFICULTY = 1;
    static final int DEFAULT_SPEED = 2;

    private final LearningProgressRepository progressRepo;
    private final TrainingSessionRepository sessionRepo;
    private final TaskGenerator taskGenerator;
    private final AdaptiveEngine adaptiveEngine;
    private final MasteryTracker masteryTracker;
    private final StarPointsService starPoints;
    private final TaskPoolRepository taskPools;
    private final LearningAuditRepository audit;
    private final Clock clock;
    private final Random rng;

    @org.springframework.beans.factory.annotation.Autowired
    public TrainingService(LearningProgressRepository progressRepo,
                           TrainingSessionRepository sessionRepo,
                           TaskGenerator taskGenerator,
                           AdaptiveEngine adaptiveEngine,
                           MasteryTracker masteryTracker,
                           StarPointsService starPoints,
                           TaskPoolRepository taskPools,
                           LearningAuditRepository audit) {
        this(progressRepo, sessionRepo, taskGenerator, adaptiveEngine, masteryTracker,
                starPoints, taskPools, audit, Clock.systemUTC(), new Random());
    }

    public TrainingService(LearningProgressRepository progressRepo,
                           TrainingSessionRepository sessionRepo,
                           TaskGenerator taskGenerator,
                           AdaptiveEngine adaptiveEngine,
                           MasteryTracker masteryTracker,
                           StarPointsService starPoints,
                           TaskPoolRepository taskPools,
                           LearningAuditRepository audit,
                           Clock clock,
                           Random rng) {
        this.progressRepo = progressRepo;
        this.sessionRepo = sessionRepo;
        this.taskGenerator = taskGenerator;
        this.adaptiveEngine = adaptiveEngine;
        this.masteryTracker = masteryTracker;
        this.starPoints = starPoints;
        this.taskPools = taskPools;
        this.audit = audit;
        this.clock = clock;
        this.rng = rng;
    }

    public TrainingSession startSession(UUID childId, Operation operation, String worldId) {
        if (!taskPools.isConfigured(worldId, operation)) {
            audit.append(childId, LearningAuditAction.TASK_POOL_EMPTY,
                    "world=" + worldId + ",op=" + operation);
            throw new TaskPoolNotConfiguredException(
                    "No task pool configured for world=" + worldId + " op=" + operation);
        }
        var progress = progressRepo.findByChildAndOperation(childId, operation)
                .orElseGet(() -> progressRepo.save(new LearningProgress(
                        childId, operation, DEFAULT_DIFFICULTY, DEFAULT_SPEED)));
        var session = new TrainingSession(
                UUID.randomUUID(), childId, operation,
                progress.currentDifficulty(), progress.currentSpeed(),
                clock.instant());
        sessionRepo.save(session);
        audit.append(childId, LearningAuditAction.TRAINING_SESSION_STARTED,
                "session=" + session.id() + ",op=" + operation);
        log.info("training.session.started childRef={} op={} S={} G={}",
                childId, operation, session.currentDifficulty(), session.currentSpeed());
        return session;
    }

    public MathTask nextTask(UUID sessionId) {
        var session = requireSession(sessionId);
        var task = taskGenerator.generate(
                session.operation(),
                session.currentDifficulty(),
                session.currentSpeed(),
                rng);
        if (task.expectedAnswer() < 0 || task.expectedAnswer() > MathTask.MAX_RESULT) {
            audit.append(session.childId(), LearningAuditAction.OUT_OF_RANGE_TASK_REJECTED,
                    "session=" + sessionId);
            throw new IllegalStateException("Task generator produced out-of-range result");
        }
        session.recordTask(task);
        sessionRepo.save(session);
        audit.append(session.childId(), LearningAuditAction.TASK_GENERATED,
                "session=" + sessionId + ",S=" + task.difficulty() + ",G=" + task.speed());
        return task;
    }

    public AnswerResult submitAnswer(UUID sessionId, int answer, long responseTimeMs) {
        var session = requireSession(sessionId);
        var task = session.currentTask();
        if (task == null) {
            throw new IllegalStateException("No current task on session " + sessionId);
        }
        AnswerOutcome outcome = (answer == task.expectedAnswer())
                ? AnswerOutcome.CORRECT : AnswerOutcome.WRONG;
        return finalizeAnswer(session, outcome);
    }

    public AnswerResult submitTimeout(UUID sessionId) {
        var session = requireSession(sessionId);
        return finalizeAnswer(session, AnswerOutcome.TIMEOUT);
    }

    private AnswerResult finalizeAnswer(TrainingSession session, AnswerOutcome outcome) {
        session.recordOutcome(outcome);
        ModeSuggestion suggestion = adaptiveEngine.applyAfterAnswer(session);
        if (suggestion != ModeSuggestion.NONE) {
            audit.append(session.childId(), LearningAuditAction.SPEED_DOWNGRADED,
                    "session=" + session.id() + ",newG=" + session.currentSpeed());
            audit.append(session.childId(), LearningAuditAction.MODE_SUGGESTED,
                    "session=" + session.id() + ",mode=" + suggestion);
        }
        int balance = starPoints.reward(session.childId(), outcome);
        sessionRepo.save(session);
        audit.append(session.childId(), LearningAuditAction.ANSWER_SUBMITTED,
                "session=" + session.id() + ",outcome=" + outcome);
        return new AnswerResult(outcome, session.currentSpeed(), suggestion, balance);
    }

    public SessionSummary endSession(UUID sessionId) {
        var session = requireSession(sessionId);
        session.end(clock.instant());
        var progress = progressRepo.findByChildAndOperation(session.childId(), session.operation())
                .orElseThrow(() -> new IllegalStateException("Missing progress"));
        boolean thresholdsMet = session.totalTasks() >= MASTERY_TASK_THRESHOLD
                && session.accuracy() >= MASTERY_ACCURACY_THRESHOLD;
        LocalDate today = LocalDate.ofInstant(clock.instant(), ZoneId.of("UTC"));
        boolean changed = masteryTracker.evaluate(progress, thresholdsMet, today);
        if (changed) {
            progressRepo.save(progress);
            if (progress.masteryStatus() == MasteryStatus.MASTERED) {
                audit.append(session.childId(), LearningAuditAction.MASTERY_PROMOTED,
                        "op=" + progress.operation() + ",S=" + progress.currentDifficulty());
            }
        }
        sessionRepo.save(session);
        audit.append(session.childId(), LearningAuditAction.SESSION_ENDED,
                "session=" + sessionId + ",total=" + session.totalTasks()
                        + ",correct=" + session.correctTasks());
        return new SessionSummary(
                session.id(),
                session.totalTasks(),
                session.correctTasks(),
                starPoints.balanceOf(session.childId()),
                progress.masteryStatus());
    }

    private TrainingSession requireSession(UUID id) {
        return sessionRepo.findById(id).orElseThrow(
                () -> new TrainingSessionNotFoundException("Unknown session " + id));
    }
}
