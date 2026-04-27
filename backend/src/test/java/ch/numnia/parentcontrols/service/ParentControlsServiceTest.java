package ch.numnia.parentcontrols.service;

import ch.numnia.iam.domain.ChildSession;
import ch.numnia.iam.spi.ChildSessionRepository;
import ch.numnia.parentcontrols.domain.ChildControls;
import ch.numnia.parentcontrols.domain.ControlsAction;
import ch.numnia.parentcontrols.domain.ControlsAuditEntry;
import ch.numnia.parentcontrols.infra.InMemoryChildControlsRepository;
import ch.numnia.parentcontrols.infra.InMemoryControlsAuditRepository;
import ch.numnia.parentcontrols.infra.InMemoryPlayTimeLedger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ParentControlsServiceTest {

    private InMemoryChildControlsRepository controlsRepo;
    private InMemoryControlsAuditRepository auditRepo;
    private InMemoryPlayTimeLedger ledger;
    private ChildSessionRepository sessionRepo;
    private Clock clock;
    private ParentControlsService service;

    private final UUID parentId = UUID.fromString("00000000-0000-0000-0000-00000000aaa1");
    private final UUID childId = UUID.fromString("00000000-0000-0000-0000-00000000bbb1");

    @BeforeEach
    void setUp() {
        controlsRepo = new InMemoryChildControlsRepository();
        auditRepo = new InMemoryControlsAuditRepository();
        ledger = new InMemoryPlayTimeLedger();
        sessionRepo = mock(ChildSessionRepository.class);
        when(sessionRepo.findFirstByChildIdAndRevokedAtIsNullOrderByCreatedAtDesc(any()))
                .thenReturn(Optional.empty());
        clock = Clock.fixed(Instant.parse("2026-04-28T10:00:00Z"), ZoneOffset.UTC);
        service = new ParentControlsService(controlsRepo, auditRepo, ledger, sessionRepo, clock);
    }

    @Test
    void getOrDefault_withNoExistingControls_returnsDefaults_brDefaults() {
        ChildControls c = service.getOrDefault(parentId, childId);
        assertThat(c.dailyLimitMinutes()).isEqualTo(30);
        assertThat(c.breakRecommendationMinutes()).isEqualTo(15);
        assertThat(c.riskMechanicEnabled()).isFalse();
    }

    @Test
    void getOrDefault_returnsPersistedControls() {
        controlsRepo.save(new ChildControls(childId, parentId, 45, 20, true));
        ChildControls c = service.getOrDefault(parentId, childId);
        assertThat(c.dailyLimitMinutes()).isEqualTo(45);
        assertThat(c.riskMechanicEnabled()).isTrue();
    }

    @Test
    void getOrDefault_withForeignParent_throwsUnauthorized() {
        controlsRepo.save(new ChildControls(childId, parentId, 30, 15, false));
        UUID otherParent = UUID.randomUUID();
        assertThatThrownBy(() -> service.getOrDefault(otherParent, childId))
                .isInstanceOf(UnauthorizedControlsAccessException.class);
    }

    @Test
    void updateControls_changesValuesAndAuditsBeforeAfter_brAuditable() {
        controlsRepo.save(new ChildControls(childId, parentId, 30, 15, false));

        service.updateControls(parentId, childId, 45, 15, false, false);

        assertThat(controlsRepo.findByChildId(childId)).hasValueSatisfying(c ->
                assertThat(c.dailyLimitMinutes()).isEqualTo(45));
        List<ControlsAuditEntry> audit = auditRepo.findByChildId(childId);
        assertThat(audit).anySatisfy(e -> {
            assertThat(e.action()).isEqualTo(ControlsAction.CONTROLS_UPDATED);
            assertThat(e.field()).isEqualTo("dailyLimitMinutes");
            assertThat(e.beforeValue()).isEqualTo("30");
            assertThat(e.afterValue()).isEqualTo("45");
            assertThat(e.timestamp()).isNotNull();
        });
    }

    @Test
    void updateControls_riskMechanicTurnedOn_emitsRiskEnabledAudit() {
        service.updateControls(parentId, childId, 30, 15, true, false);
        assertThat(auditRepo.findByChildId(childId))
                .anyMatch(e -> e.action() == ControlsAction.RISK_MECHANIC_ENABLED);
    }

    @Test
    void updateControls_riskMechanicTurnedOff_emitsRiskDisabledAudit() {
        controlsRepo.save(new ChildControls(childId, parentId, 30, 15, true));
        service.updateControls(parentId, childId, 30, 15, false, false);
        assertThat(auditRepo.findByChildId(childId))
                .anyMatch(e -> e.action() == ControlsAction.RISK_MECHANIC_DISABLED);
    }

    @Test
    void updateControls_noLimitWithoutConfirmation_isRejected_alt3a() {
        assertThatThrownBy(() ->
                service.updateControls(parentId, childId, null, 15, false, false))
                .isInstanceOf(NoLimitConfirmationRequiredException.class);
        assertThat(controlsRepo.findByChildId(childId)).isEmpty();
    }

    @Test
    void updateControls_noLimitWithConfirmation_isPersistedAndAudited_alt3a() {
        service.updateControls(parentId, childId, null, 15, false, true);
        assertThat(controlsRepo.findByChildId(childId)).hasValueSatisfying(c ->
                assertThat(c.noLimit()).isTrue());
        assertThat(auditRepo.findByChildId(childId))
                .anyMatch(e -> e.action() == ControlsAction.NO_LIMIT_CONFIRMED);
    }

    @Test
    void updateControls_lowerLimitBelowUsedToday_terminatesActiveSession_br001() {
        // 25 minutes already played
        ledger.addMinutes(childId, LocalDate.parse("2026-04-28"), 25);
        ChildSession active = new ChildSession(childId, parentId);
        when(sessionRepo.findFirstByChildIdAndRevokedAtIsNullOrderByCreatedAtDesc(childId))
                .thenReturn(Optional.of(active));

        // Parent lowers to 20 → already over limit
        service.updateControls(parentId, childId, 20, 15, false, false);

        assertThat(active.getRevokedAt()).isNotNull();
        verify(sessionRepo).save(active);
        assertThat(auditRepo.findByChildId(childId))
                .anyMatch(e -> e.action() == ControlsAction.SESSION_TERMINATED_BY_LIMIT);
        assertThat(service.canStartSession(childId)).isFalse();
    }

    @Test
    void canStartSession_belowLimit_isTrue() {
        controlsRepo.save(new ChildControls(childId, parentId, 30, 15, false));
        ledger.addMinutes(childId, LocalDate.parse("2026-04-28"), 10);
        assertThat(service.canStartSession(childId)).isTrue();
    }

    @Test
    void canStartSession_atOrAboveLimit_isFalse_br001() {
        controlsRepo.save(new ChildControls(childId, parentId, 30, 15, false));
        ledger.addMinutes(childId, LocalDate.parse("2026-04-28"), 30);
        assertThat(service.canStartSession(childId)).isFalse();
    }

    @Test
    void canStartSession_withNoLimit_isAlwaysTrue() {
        controlsRepo.save(new ChildControls(childId, parentId, null, 15, false));
        ledger.addMinutes(childId, LocalDate.parse("2026-04-28"), 999);
        assertThat(service.canStartSession(childId)).isTrue();
    }

    @Test
    void getOrDefault_withNullArgs_throws() {
        assertThatThrownBy(() -> service.getOrDefault(null, childId))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.getOrDefault(parentId, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void recordPlayMinutes_increasesUsedMinutes() {
        service.recordPlayMinutes(childId, 7);
        service.recordPlayMinutes(childId, 3);
        assertThat(ledger.minutesPlayedToday(childId, LocalDate.parse("2026-04-28"))).isEqualTo(10);
    }

    @Test
    void defaultControls_riskMechanicIsDisabled_br002() {
        ChildControls c = ChildControls.defaults(childId, parentId);
        assertThat(c.riskMechanicEnabled()).isFalse();
    }
}
