package ch.numnia.deletion.service;

/**
 * Thrown when the cool-off confirmation link cannot be honoured
 * (unknown / expired / already-used token, UC-011 alt-flow 4a).
 */
public class DeletionLinkUnavailableException extends RuntimeException {
    public DeletionLinkUnavailableException(String message) {
        super(message);
    }
}
