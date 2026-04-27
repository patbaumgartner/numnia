package ch.numnia.deletion.service;

/** Thrown when the supplied parent password does not match (UC-011 BR-001). */
public class InvalidPasswordException extends RuntimeException {
    public InvalidPasswordException(String message) {
        super(message);
    }
}
