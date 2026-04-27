package ch.numnia.parentcontrols.service;

import ch.numnia.parentcontrols.domain.ChildControls;
import ch.numnia.parentcontrols.domain.ControlsAction;
import ch.numnia.parentcontrols.domain.ControlsAuditEntry;
import ch.numnia.parentcontrols.domain.RoundPoolSnapshot;
import ch.numnia.parentcontrols.spi.ChildControlsRepository;
import ch.numnia.parentcontrols.spi.ControlsAuditRepository;
import ch.numnia.parentcontrols.spi.RoundPoolRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * UC-009 BR-003 — risk mechanic implementation.
 *
 * <p>While the risk mechanic is active a wrong answer moves a small number
 * of star points and items into a per-match round pool. At
 * {@link #endMatch(UUID)} the pool is restored to the child, ensuring no
 * permanent loss (FR-GAM-005, FR-GAM-006).
 *
 * <p>If the mechanic is disabled, both methods are no-ops returning the
 * unchanged balance.
 */
@Service
public class RiskMechanicService {

    private final ChildControlsRepository controlsRepo;
    private final RoundPoolRepository roundPoolRepo;
    private final ControlsAuditRepository auditRepo;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public RiskMechanicService(ChildControlsRepository controlsRepo,
                               RoundPoolRepository roundPoolRepo,
                               ControlsAuditRepository auditRepo) {
        this(controlsRepo, roundPoolRepo, auditRepo, Clock.systemUTC());
    }

    public RiskMechanicService(ChildControlsRepository controlsRepo,
                               RoundPoolRepository roundPoolRepo,
                               ControlsAuditRepository auditRepo,
                               Clock clock) {
        this.controlsRepo = controlsRepo;
        this.roundPoolRepo = roundPoolRepo;
        this.auditRepo = auditRepo;
        this.clock = clock;
    }

    /**
     * Records that the child answered wrong inside a match. While the risk
     * mechanic is enabled, {@code starsAtRisk} is moved into the round pool;
     * otherwise the call is a no-op.
     *
     * @return the updated round pool snapshot
     */
    public RoundPoolSnapshot recordWrongAnswer(UUID childId, int starsAtRisk, int itemsAtRisk) {
        if (childId == null) throw new IllegalArgumentException("childId must not be null");
        ChildControls controls = controlsRepo.findByChildId(childId).orElse(null);
        if (controls == null || !controls.riskMechanicEnabled()) {
            return roundPoolRepo.get(childId);
        }
        RoundPoolSnapshot current = roundPoolRepo.get(childId);
        RoundPoolSnapshot updated = new RoundPoolSnapshot(
                current.starPointsInPool() + Math.max(0, starsAtRisk),
                current.itemsInPool() + Math.max(0, itemsAtRisk));
        roundPoolRepo.put(childId, updated);
        return updated;
    }

    /**
     * Closes a match and restores the round pool back to the child
     * (BR-003). Returns the snapshot that was restored, then clears it.
     */
    public RoundPoolSnapshot endMatch(UUID childId) {
        if (childId == null) throw new IllegalArgumentException("childId must not be null");
        RoundPoolSnapshot pool = roundPoolRepo.get(childId);
        if (pool.starPointsInPool() == 0 && pool.itemsInPool() == 0) {
            roundPoolRepo.clear(childId);
            return RoundPoolSnapshot.EMPTY;
        }
        ChildControls controls = controlsRepo.findByChildId(childId).orElse(null);
        UUID parentId = controls != null ? controls.parentId() : childId;
        auditRepo.append(new ControlsAuditEntry(
                parentId, childId,
                ControlsAction.RISK_OUTCOME_RESTORED,
                "roundPool",
                String.valueOf(pool.starPointsInPool() + pool.itemsInPool()),
                "0",
                Instant.now(clock)));
        roundPoolRepo.clear(childId);
        return pool;
    }
}
