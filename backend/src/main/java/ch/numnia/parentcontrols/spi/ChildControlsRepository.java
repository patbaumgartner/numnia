package ch.numnia.parentcontrols.spi;

import ch.numnia.parentcontrols.domain.ChildControls;
import java.util.Optional;
import java.util.UUID;

public interface ChildControlsRepository {
    Optional<ChildControls> findByChildId(UUID childId);
    void save(ChildControls controls);

    /** Removes the per-child controls for the given child (UC-011). */
    boolean deleteByChildId(UUID childId);
}
