package ch.numnia.creatures.domain;

import ch.numnia.learning.domain.Operation;

/**
 * A collectible creature (UC-006, FR-CRE-001/002/007).
 *
 * <p>BR-002: creature names must support variable endings — the validation
 * here intentionally does <em>not</em> enforce a particular ending pattern
 * (e.g. trailing "i"). Only generic safety checks apply: non-blank, no
 * sharp s (NFR-I18N-004).
 *
 * @param id            stable slug used as REST path parameter (e.g. "pilzar")
 * @param displayName   UI label in Swiss High German with umlauts, no sharp s
 * @param operation     learning operation whose mastery unlocks the creature
 * @param sourceWorldId id of the world the creature thematically belongs to
 */
public record Creature(
        String id,
        String displayName,
        Operation operation,
        String sourceWorldId) {

    public Creature {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("creature id must not be blank");
        }
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("creature displayName must not be blank");
        }
        if (displayName.contains("ß")) {
            throw new IllegalArgumentException("displayName must not contain sharp s");
        }
        if (operation == null) {
            throw new IllegalArgumentException("operation must not be null");
        }
        if (sourceWorldId == null || sourceWorldId.isBlank()) {
            throw new IllegalArgumentException("sourceWorldId must not be blank");
        }
    }
}
