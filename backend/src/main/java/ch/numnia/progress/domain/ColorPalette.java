package ch.numnia.progress.domain;

/**
 * Color palette for the progress visualization (NFR-A11Y-003).
 *
 * <p>The {@link #DEFAULT} palette is used by default. Color-blind profiles
 * map to the corresponding palette so the child can still distinguish
 * mastery / in-consolidation / not-started markers.
 */
public enum ColorPalette {
    DEFAULT,
    DEUTERANOPIA,
    PROTANOPIA,
    TRITANOPIA
}
