package ch.numnia.learning.infra;

import ch.numnia.learning.domain.LearningProgress;
import ch.numnia.learning.domain.Operation;
import ch.numnia.learning.spi.LearningProgressRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory repository for {@link LearningProgress}. Will be superseded by a
 * Postgres-backed JPA repository once UC-008 introduces real persistence.
 */
@Repository
public class InMemoryLearningProgressRepository implements LearningProgressRepository {

    private final Map<String, LearningProgress> store = new ConcurrentHashMap<>();

    @Override
    public Optional<LearningProgress> findByChildAndOperation(UUID childId, Operation op) {
        return Optional.ofNullable(store.get(key(childId, op)));
    }

    @Override
    public LearningProgress save(LearningProgress progress) {
        store.put(key(progress.childId(), progress.operation()), progress);
        return progress;
    }

    private static String key(UUID childId, Operation op) {
        return childId + "::" + op.name();
    }

    @Override
    public int deleteByChildId(UUID childId) {
        String prefix = childId + "::";
        int removed = 0;
        for (String k : Set.copyOf(store.keySet())) {
            if (k.startsWith(prefix)) {
                store.remove(k);
                removed++;
            }
        }
        return removed;
    }
}
