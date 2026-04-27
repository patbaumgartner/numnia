package ch.numnia.learning.spi;

import ch.numnia.learning.domain.LearningProgress;
import ch.numnia.learning.domain.Operation;

import java.util.Optional;
import java.util.UUID;

public interface LearningProgressRepository {
    Optional<LearningProgress> findByChildAndOperation(UUID childId, Operation op);
    LearningProgress save(LearningProgress progress);

    /** Removes all progress rows for the given child (UC-011). */
    int deleteByChildId(UUID childId);
}
