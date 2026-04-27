package ch.numnia.parentcontrols.domain;

/**
 * Auditable actions for parent-control changes (UC-009 BR-004).
 */
public enum ControlsAction {
    CONTROLS_UPDATED,
    NO_LIMIT_CONFIRMED,
    RISK_MECHANIC_ENABLED,
    RISK_MECHANIC_DISABLED,
    SESSION_TERMINATED_BY_LIMIT,
    NEW_SESSION_BLOCKED_BY_LIMIT,
    RISK_OUTCOME_RESTORED
}
