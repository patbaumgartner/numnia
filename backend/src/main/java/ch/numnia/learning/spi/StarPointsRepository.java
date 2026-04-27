package ch.numnia.learning.spi;

import java.util.UUID;

/** Star points balance per child (FR-GAM-001, BR-002). */
public interface StarPointsRepository {
    int balanceOf(UUID childId);
    int addPoints(UUID childId, int delta);
    void setBalance(UUID childId, int balance);
}
