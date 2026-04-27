package ch.numnia.creatures.service;

/** Thrown when a creature id is not in the catalogue (UC-006 → HTTP 404). */
public class UnknownCreatureException extends RuntimeException {
    public UnknownCreatureException(String creatureId) {
        super("unknown creature: " + creatureId);
    }
}
