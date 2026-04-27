package ch.numnia.parentcontrols.service;

import ch.numnia.parentcontrols.domain.ChildControls;
import ch.numnia.parentcontrols.domain.ControlsAction;
import ch.numnia.parentcontrols.domain.RoundPoolSnapshot;
import ch.numnia.parentcontrols.infra.InMemoryChildControlsRepository;
import ch.numnia.parentcontrols.infra.InMemoryControlsAuditRepository;
import ch.numnia.parentcontrols.infra.InMemoryRoundPoolRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RiskMechanicServiceTest {

    private InMemoryChildControlsRepository controlsRepo;
    private InMemoryRoundPoolRepository poolRepo;
    private InMemoryControlsAuditRepository auditRepo;
    private RiskMechanicService service;

    private final UUID parentId = UUID.fromString("00000000-0000-0000-0000-0000000000aa");
    private final UUID childId = UUID.fromString("00000000-0000-0000-0000-0000000000bb");

    @BeforeEach
    void setUp() {
        controlsRepo = new InMemoryChildControlsRepository();
        poolRepo = new InMemoryRoundPoolRepository();
        auditRepo = new InMemoryControlsAuditRepository();
        service = new RiskMechanicService(controlsRepo, poolRepo, auditRepo);
    }

    @Test
    void recordWrongAnswer_withRiskDisabled_keepsPoolEmpty_brNoLossWhenOff() {
        controlsRepo.save(new ChildControls(childId, parentId, 30, 15, false));
        RoundPoolSnapshot pool = service.recordWrongAnswer(childId, 5, 1);
        assertThat(pool).isEqualTo(RoundPoolSnapshot.EMPTY);
    }

    @Test
    void recordWrongAnswer_withRiskEnabled_movesIntoPool_notPermanent() {
        controlsRepo.save(new ChildControls(childId, parentId, 30, 15, true));
        RoundPoolSnapshot pool = service.recordWrongAnswer(childId, 5, 1);
        assertThat(pool.starPointsInPool()).isEqualTo(5);
        assertThat(pool.itemsInPool()).isEqualTo(1);
    }

    @Test
    void endMatch_restoresEverything_br003() {
        controlsRepo.save(new ChildControls(childId, parentId, 30, 15, true));
        service.recordWrongAnswer(childId, 5, 1);
        service.recordWrongAnswer(childId, 3, 0);

        RoundPoolSnapshot restored = service.endMatch(childId);

        assertThat(restored.starPointsInPool()).isEqualTo(8);
        assertThat(restored.itemsInPool()).isEqualTo(1);
        // pool is cleared after end
        assertThat(poolRepo.get(childId)).isEqualTo(RoundPoolSnapshot.EMPTY);
        assertThat(auditRepo.findByChildId(childId))
                .anyMatch(e -> e.action() == ControlsAction.RISK_OUTCOME_RESTORED);
    }

    @Test
    void endMatch_withEmptyPool_isNoOp() {
        controlsRepo.save(new ChildControls(childId, parentId, 30, 15, true));
        RoundPoolSnapshot result = service.endMatch(childId);
        assertThat(result).isEqualTo(RoundPoolSnapshot.EMPTY);
        assertThat(auditRepo.findByChildId(childId)).isEmpty();
    }

    @Test
    void recordWrongAnswer_nullChildId_throws() {
        assertThatThrownBy(() -> service.recordWrongAnswer(null, 1, 1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
