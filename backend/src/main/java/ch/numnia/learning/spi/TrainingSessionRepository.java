package ch.numnia.learning.spi;

import ch.numnia.learning.domain.Operation;
import ch.numnia.learning.domain.TrainingSession;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TrainingSessionRepository {
    TrainingSession save(TrainingSession session);
    Optional<TrainingSession> findById(UUID id);

    /**
     * Returns all completed (i.e. ended) sessions for a given child and
     * operation. Used by the progress aggregation (UC-008).
     */
    List<TrainingSession> findEndedByChildAndOperation(UUID childId, Operation operation);

    /** Removes all training-session rows for the given child (UC-011). */
    int deleteByChildId(UUID childId);
}
