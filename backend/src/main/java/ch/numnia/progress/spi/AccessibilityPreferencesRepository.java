package ch.numnia.progress.spi;

import ch.numnia.progress.domain.ColorPalette;

import java.util.UUID;

/**
 * Per-child accessibility preferences for the progress view (NFR-A11Y-003).
 *
 * <p>Deliberately separate from {@code ChildPreferencesRepository} (worlds
 * module) to keep module boundaries clean — the worlds module should not
 * depend on the progress module.
 */
public interface AccessibilityPreferencesRepository {

    ColorPalette getPalette(UUID childId);

    void setPalette(UUID childId, ColorPalette palette);
}
