package ch.numnia.worlds.domain;

/**
 * A discoverable game world (UC-005, FR-WORLD-001..005).
 *
 * <p>Identifier is the stable, locale-independent slug used by the API
 * (e.g. {@code "mushroom-jungle"}); {@link #displayName} carries the
 * Swiss High German UI label (NFR-I18N-002, no sharp s).
 *
 * @param id              stable slug used as REST path parameter
 * @param displayName     UI label in Swiss High German
 * @param difficultyLevel 1..3 — visual difficulty hint (BR-003)
 * @param requiredLevel   minimum competence level S1..S3 to enter the
 *                        training portal (BR-002)
 */
public record World(
        String id,
        String displayName,
        int difficultyLevel,
        int requiredLevel) {

    public World {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("world id must not be blank");
        }
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("world displayName must not be blank");
        }
        if (displayName.contains("ß")) {
            throw new IllegalArgumentException("displayName must not contain sharp s");
        }
        if (difficultyLevel < 1 || difficultyLevel > 3) {
            throw new IllegalArgumentException("difficultyLevel must be in [1,3]");
        }
        if (requiredLevel < 1 || requiredLevel > 3) {
            throw new IllegalArgumentException("requiredLevel must be in [1,3]");
        }
    }
}
