package ch.numnia.iam.service;

/**
 * Thrown when a child-profile creation request violates a business rule
 * (age outside 7-12 range, fantasy name not in vetted catalog, avatar not
 * in gender-neutral catalog).
 */
public class InvalidChildProfileException extends RuntimeException {
    public InvalidChildProfileException(String message) {
        super(message);
    }
}
