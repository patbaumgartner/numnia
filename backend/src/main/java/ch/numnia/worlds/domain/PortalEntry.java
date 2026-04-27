package ch.numnia.worlds.domain;

/**
 * Result of a portal-entry attempt (UC-005 main flow step 6).
 */
public record PortalEntry(
        String worldId,
        PortalType portalType,
        boolean locked,
        String target,
        String messageCode,
        boolean reducedMotion) {

    public static final String TARGET_PRACTICE_STAGE = "PRACTICE_STAGE";

    public static final String CODE_COMING_LATER = "WORLD_PORTAL_COMING_LATER";
    public static final String CODE_LEVEL_TOO_LOW = "WORLD_PORTAL_LEVEL_TOO_LOW";
    public static final String CODE_TASK_POOL_MISSING = "WORLD_PORTAL_TASK_POOL_MISSING";

    public static PortalEntry opened(String worldId, PortalType type, boolean reducedMotion) {
        return new PortalEntry(worldId, type, false, TARGET_PRACTICE_STAGE, null, reducedMotion);
    }

    public static PortalEntry locked(String worldId, PortalType type, String code, boolean reducedMotion) {
        return new PortalEntry(worldId, type, true, null, code, reducedMotion);
    }
}
