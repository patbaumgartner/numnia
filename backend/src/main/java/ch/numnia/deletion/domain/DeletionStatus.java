package ch.numnia.deletion.domain;

/** Lifecycle of a parent-initiated child-account deletion (UC-011 BR-001). */
public enum DeletionStatus {
    /** Confirmation email sent, cool-off period running (24 h). */
    PENDING,
    /** Parent confirmed within cool-off, deletion executed. */
    COMPLETED,
    /** Cool-off elapsed without confirmation, or parent cancelled. */
    DISCARDED
}
