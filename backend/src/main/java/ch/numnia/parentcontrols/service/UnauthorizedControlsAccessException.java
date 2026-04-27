package ch.numnia.parentcontrols.service;

/** Thrown when an updateControls request is not authorised for the parent. */
public class UnauthorizedControlsAccessException extends RuntimeException {
    public UnauthorizedControlsAccessException(String message) {
        super(message);
    }
}
