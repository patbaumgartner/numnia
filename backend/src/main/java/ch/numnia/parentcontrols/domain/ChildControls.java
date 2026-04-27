package ch.numnia.parentcontrols.domain;

import java.util.UUID;

/**
 * UC-009 — per-child play-time and risk-mechanic configuration.
 *
 * <p>Defaults (FR-PAR-001..003):
 * <ul>
 *   <li>{@code dailyLimitMinutes} = 30 (BR-001)
 *   <li>{@code breakRecommendationMinutes} = 15
 *   <li>{@code riskMechanicEnabled} = false (BR-002)
 * </ul>
 *
 * <p>{@code dailyLimitMinutes} may be {@code null} which encodes the
 * "no limit" alt-3a path. The parent must explicitly confirm that choice.
 */
public record ChildControls(
        UUID childId,
        UUID parentId,
        Integer dailyLimitMinutes,
        int breakRecommendationMinutes,
        boolean riskMechanicEnabled) {

    /** Default daily hard limit per FR-PAR-001 / spec main flow step 1. */
    public static final int DEFAULT_DAILY_LIMIT_MINUTES = 30;

    /** Default break recommendation. */
    public static final int DEFAULT_BREAK_RECOMMENDATION_MINUTES = 15;

    public static ChildControls defaults(UUID childId, UUID parentId) {
        return new ChildControls(
                childId,
                parentId,
                DEFAULT_DAILY_LIMIT_MINUTES,
                DEFAULT_BREAK_RECOMMENDATION_MINUTES,
                false);
    }

    public ChildControls {
        if (childId == null) throw new IllegalArgumentException("childId must not be null");
        if (parentId == null) throw new IllegalArgumentException("parentId must not be null");
        if (dailyLimitMinutes != null && dailyLimitMinutes <= 0) {
            throw new IllegalArgumentException("dailyLimitMinutes must be positive or null (no limit)");
        }
        if (breakRecommendationMinutes <= 0) {
            throw new IllegalArgumentException("breakRecommendationMinutes must be positive");
        }
    }

    public boolean noLimit() {
        return dailyLimitMinutes == null;
    }
}
