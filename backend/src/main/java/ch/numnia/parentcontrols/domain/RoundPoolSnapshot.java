package ch.numnia.parentcontrols.domain;

/**
 * Snapshot of a child's "round pool" inside the active match while the
 * risk mechanic is enabled (UC-009 BR-003, FR-GAM-005, FR-GAM-006).
 *
 * <p>Wrong answers move star points and items into the round pool; at the
 * end of the match they are returned untouched, so the mechanic never
 * causes permanent loss.
 */
public record RoundPoolSnapshot(int starPointsInPool, int itemsInPool) {

    public static final RoundPoolSnapshot EMPTY = new RoundPoolSnapshot(0, 0);
}
