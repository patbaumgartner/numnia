package ch.numnia.iam.service;

/** Thrown when a registration attempt uses an already-registered email address. */
public class DuplicateEmailException extends RuntimeException {
    public DuplicateEmailException(String message) {
        super(message);
    }
}
