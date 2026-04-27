package ch.numnia.worlds.infra;

import ch.numnia.worlds.spi.ChildPreferencesRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Repository
public class InMemoryChildPreferencesRepository implements ChildPreferencesRepository {

    private final ConcurrentMap<UUID, Boolean> reducedMotion = new ConcurrentHashMap<>();

    @Override
    public boolean isReducedMotion(UUID childId) {
        return reducedMotion.getOrDefault(childId, Boolean.FALSE);
    }

    @Override
    public void setReducedMotion(UUID childId, boolean enabled) {
        reducedMotion.put(childId, enabled);
    }
}
