package ch.numnia.avatar.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Permanent record that a child owns a shop item (UC-007, BR-003 / FR-CRE-006).
 *
 * <p>Items are never removed from inventory once purchased — loss through
 * errors is excluded (FR-GAM-005).
 */
public record InventoryEntry(UUID childId, String itemId, Instant purchasedAt) {

    public InventoryEntry {
        if (childId == null) {
            throw new IllegalArgumentException("childId must not be null");
        }
        if (itemId == null || itemId.isBlank()) {
            throw new IllegalArgumentException("itemId must not be blank");
        }
        if (purchasedAt == null) {
            throw new IllegalArgumentException("purchasedAt must not be null");
        }
    }
}
