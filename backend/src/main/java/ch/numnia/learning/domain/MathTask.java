package ch.numnia.learning.domain;

import java.util.UUID;

/**
 * A generated math task (FR-LEARN-003, BR-001).
 *
 * <p>{@code expectedAnswer} is kept on the server-side instance for evaluation;
 * it is never returned to the client.
 */
public record MathTask(
        UUID id,
        Operation operation,
        int operandA,
        int operandB,
        int expectedAnswer,
        int difficulty,
        int speed) {

    public static final int MAX_RESULT = 1_000_000;

    public MathTask {
        if (expectedAnswer < 0 || expectedAnswer > MAX_RESULT) {
            throw new IllegalArgumentException(
                    "expectedAnswer outside [0, " + MAX_RESULT + "]: " + expectedAnswer);
        }
    }

    /** UC-004 BR-001: a task is timed iff its speed level is greater than zero (G0 = no timer). */
    public boolean timed() {
        return speed > 0;
    }
}
