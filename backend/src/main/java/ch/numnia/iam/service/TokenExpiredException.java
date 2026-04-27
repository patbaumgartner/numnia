package ch.numnia.iam.service;

/** Thrown when a verification token has expired (older than 24 h). */
public class TokenExpiredException extends RuntimeException {
    public TokenExpiredException(String message) {
        super(message);
    }
}
