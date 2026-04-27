package ch.numnia.parentcontrols.infra;

import ch.numnia.parentcontrols.domain.ControlsAuditEntry;
import ch.numnia.parentcontrols.spi.ControlsAuditRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

@Repository
public class InMemoryControlsAuditRepository implements ControlsAuditRepository {

    private final List<ControlsAuditEntry> entries = new CopyOnWriteArrayList<>();

    @Override
    public void append(ControlsAuditEntry entry) {
        entries.add(entry);
    }

    @Override
    public List<ControlsAuditEntry> findByChildId(UUID childId) {
        return entries.stream().filter(e -> e.childId().equals(childId)).toList();
    }

    @Override
    public List<ControlsAuditEntry> findByParentId(UUID parentId) {
        return entries.stream().filter(e -> e.parentId().equals(parentId)).toList();
    }
}
