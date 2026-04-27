package ch.numnia.parentcontrols.spi;

import ch.numnia.parentcontrols.domain.ChildControls;
import java.util.Optional;
import java.util.UUID;

public interface ChildControlsRepository {
    Optional<ChildControls> findByChildId(UUID childId);
    void save(ChildControls controls);
}
