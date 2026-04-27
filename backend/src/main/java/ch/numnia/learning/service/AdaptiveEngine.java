package ch.numnia.learning.service;

import ch.numnia.learning.domain.ModeSuggestion;
import ch.numnia.learning.domain.TrainingSession;
import org.springframework.stereotype.Component;

/**
 * Adaptive engine — speed/mode adjustments after each answer (BR-003).
 *
 * <p>Three consecutive errors lower G by one (minimum {@link #MIN_SPEED}) and
 * emit a {@link ModeSuggestion#ACCURACY} suggestion.
 */
@Component
public class AdaptiveEngine {

    public static final int MIN_SPEED = 1;
    public static final int ERROR_TRIGGER = 3;

    public ModeSuggestion applyAfterAnswer(TrainingSession session) {
        // UC-004 BR-001: accuracy mode (G0) never has its speed adjusted.
        if (session.accuracyMode()) {
            return ModeSuggestion.NONE;
        }
        if (session.consecutiveErrors() >= ERROR_TRIGGER) {
            int newSpeed = Math.max(MIN_SPEED, session.currentSpeed() - 1);
            session.setCurrentSpeed(newSpeed);
            session.setModeSuggestion(ModeSuggestion.ACCURACY);
            session.resetConsecutiveErrors();
            return ModeSuggestion.ACCURACY;
        }
        return ModeSuggestion.NONE;
    }
}
