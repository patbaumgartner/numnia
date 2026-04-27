package ch.numnia.learning.domain;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Per-child, per-operation learning progress (FR-LEARN-009, BR-004).
 */
public final class LearningProgress {

    private final UUID childId;
    private final Operation operation;
    private int currentDifficulty;
    private int currentSpeed;
    private MasteryStatus masteryStatus;
    private LocalDate firstQualifiedDate;

    public LearningProgress(UUID childId, Operation operation,
                            int currentDifficulty, int currentSpeed) {
        this.childId = childId;
        this.operation = operation;
        this.currentDifficulty = currentDifficulty;
        this.currentSpeed = currentSpeed;
        this.masteryStatus = MasteryStatus.NOT_STARTED;
    }

    public UUID childId() { return childId; }
    public Operation operation() { return operation; }
    public int currentDifficulty() { return currentDifficulty; }
    public int currentSpeed() { return currentSpeed; }
    public MasteryStatus masteryStatus() { return masteryStatus; }
    public LocalDate firstQualifiedDate() { return firstQualifiedDate; }

    public void setCurrentSpeed(int speed) { this.currentSpeed = speed; }
    public void setCurrentDifficulty(int difficulty) { this.currentDifficulty = difficulty; }
    public void setMasteryStatus(MasteryStatus s) { this.masteryStatus = s; }
    public void setFirstQualifiedDate(LocalDate d) { this.firstQualifiedDate = d; }
}
