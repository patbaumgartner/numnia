package ch.numnia.learning.domain;

import java.time.Instant;
import java.util.UUID;

public record LearningAuditEntry(
        UUID id,
        UUID childRef,
        LearningAuditAction action,
        String detail,
        Instant timestamp) {
}
