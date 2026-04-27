package ch.numnia.creatures.domain;

/**
 * Read model for one row of the gallery (UC-006 main flow step 4).
 *
 * <p>Locked creatures are still visible (silhouette) but {@link #unlocked}
 * is {@code false}; the {@code id} and {@code displayName} carry the same
 * values as the unlocked card so the UI can render a consistent layout.
 */
public record GalleryEntry(
        Creature creature,
        boolean unlocked,
        boolean isCompanion) {
}
