package ch.numnia.parentcontrols.spi;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Tracks per-child play time per calendar day (UC-009 BR-001).
 *
 * <p>Backed in-memory in this iteration; will move to Postgres alongside
 * other repositories.
 */
public interface PlayTimeLedger {

    /** Returns minutes already played by the child today (UTC calendar day). */
    int minutesPlayedToday(UUID childId, LocalDate today);

    /** Records additional minutes played by the child today. */
    void addMinutes(UUID childId, LocalDate today, int minutes);
}
