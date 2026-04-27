package ch.numnia.creatures.spi;

import ch.numnia.creatures.domain.CreatureUnlock;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Per-child inventory of unlocked creatures (BR-001 — permanent). */
public interface CreatureInventoryRepository {

    /** Append a new unlock; idempotent (no duplicates per (childId, creatureId)). */
    void recordUnlock(CreatureUnlock unlock);

    /** All creature ids the child has unlocked, insertion order. */
    List<String> unlockedCreatureIds(UUID childId);

    Set<String> unlockedSet(UUID childId);

    /** Removes the creature inventory for the given child (UC-011). */
    int deleteByChildId(UUID childId);
}
