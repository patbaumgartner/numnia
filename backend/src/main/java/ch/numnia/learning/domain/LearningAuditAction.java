package ch.numnia.learning.domain;

/** Audit actions for the learning module (NFR-SEC-001). No PII attached. */
public enum LearningAuditAction {
    TRAINING_SESSION_STARTED,
    ACCURACY_SESSION_STARTED,
    EXPLANATION_REQUESTED,
    TASK_GENERATED,
    ANSWER_SUBMITTED,
    SPEED_DOWNGRADED,
    MODE_SUGGESTED,
    MASTERY_PROMOTED,
    SESSION_ENDED,
    TASK_POOL_EMPTY,
    OUT_OF_RANGE_TASK_REJECTED
}
