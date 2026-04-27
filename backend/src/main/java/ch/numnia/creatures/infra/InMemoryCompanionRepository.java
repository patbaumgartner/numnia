package ch.numnia.creatures.infra;

import ch.numnia.creatures.spi.CompanionRepository;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryCompanionRepository implements CompanionRepository {

    private final Map<UUID, String> companions = new ConcurrentHashMap<>();

    @Override
    public Optional<String> findCompanion(UUID childId) {
        return Optional.ofNullable(companions.get(childId));
    }

    @Override
    public void setCompanion(UUID childId, String creatureId) {
        companions.put(childId, creatureId);
    }
}
