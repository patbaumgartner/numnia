package ch.numnia.learning.service;

import ch.numnia.learning.domain.MathTask;
import ch.numnia.learning.domain.Operation;

import java.util.List;
import java.util.UUID;

/**
 * Animated solution steps shown when the child taps "Show explanation"
 * in accuracy mode (UC-004 alt-flow 5a, FR-LEARN-008).
 *
 * <p>Each step is a Swiss-High-German sentence (no sharp s) that the UI
 * renders as one frame of the animation. Steps are computed on the server
 * from the current task and never reveal the answer prematurely; the final
 * step states the result.</p>
 */
public record ExplanationSteps(UUID taskId, Operation operation, List<String> steps) {

    public ExplanationSteps {
        if (steps == null || steps.isEmpty()) {
            throw new IllegalArgumentException("explanation steps must not be empty");
        }
        steps = List.copyOf(steps);
    }

    /** Produce a minimal multi-step explanation for the given task. */
    public static ExplanationSteps forTask(MathTask task) {
        String op = symbol(task.operation());
        List<String> steps = List.of(
                "Schau die Aufgabe an: " + task.operandA() + " " + op + " " + task.operandB() + ".",
                "Stelle die Zahlen nebeneinander und rechne Schritt fuer Schritt.",
                "Loesung: " + task.operandA() + " " + op + " " + task.operandB()
                        + " = " + task.expectedAnswer() + "."
        );
        return new ExplanationSteps(task.id(), task.operation(), steps);
    }

    private static String symbol(Operation operation) {
        return switch (operation) {
            case ADDITION -> "+";
            case SUBTRACTION -> "-";
            case MULTIPLICATION -> "x";
            case DIVISION -> ":";
        };
    }
}
