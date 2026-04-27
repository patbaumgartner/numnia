package ch.numnia.learning.service;

import ch.numnia.learning.domain.AnswerOutcome;
import ch.numnia.learning.spi.StarPointsRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Star-point bookkeeping (FR-GAM-001, BR-002, FR-GAM-005).
 * Errors do NOT deduct points.
 */
@Service
public class StarPointsService {

    static final int POINTS_PER_CORRECT = 1;

    private final StarPointsRepository balances;

    public StarPointsService(StarPointsRepository balances) {
        this.balances = balances;
    }

    public int reward(UUID childId, AnswerOutcome outcome) {
        if (outcome == AnswerOutcome.CORRECT) {
            return balances.addPoints(childId, POINTS_PER_CORRECT);
        }
        return balances.balanceOf(childId);
    }

    public int balanceOf(UUID childId) {
        return balances.balanceOf(childId);
    }
}
