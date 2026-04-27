package ch.numnia.worlds.domain;

import java.util.List;

/**
 * Available portal types per world (SRS 12.1). Only {@link #TRAINING}
 * is openable in Release 1; the others are visibly locked
 * with a "coming later" notice (UC-005 BR-001).
 */
public enum PortalType {
    TRAINING(true),
    DUEL(false),
    TEAM(false),
    EVENT(false),
    BOSS(false),
    CLASS(false),
    SEASON(false);

    private final boolean availableInRelease1;

    PortalType(boolean availableInRelease1) {
        this.availableInRelease1 = availableInRelease1;
    }

    public boolean availableInRelease1() {
        return availableInRelease1;
    }

    public static List<PortalType> all() {
        return List.of(values());
    }
}
