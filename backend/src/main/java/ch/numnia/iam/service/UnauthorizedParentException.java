package ch.numnia.iam.service;

/**
 * Thrown when a parent attempts to perform an action on a child profile
 * that belongs to a different parent (UC-002 server-side authorization).
 */
public class UnauthorizedParentException extends RuntimeException {
    public UnauthorizedParentException(String message) {
        super(message);
    }
}
