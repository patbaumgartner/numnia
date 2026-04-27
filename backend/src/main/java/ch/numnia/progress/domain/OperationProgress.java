package ch.numnia.progress.domain;

import ch.numnia.learning.domain.MasteryStatus;
import ch.numnia.learning.domain.Operation;

/**
 * Per-operation aggregated progress for a child (UC-008).
 *
 * <p>BR-001: only data of the own profile is exposed. BR-002: no
 * comparative fields against other children — by construction.
 *
 * @param operation       the arithmetic operation
 * @param totalSessions   number of completed training sessions
 * @param totalTasks      number of tasks answered across those sessions
 * @param correctTasks    number of correctly answered tasks
 * @param accuracy        ratio in [0.0, 1.0]
 * @param masteryStatus   current mastery status (NOT_STARTED / IN_CONSOLIDATION / MASTERED)
 * @param currentDifficulty current S-level
 */
public record OperationProgress(
        Operation operation,
        int totalSessions,
        int totalTasks,
        int correctTasks,
        double accuracy,
        MasteryStatus masteryStatus,
        int currentDifficulty) {
}
