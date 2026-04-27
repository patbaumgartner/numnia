package ch.numnia.avatar.infra;

import ch.numnia.avatar.domain.InventoryEntry;
import ch.numnia.avatar.spi.InventoryRepository;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Repository
public class InMemoryInventoryRepository implements InventoryRepository {

    private final Map<UUID, List<InventoryEntry>> byChild = new ConcurrentHashMap<>();

    @Override
    public Set<String> ownedItemIds(UUID childId) {
        return entriesFor(childId).stream()
                .map(InventoryEntry::itemId)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public List<InventoryEntry> entriesFor(UUID childId) {
        return List.copyOf(byChild.getOrDefault(childId, List.of()));
    }

    @Override
    public void recordPurchase(InventoryEntry entry) {
        byChild.computeIfAbsent(entry.childId(), id -> new CopyOnWriteArrayList<>()).add(entry);
    }

    @Override
    public int deleteByChildId(UUID childId) {
        List<InventoryEntry> removed = byChild.remove(childId);
        return removed == null ? 0 : removed.size();
    }
}
