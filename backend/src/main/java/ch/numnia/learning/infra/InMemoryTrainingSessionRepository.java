package ch.numnia.learning.infra;

import ch.numnia.learning.domain.TrainingSession;
import ch.numnia.learning.spi.TrainingSessionRepository;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryTrainingSessionRepository implements TrainingSessionRepository {

    private final Map<UUID, TrainingSession> store = new ConcurrentHashMap<>();

    @Override
    public TrainingSession save(TrainingSession session) {
        store.put(session.id(), session);
        return session;
    }

    @Override
    public Optional<TrainingSession> findById(UUID id) {
        return Optional.ofNullable(store.get(id));
    }
}
