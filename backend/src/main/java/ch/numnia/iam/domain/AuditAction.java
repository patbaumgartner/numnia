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
    RATE_LIMIT_HIT
}
