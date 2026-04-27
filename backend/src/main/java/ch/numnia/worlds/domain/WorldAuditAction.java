package ch.numnia.worlds.domain;

/** Audit actions emitted by the worlds module (UC-005). */
public enum WorldAuditAction {
    PORTAL_OPENED,
    PORTAL_LOCKED_RELEASE_RULE,
    PORTAL_LOCKED_LEVEL_RULE,
    PORTAL_LOCKED_TASK_POOL_MISSING,
    REDUCED_MOTION_APPLIED
}
