package ch.numnia.worlds.domain;

import java.time.Instant;
import java.util.UUID;

/** Append-only audit entry for the worlds module (UC-005). */
public record WorldAuditEntry(
        UUID childId,
        String worldId,
        PortalType portalType,
        WorldAuditAction action,
        Instant occurredAt) {
}
