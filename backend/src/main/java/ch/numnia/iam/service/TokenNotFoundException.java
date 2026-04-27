package ch.numnia.iam.service;

/** Thrown when a verification token cannot be found. */
public class TokenNotFoundException extends RuntimeException {
    public TokenNotFoundException(String message) {
        super(message);
    }
}
