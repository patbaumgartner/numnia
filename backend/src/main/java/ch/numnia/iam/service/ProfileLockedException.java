package ch.numnia.iam.service;

/**
 * Thrown when sign-in is attempted for a locked child profile (UC-002 BR-004).
 */
public class ProfileLockedException extends RuntimeException {
    public ProfileLockedException(String message) {
        super(message);
    }
}
