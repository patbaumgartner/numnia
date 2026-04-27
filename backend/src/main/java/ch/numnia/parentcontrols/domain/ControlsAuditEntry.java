package ch.numnia.parentcontrols.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Auditable record of a parent-control change (UC-009 BR-004).
 *
 * <p>{@code beforeValue} and {@code afterValue} are short, human-readable
 * representations (e.g. {@code "30"} and {@code "45"}); never PII.
 */
public record ControlsAuditEntry(
        UUID parentId,
        UUID childId,
        ControlsAction action,
        String field,
        String beforeValue,
        String afterValue,
        Instant timestamp) {
}
