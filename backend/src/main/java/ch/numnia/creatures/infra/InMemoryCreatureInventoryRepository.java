package ch.numnia.creatures.infra;

import ch.numnia.creatures.domain.CreatureUnlock;
import ch.numnia.creatures.spi.CreatureInventoryRepository;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryCreatureInventoryRepository implements CreatureInventoryRepository {

    private final Map<UUID, LinkedHashSet<String>> byChild = new ConcurrentHashMap<>();

    @Override
    public synchronized void recordUnlock(CreatureUnlock unlock) {
        byChild.computeIfAbsent(unlock.childId(), id -> new LinkedHashSet<>())
                .add(unlock.creatureId());
    }

    @Override
    public synchronized List<String> unlockedCreatureIds(UUID childId) {
        LinkedHashSet<String> set = byChild.get(childId);
        return set == null ? List.of() : List.copyOf(set);
    }

    @Override
    public synchronized Set<String> unlockedSet(UUID childId) {
        LinkedHashSet<String> set = byChild.get(childId);
        return set == null ? Set.of() : Set.copyOf(set);
    }
}
