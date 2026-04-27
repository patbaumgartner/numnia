package ch.numnia.worlds.infra;

import ch.numnia.worlds.spi.WorldAuditRepository;
import ch.numnia.worlds.domain.WorldAuditAction;
import ch.numnia.worlds.domain.WorldAuditEntry;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Repository
public class InMemoryWorldAuditRepository implements WorldAuditRepository {

    private final ConcurrentMap<UUID, List<WorldAuditEntry>> store = new ConcurrentHashMap<>();

    @Override
    public synchronized void append(WorldAuditEntry entry) {
        store.computeIfAbsent(entry.childId(), id -> new ArrayList<>()).add(entry);
    }

    @Override
    public synchronized List<WorldAuditAction> actionsFor(UUID childId) {
        return store.getOrDefault(childId, Collections.emptyList())
                .stream()
                .map(WorldAuditEntry::action)
                .toList();
    }
}
