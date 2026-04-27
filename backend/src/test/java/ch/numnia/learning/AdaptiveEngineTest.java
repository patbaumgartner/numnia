package ch.numnia.learning;

import ch.numnia.learning.domain.AnswerOutcome;
import ch.numnia.learning.domain.ModeSuggestion;
import ch.numnia.learning.domain.Operation;
import ch.numnia.learning.domain.TrainingSession;
import ch.numnia.learning.service.AdaptiveEngine;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AdaptiveEngineTest {

    private final AdaptiveEngine engine = new AdaptiveEngine();

    @Test
    void applyAfterAnswer_threeConsecutiveErrors_downgradesSpeedAndSuggestsAccuracy() {
        TrainingSession session = new TrainingSession(
                UUID.randomUUID(), UUID.randomUUID(), Operation.MULTIPLICATION,
                3, 3, Instant.now());

        session.recordOutcome(AnswerOutcome.WRONG);
        session.recordOutcome(AnswerOutcome.WRONG);
        session.recordOutcome(AnswerOutcome.TIMEOUT);
        ModeSuggestion suggestion = engine.applyAfterAnswer(session);

        assertThat(session.currentSpeed()).isEqualTo(2);
        assertThat(suggestion).isEqualTo(ModeSuggestion.ACCURACY);
        assertThat(session.consecutiveErrors()).isZero();
    }

    @Test
    void applyAfterAnswer_twoErrors_doesNotDowngrade() {
        TrainingSession session = new TrainingSession(
                UUID.randomUUID(), UUID.randomUUID(), Operation.ADDITION,
                3, 3, Instant.now());
        session.recordOutcome(AnswerOutcome.WRONG);
        session.recordOutcome(AnswerOutcome.WRONG);

        ModeSuggestion suggestion = engine.applyAfterAnswer(session);

        assertThat(session.currentSpeed()).isEqualTo(3);
        assertThat(suggestion).isEqualTo(ModeSuggestion.NONE);
    }

    @Test
    void applyAfterAnswer_correctAnswerResetsErrorChain() {
        TrainingSession session = new TrainingSession(
                UUID.randomUUID(), UUID.randomUUID(), Operation.ADDITION,
                3, 3, Instant.now());
        session.recordOutcome(AnswerOutcome.WRONG);
        session.recordOutcome(AnswerOutcome.WRONG);
        session.recordOutcome(AnswerOutcome.CORRECT);

        ModeSuggestion suggestion = engine.applyAfterAnswer(session);

        assertThat(session.currentSpeed()).isEqualTo(3);
        assertThat(suggestion).isEqualTo(ModeSuggestion.NONE);
    }

    @Test
    void applyAfterAnswer_speedDowngradeRespectsMinimum() {
        TrainingSession session = new TrainingSession(
                UUID.randomUUID(), UUID.randomUUID(), Operation.ADDITION,
                3, 1, Instant.now());
        session.recordOutcome(AnswerOutcome.WRONG);
        session.recordOutcome(AnswerOutcome.WRONG);
        session.recordOutcome(AnswerOutcome.WRONG);
        engine.applyAfterAnswer(session);

        assertThat(session.currentSpeed()).isEqualTo(AdaptiveEngine.MIN_SPEED);
    }

    /**
     * UC-004 BR-001: accuracy-mode (G0) sessions are not subject to speed
     * adjustments by the adaptive engine.
     */
    @Test
    void applyAfterAnswer_inAccuracyMode_doesNotAdjustSpeedOrSuggestMode() {
        TrainingSession session = new TrainingSession(
                UUID.randomUUID(), UUID.randomUUID(), Operation.ADDITION,
                3, 0, true, Instant.now());
        session.recordOutcome(AnswerOutcome.WRONG);
        session.recordOutcome(AnswerOutcome.WRONG);
        session.recordOutcome(AnswerOutcome.WRONG);

        ModeSuggestion suggestion = engine.applyAfterAnswer(session);

        assertThat(session.currentSpeed()).isZero();
        assertThat(suggestion).isEqualTo(ModeSuggestion.NONE);
        assertThat(session.consecutiveErrors()).isEqualTo(3);
    }
}
