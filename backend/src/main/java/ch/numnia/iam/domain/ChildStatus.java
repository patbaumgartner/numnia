package ch.numnia.iam.domain;

/**
 * Lifecycle states of a child profile during the double opt-in process.
 */
public enum ChildStatus {
    /** Child profile created; secondary email confirmation pending. */
    PENDING_CONFIRM,
    /** Secondary email confirmed; profile is ready to play. */
    ACTIVE
}
