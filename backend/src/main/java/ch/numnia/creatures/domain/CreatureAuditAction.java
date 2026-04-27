package ch.numnia.creatures.domain;

/** Audit actions emitted by the creatures module (UC-006). */
public enum CreatureAuditAction {
    CREATURE_UNLOCKED,
    CREATURE_UNLOCK_CONSOLATION_AWARDED,
    COMPANION_CHANGED,
    COMPANION_PICK_REJECTED_NOT_UNLOCKED
}
