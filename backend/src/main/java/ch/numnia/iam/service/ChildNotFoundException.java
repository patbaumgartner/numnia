package ch.numnia.iam.service;

/**
 * Thrown when a child profile is not found by ID.
 */
public class ChildNotFoundException extends RuntimeException {
    public ChildNotFoundException(String message) {
        super(message);
    }
}
