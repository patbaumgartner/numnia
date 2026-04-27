package ch.numnia.worlds.spi;

import java.util.UUID;

/** Per-child accessibility / display preferences (NFR-A11Y-003). */
public interface ChildPreferencesRepository {

    boolean isReducedMotion(UUID childId);

    void setReducedMotion(UUID childId, boolean enabled);
}
