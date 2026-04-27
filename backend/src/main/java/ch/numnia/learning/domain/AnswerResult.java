package ch.numnia.learning.domain;

/**
 * Result of submitting an answer in a training session.
 *
 * @param outcome           CORRECT / WRONG / TIMEOUT
 * @param currentSpeed      G level after the adaptive engine has been applied
 * @param modeSuggestion    suggested mode change, NONE if no trigger fired
 * @param starPointsBalance star points balance after evaluation (errors do not
 *                          deduct points, BR-002)
 */
public record AnswerResult(
        AnswerOutcome outcome,
        int currentSpeed,
        ModeSuggestion modeSuggestion,
        int starPointsBalance) {
}
