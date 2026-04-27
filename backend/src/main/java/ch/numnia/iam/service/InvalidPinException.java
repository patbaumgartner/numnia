package ch.numnia.iam.service;

/**
 * Thrown when a child sign-in attempt fails due to an incorrect PIN.
 */
public class InvalidPinException extends RuntimeException {
    public InvalidPinException(String message) {
        super(message);
    }
}
