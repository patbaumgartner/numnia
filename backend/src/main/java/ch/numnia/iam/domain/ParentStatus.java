package ch.numnia.iam.domain;

/**
 * Lifecycle states of a parent account during the double opt-in process.
 *
 * <p>Transitions: NOT_VERIFIED → EMAIL_VERIFIED (after primary email confirmation)
 * → FULLY_CONSENTED (after secondary email confirmation for the child profile).
 */
public enum ParentStatus {
    /** Account created; primary email not yet confirmed. */
    NOT_VERIFIED,
    /** Primary email confirmed via verification link. */
    EMAIL_VERIFIED,
    /** Both opt-in steps completed; sensitive functions may be unlocked. */
    FULLY_CONSENTED
}
