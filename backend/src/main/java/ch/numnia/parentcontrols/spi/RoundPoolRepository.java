package ch.numnia.parentcontrols.spi;

import ch.numnia.parentcontrols.domain.RoundPoolSnapshot;
import java.util.UUID;

/**
 * Stores the per-match round-pool snapshot used by the risk mechanic
 * (UC-009 BR-003).
 */
public interface RoundPoolRepository {
    RoundPoolSnapshot get(UUID childId);
    void put(UUID childId, RoundPoolSnapshot snapshot);
    void clear(UUID childId);
}
