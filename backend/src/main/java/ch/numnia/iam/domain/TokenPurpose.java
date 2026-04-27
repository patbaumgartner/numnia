package ch.numnia.iam.domain;

/**
 * Distinguishes the two opt-in emails in the double opt-in flow (BR-001).
 */
public enum TokenPurpose {
    /** Confirms the parent's own email address (step 1). */
    EMAIL_PRIMARY,
    /** Confirms secondary consent for the child profile (step 2). */
    EMAIL_SECONDARY
}
