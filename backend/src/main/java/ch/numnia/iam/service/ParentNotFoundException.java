package ch.numnia.iam.service;

/** Thrown when a referenced parent account does not exist. */
public class ParentNotFoundException extends RuntimeException {
    public ParentNotFoundException(String message) {
        super(message);
    }
}
