package ch.numnia.deletion.service;

/** Thrown when the explicit confirmation word is not "DELETE" (UC-011 BR-001). */
public class InvalidConfirmationWordException extends RuntimeException {
    public InvalidConfirmationWordException(String message) {
        super(message);
    }
}
