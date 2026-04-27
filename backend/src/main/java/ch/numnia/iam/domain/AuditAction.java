package ch.numnia.iam.domain;

/**
 * Audit actions recorded for security-relevant events (BR-005, NFR-SEC-001).
 *
 * <p>No PII is stored alongside these actions; parents are referenced by their
 * internal UUID, children by their pseudonym only.
 */
public enum AuditAction {
    ACCOUNT_CREATED,
    EMAIL_PRIMARY_VERIFIED,
    CHILD_PROFILE_CREATED,
    EMAIL_SECONDARY_CONFIRMED,
    DUPLICATE_REGISTRATION_BLOCKED,
    INVALID_TOKEN,
    TOKEN_EXPIRED,
    VERIFICATION_EMAIL_RESENT,
    RATE_LIMIT_HIT,
    // UC-002: child sign-in lifecycle
    CHILD_PIN_SET,
    CHILD_SIGNED_IN,
    CHILD_SIGN_IN_FAILED,
    CHILD_PROFILE_LOCKED,
    CHILD_LOCK_RELEASED,
    CHILD_SIGNED_OUT,
    // UC-002: cross-area authorization guard
    PARENT_ENDPOINT_DENIED_FOR_CHILD,
    // UC-010: parent self-service data export
    EXPORT_TRIGGERED,
    EXPORT_DOWNLOADED,
    EXPORT_FILE_EXPIRED,
    EXPORT_GENERATION_FAILED,
    // UC-011: parent self-service deletion
    DELETION_REQUESTED,
    DELETION_CONFIRMED,
    DELETION_DISCARDED,
    DELETION_BACKUP_CLEANSED
}
