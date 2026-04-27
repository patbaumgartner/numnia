package ch.numnia.learning.spi;

import ch.numnia.learning.domain.TrainingSession;

import java.util.Optional;
import java.util.UUID;

public interface TrainingSessionRepository {
    TrainingSession save(TrainingSession session);
    Optional<TrainingSession> findById(UUID id);
}
