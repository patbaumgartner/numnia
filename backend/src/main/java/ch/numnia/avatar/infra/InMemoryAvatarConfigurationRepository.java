package ch.numnia.avatar.infra;

import ch.numnia.avatar.spi.AvatarConfigurationRepository;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryAvatarConfigurationRepository implements AvatarConfigurationRepository {

    private final Map<UUID, Map<String, String>> equipped = new ConcurrentHashMap<>();

    @Override
    public Map<String, String> equippedFor(UUID childId) {
        return Map.copyOf(equipped.getOrDefault(childId, Map.of()));
    }

    @Override
    public void equip(UUID childId, String slot, String itemId) {
        equipped.computeIfAbsent(childId, id -> new ConcurrentHashMap<>()).put(slot, itemId);
    }

    @Override
    public Optional<String> equippedAt(UUID childId, String slot) {
        return Optional.ofNullable(equipped.getOrDefault(childId, Map.of()).get(slot));
    }
}
