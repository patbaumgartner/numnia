package ch.numnia.creatures.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Permanent unlock of a creature for a child (UC-006, BR-001).
 *
 * <p>Once recorded, this entry is never removed — creatures cannot be lost
 * through errors (BR-001 of UC-006, FR-GAM-005).
 */
public record CreatureUnlock(UUID childId, String creatureId, Instant unlockedAt) {

    public CreatureUnlock {
        if (childId == null) {
            throw new IllegalArgumentException("childId must not be null");
        }
        if (creatureId == null || creatureId.isBlank()) {
            throw new IllegalArgumentException("creatureId must not be blank");
        }
        if (unlockedAt == null) {
            throw new IllegalArgumentException("unlockedAt must not be null");
        }
    }
}
