package ch.numnia.avatar.spi;

import ch.numnia.avatar.domain.InventoryEntry;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Inventory of permanently owned shop items per child (UC-007). */
public interface InventoryRepository {

    Set<String> ownedItemIds(UUID childId);

    List<InventoryEntry> entriesFor(UUID childId);

    void recordPurchase(InventoryEntry entry);

    /** Removes all inventory entries for the given child (UC-011). */
    int deleteByChildId(UUID childId);
}
