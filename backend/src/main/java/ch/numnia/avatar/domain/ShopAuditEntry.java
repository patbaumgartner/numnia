package ch.numnia.avatar.domain;

import java.time.Instant;
import java.util.UUID;

public record ShopAuditEntry(
        UUID childId,
        ShopAuditAction action,
        String itemId,
        Instant occurredAt) {

    public ShopAuditEntry {
        if (childId == null) {
            throw new IllegalArgumentException("childId must not be null");
        }
        if (action == null) {
            throw new IllegalArgumentException("action must not be null");
        }
        if (occurredAt == null) {
            throw new IllegalArgumentException("occurredAt must not be null");
        }
    }
}
