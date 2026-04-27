package ch.numnia.creatures.domain;

import java.time.Instant;
import java.util.UUID;

/** Audit-trail entry for the creatures module (UC-006). */
public record CreatureAuditEntry(
        UUID childId,
        CreatureAuditAction action,
        String creatureId,
        Instant occurredAt) {

    public CreatureAuditEntry {
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
