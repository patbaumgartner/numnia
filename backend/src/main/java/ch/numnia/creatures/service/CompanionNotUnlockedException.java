package ch.numnia.creatures.service;

/**
 * Thrown when the child tries to pick a creature as companion that is not
 * yet unlocked (UC-006 exception flow 5x → HTTP 409).
 */
public class CompanionNotUnlockedException extends RuntimeException {

    private final String creatureId;

    public CompanionNotUnlockedException(String creatureId) {
        super("creature is not unlocked: " + creatureId);
        this.creatureId = creatureId;
    }

    public String creatureId() {
        return creatureId;
    }
}
