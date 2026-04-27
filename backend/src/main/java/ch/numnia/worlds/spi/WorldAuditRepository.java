package ch.numnia.worlds.spi;

import ch.numnia.worlds.domain.WorldAuditAction;
import ch.numnia.worlds.domain.WorldAuditEntry;

import java.util.List;
import java.util.UUID;

public interface WorldAuditRepository {

    void append(WorldAuditEntry entry);

    List<WorldAuditAction> actionsFor(UUID childId);
}
