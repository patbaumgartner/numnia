package ch.numnia.avatar.spi;

import ch.numnia.avatar.domain.ShopAuditAction;
import ch.numnia.avatar.domain.ShopAuditEntry;

import java.util.List;
import java.util.UUID;

public interface ShopAuditRepository {

    void append(ShopAuditEntry entry);

    List<ShopAuditAction> actionsFor(UUID childId);
}
