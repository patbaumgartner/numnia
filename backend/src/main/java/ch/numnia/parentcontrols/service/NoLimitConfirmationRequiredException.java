package ch.numnia.parentcontrols.service;

/** Thrown when the parent picks "no limit" without explicit confirmation (alt 3a). */
public class NoLimitConfirmationRequiredException extends RuntimeException {
    public NoLimitConfirmationRequiredException(String message) {
        super(message);
    }
}
