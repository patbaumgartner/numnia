package ch.numnia.parentcontrols.spi;

import ch.numnia.parentcontrols.domain.ControlsAuditEntry;
import java.util.List;
import java.util.UUID;

public interface ControlsAuditRepository {
    void append(ControlsAuditEntry entry);
    List<ControlsAuditEntry> findByChildId(UUID childId);
    List<ControlsAuditEntry> findByParentId(UUID parentId);
}
