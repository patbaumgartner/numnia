package ch.numnia.creatures.spi;

import ch.numnia.creatures.domain.CreatureAuditAction;
import ch.numnia.creatures.domain.CreatureAuditEntry;

import java.util.List;
import java.util.UUID;

public interface CreatureAuditRepository {

    void append(CreatureAuditEntry entry);

    List<CreatureAuditAction> actionsFor(UUID childId);
}
