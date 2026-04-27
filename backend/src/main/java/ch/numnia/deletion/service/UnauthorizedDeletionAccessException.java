package ch.numnia.deletion.service;

/** Thrown when a parent attempts to delete a child profile they do not own (NFR-SEC-003). */
public class UnauthorizedDeletionAccessException extends RuntimeException {
    public UnauthorizedDeletionAccessException(String message) {
        super(message);
    }
}
