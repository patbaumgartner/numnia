package ch.numnia.creatures.infra;

import ch.numnia.creatures.domain.CreatureAuditAction;
import ch.numnia.creatures.domain.CreatureAuditEntry;
import ch.numnia.creatures.spi.CreatureAuditRepository;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@Repository
public class InMemoryCreatureAuditRepository implements CreatureAuditRepository {

    private final List<CreatureAuditEntry> entries = new CopyOnWriteArrayList<>();

    @Override
    public void append(CreatureAuditEntry entry) {
        entries.add(entry);
    }

    @Override
    public List<CreatureAuditAction> actionsFor(UUID childId) {
        return entries.stream()
                .filter(e -> e.childId().equals(childId))
                .map(CreatureAuditEntry::action)
                .toList();
    }
}
