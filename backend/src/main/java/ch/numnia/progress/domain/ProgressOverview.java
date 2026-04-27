package ch.numnia.progress.domain;

import java.util.List;
import java.util.UUID;

/**
 * Overall progress overview for one child (UC-008 main flow).
 *
 * <p>Carries the active color palette (NFR-A11Y-003) and per-operation
 * aggregates. Contains no fields that compare against other children
 * (BR-002).
 */
public record ProgressOverview(
        UUID childId,
        ColorPalette palette,
        List<OperationProgress> entries,
        boolean empty) {

    public ProgressOverview {
        entries = List.copyOf(entries);
    }
}
