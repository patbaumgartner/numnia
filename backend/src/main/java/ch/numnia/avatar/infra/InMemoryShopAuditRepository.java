package ch.numnia.avatar.infra;

import ch.numnia.avatar.domain.ShopAuditAction;
import ch.numnia.avatar.domain.ShopAuditEntry;
import ch.numnia.avatar.spi.ShopAuditRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

@Repository
public class InMemoryShopAuditRepository implements ShopAuditRepository {

    private final List<ShopAuditEntry> entries = new CopyOnWriteArrayList<>();

    @Override
    public void append(ShopAuditEntry entry) {
        entries.add(entry);
    }

    @Override
    public List<ShopAuditAction> actionsFor(UUID childId) {
        return entries.stream()
                .filter(e -> e.childId().equals(childId))
                .map(ShopAuditEntry::action)
                .toList();
    }
}
