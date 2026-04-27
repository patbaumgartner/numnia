package ch.numnia.learning.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Server-side state of an active training session.
 */
public final class TrainingSession {

    private final UUID id;
    private final UUID childId;
    private final Operation operation;
    private final Instant startedAt;
    private Instant endedAt;
    private int currentDifficulty;
    private int currentSpeed;
    private int totalTasks;
    private int correctTasks;
    private int consecutiveErrors;
    private ModeSuggestion modeSuggestion = ModeSuggestion.NONE;
    private final List<UUID> taskIds = new ArrayList<>();
    private MathTask currentTask;

    public TrainingSession(UUID id, UUID childId, Operation operation,
                           int initialDifficulty, int initialSpeed,
                           Instant startedAt) {
        this.id = id;
        this.childId = childId;
        this.operation = operation;
        this.currentDifficulty = initialDifficulty;
        this.currentSpeed = initialSpeed;
        this.startedAt = startedAt;
    }

    public UUID id() { return id; }
    public UUID childId() { return childId; }
    public Operation operation() { return operation; }
    public Instant startedAt() { return startedAt; }
    public Instant endedAt() { return endedAt; }
    public int currentDifficulty() { return currentDifficulty; }
    public int currentSpeed() { return currentSpeed; }
    public int totalTasks() { return totalTasks; }
    public int correctTasks() { return correctTasks; }
    public int consecutiveErrors() { return consecutiveErrors; }
    public ModeSuggestion modeSuggestion() { return modeSuggestion; }
    public List<UUID> taskIds() { return List.copyOf(taskIds); }
    public MathTask currentTask() { return currentTask; }

    public void recordTask(MathTask task) {
        this.currentTask = task;
        this.taskIds.add(task.id());
    }

    public void recordOutcome(AnswerOutcome outcome) {
        totalTasks++;
        if (outcome == AnswerOutcome.CORRECT) {
            correctTasks++;
            consecutiveErrors = 0;
        } else {
            consecutiveErrors++;
        }
    }

    public void setCurrentSpeed(int speed) { this.currentSpeed = speed; }
    public void setModeSuggestion(ModeSuggestion s) { this.modeSuggestion = s; }
    public void resetConsecutiveErrors() { this.consecutiveErrors = 0; }
    public void end(Instant when) { this.endedAt = when; }

    public double accuracy() {
        return totalTasks == 0 ? 0.0 : (double) correctTasks / totalTasks;
    }
}
