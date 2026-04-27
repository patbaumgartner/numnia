package ch.numnia.worlds.service;

/** Raised when a portal request references a slug that is not in the catalogue. */
public class UnknownWorldException extends RuntimeException {

    public UnknownWorldException(String worldId) {
        super("unknown world: " + worldId);
    }
}
