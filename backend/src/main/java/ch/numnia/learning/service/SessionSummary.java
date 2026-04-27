package ch.numnia.learning.service;

import ch.numnia.learning.domain.MasteryStatus;

import java.util.UUID;

/** Summary returned to the child at session end (UC-003 main flow step 12). */
public record SessionSummary(
        UUID sessionId,
        int totalTasks,
        int correctTasks,
        int starPointsBalance,
        MasteryStatus masteryStatus) {
}
