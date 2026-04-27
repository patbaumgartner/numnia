package ch.numnia.creatures.spi;

import java.util.Optional;
import java.util.UUID;

/** Per-child active companion (FR-CRE-004, BR-003 — swappable any time). */
public interface CompanionRepository {

    Optional<String> findCompanion(UUID childId);

    void setCompanion(UUID childId, String creatureId);

    /** Removes the active companion for the given child (UC-011). */
    boolean deleteByChildId(UUID childId);
}
