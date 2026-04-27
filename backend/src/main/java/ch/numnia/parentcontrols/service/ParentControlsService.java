package ch.numnia.parentcontrols.service;

import ch.numnia.iam.spi.ChildSessionRepository;
import ch.numnia.parentcontrols.domain.ChildControls;
import ch.numnia.parentcontrols.domain.ControlsAction;
import ch.numnia.parentcontrols.domain.ControlsAuditEntry;
import ch.numnia.parentcontrols.spi.ChildControlsRepository;
import ch.numnia.parentcontrols.spi.ControlsAuditRepository;
import ch.numnia.parentcontrols.spi.PlayTimeLedger;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * UC-009 — Parent self-service for daily play-time hard limit and risk
 * mechanic.
 *
 * <p>BR-001 daily limit is enforced server-side: when an update reduces the
 * limit below the minutes already played today, the running session is
 * revoked and {@link #canStartSession(UUID)} returns {@code false} for the
 * remainder of the calendar day.
 *
 * <p>BR-002 risk mechanic defaults to {@code false}; can be toggled at any
 * time. BR-004 every change is appended to {@link ControlsAuditRepository}
 * with before/after value and timestamp; no PII is persisted alongside.
 */
@Service
public class ParentControlsService {

    private final ChildControlsRepository controlsRepo;
    private final ControlsAuditRepository auditRepo;
    private final PlayTimeLedger playTimeLedger;
    private final ChildSessionRepository childSessionRepo;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public ParentControlsService(ChildControlsRepository controlsRepo,
                                 ControlsAuditRepository auditRepo,
                                 PlayTimeLedger playTimeLedger,
                                 ChildSessionRepository childSessionRepo) {
        this(controlsRepo, auditRepo, playTimeLedger, childSessionRepo, Clock.systemUTC());
    }

    public ParentControlsService(ChildControlsRepository controlsRepo,
                                 ControlsAuditRepository auditRepo,
                                 PlayTimeLedger playTimeLedger,
                                 ChildSessionRepository childSessionRepo,
                                 Clock clock) {
        this.controlsRepo = controlsRepo;
        this.auditRepo = auditRepo;
        this.playTimeLedger = playTimeLedger;
        this.childSessionRepo = childSessionRepo;
        this.clock = clock;
    }

    /** Returns the current controls; if none exist yet, returns the defaults
     * (FR-PAR-001..003: 30 min, 15 min, risk OFF). */
    public ChildControls getOrDefault(UUID parentId, UUID childId) {
        if (parentId == null) throw new IllegalArgumentException("parentId must not be null");
        if (childId == null) throw new IllegalArgumentException("childId must not be null");
        return controlsRepo.findByChildId(childId)
                .map(c -> {
                    if (!c.parentId().equals(parentId)) {
                        throw new UnauthorizedControlsAccessException(
                                "parent does not own this child profile");
                    }
                    return c;
                })
                .orElseGet(() -> ChildControls.defaults(childId, parentId));
    }

    /**
     * Updates a child's controls and emits one audit entry per changed field.
     *
     * <p>Picking "no limit" requires {@code confirmNoLimit=true} (alt 3a);
     * otherwise {@link NoLimitConfirmationRequiredException} is thrown and no
     * change is persisted.
     */
    public ChildControls updateControls(UUID parentId,
                                        UUID childId,
                                        Integer dailyLimitMinutes,
                                        int breakRecommendationMinutes,
                                        boolean riskMechanicEnabled,
                                        boolean confirmNoLimit) {
        if (parentId == null) throw new IllegalArgumentException("parentId must not be null");
        if (childId == null) throw new IllegalArgumentException("childId must not be null");
        if (dailyLimitMinutes == null && !confirmNoLimit) {
            throw new NoLimitConfirmationRequiredException(
                    "no-limit must be explicitly confirmed by the parent (UC-009 alt 3a)");
        }

        ChildControls before = getOrDefault(parentId, childId);
        ChildControls after = new ChildControls(
                childId, parentId,
                dailyLimitMinutes,
                breakRecommendationMinutes,
                riskMechanicEnabled);

        controlsRepo.save(after);

        Instant now = Instant.now(clock);
        if (!java.util.Objects.equals(before.dailyLimitMinutes(), after.dailyLimitMinutes())) {
            auditRepo.append(new ControlsAuditEntry(
                    parentId, childId, ControlsAction.CONTROLS_UPDATED,
                    "dailyLimitMinutes",
                    String.valueOf(before.dailyLimitMinutes()),
                    String.valueOf(after.dailyLimitMinutes()),
                    now));
        }
        if (before.breakRecommendationMinutes() != after.breakRecommendationMinutes()) {
            auditRepo.append(new ControlsAuditEntry(
                    parentId, childId, ControlsAction.CONTROLS_UPDATED,
                    "breakRecommendationMinutes",
                    String.valueOf(before.breakRecommendationMinutes()),
                    String.valueOf(after.breakRecommendationMinutes()),
                    now));
        }
        if (before.riskMechanicEnabled() != after.riskMechanicEnabled()) {
            ControlsAction action = after.riskMechanicEnabled()
                    ? ControlsAction.RISK_MECHANIC_ENABLED
                    : ControlsAction.RISK_MECHANIC_DISABLED;
            auditRepo.append(new ControlsAuditEntry(
                    parentId, childId, action, "riskMechanicEnabled",
                    String.valueOf(before.riskMechanicEnabled()),
                    String.valueOf(after.riskMechanicEnabled()),
                    now));
        }
        if (after.noLimit()) {
            auditRepo.append(new ControlsAuditEntry(
                    parentId, childId, ControlsAction.NO_LIMIT_CONFIRMED,
                    "dailyLimitMinutes",
                    String.valueOf(before.dailyLimitMinutes()),
                    null,
                    now));
        }

        // BR-001 — enforce limit immediately: terminate active sessions if used.
        if (!after.noLimit()
                && playTimeLedger.minutesPlayedToday(childId, today()) >= after.dailyLimitMinutes()) {
            terminateActiveSessions(parentId, childId, now);
        }
        return after;
    }

    /** BR-001 — true unless today's used minutes already meet the configured limit. */
    public boolean canStartSession(UUID childId) {
        if (childId == null) throw new IllegalArgumentException("childId must not be null");
        ChildControls controls = controlsRepo.findByChildId(childId).orElse(null);
        if (controls == null || controls.noLimit()) return true;
        int used = playTimeLedger.minutesPlayedToday(childId, today());
        return used < controls.dailyLimitMinutes();
    }

    /** Records play time for the running session (called by the training
     * service when a session ends or every minute tick). */
    public void recordPlayMinutes(UUID childId, int minutes) {
        if (childId == null) throw new IllegalArgumentException("childId must not be null");
        if (minutes <= 0) return;
        playTimeLedger.addMinutes(childId, today(), minutes);
    }

    private void terminateActiveSessions(UUID parentId, UUID childId, Instant when) {
        childSessionRepo.findFirstByChildIdAndRevokedAtIsNullOrderByCreatedAtDesc(childId)
                .ifPresent(s -> {
                    s.revoke();
                    childSessionRepo.save(s);
                    auditRepo.append(new ControlsAuditEntry(
                            parentId, childId,
                            ControlsAction.SESSION_TERMINATED_BY_LIMIT,
                            "session", s.getId().toString(), null, when));
                });
    }

    private LocalDate today() {
        return LocalDate.ofInstant(Instant.now(clock), ZoneOffset.UTC);
    }
}
