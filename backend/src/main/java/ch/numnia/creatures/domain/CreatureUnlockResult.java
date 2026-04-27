package ch.numnia.creatures.domain;

import java.util.List;

/**
 * Result of {@code processUnlocks} (UC-006 main flow step 1-3).
 *
 * @param newlyUnlocked     creatures unlocked in this call (may be empty)
 * @param consolationAwarded {@code true} when the threshold was reached
 *                           but all R1 creatures were already unlocked
 *                           (alternative flow 1a, FR-GAM-001)
 * @param starPointsAwarded star points granted as a consolation reward
 */
public record CreatureUnlockResult(
        List<Creature> newlyUnlocked,
        boolean consolationAwarded,
        int starPointsAwarded) {

    public CreatureUnlockResult {
        if (newlyUnlocked == null) {
            throw new IllegalArgumentException("newlyUnlocked must not be null");
        }
        newlyUnlocked = List.copyOf(newlyUnlocked);
        if (starPointsAwarded < 0) {
            throw new IllegalArgumentException("starPointsAwarded must be ≥ 0");
        }
    }
}
