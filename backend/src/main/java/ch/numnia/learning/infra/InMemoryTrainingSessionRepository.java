package ch.numnia.learning.infra;

import ch.numnia.learning.domain.Operation;
import ch.numnia.learning.domain.TrainingSession;
import ch.numnia.learning.spi.TrainingSessionRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
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

    @Override
    public List<TrainingSession> findEndedByChildAndOperation(UUID childId, Operation operation) {
        return store.values().stream()
                .filter(s -> s.childId().equals(childId))
                .filter(s -> s.operation() == operation)
                .filter(s -> s.endedAt() != null)
                .toList();
    }

    @Override
    public int deleteByChildId(UUID childId) {
        int removed = 0;
        for (TrainingSession s : List.copyOf(store.values())) {
            if (s.childId().equals(childId)) {
                store.remove(s.id());
                removed++;
            }
        }
        return removed;
    }
}
