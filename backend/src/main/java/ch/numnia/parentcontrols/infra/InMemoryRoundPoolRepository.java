package ch.numnia.parentcontrols.infra;

import ch.numnia.parentcontrols.domain.RoundPoolSnapshot;
import ch.numnia.parentcontrols.spi.RoundPoolRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Repository
public class InMemoryRoundPoolRepository implements RoundPoolRepository {

    private final ConcurrentMap<UUID, RoundPoolSnapshot> store = new ConcurrentHashMap<>();

    @Override
    public RoundPoolSnapshot get(UUID childId) {
        return store.getOrDefault(childId, RoundPoolSnapshot.EMPTY);
    }

    @Override
    public void put(UUID childId, RoundPoolSnapshot snapshot) {
        store.put(childId, snapshot);
    }

    @Override
    public void clear(UUID childId) {
        store.remove(childId);
    }
}
