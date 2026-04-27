package ch.numnia.deletion.service;

import ch.numnia.deletion.domain.DeletionRequest;
import ch.numnia.deletion.domain.DeletionStatus;
import ch.numnia.deletion.infra.InMemoryDeletionRequestRepository;
import ch.numnia.deletion.spi.ChildDataPurger;
import ch.numnia.iam.domain.AuditAction;
import ch.numnia.iam.domain.AuditLogEntry;
import ch.numnia.iam.domain.ChildProfile;
import ch.numnia.iam.domain.ParentAccount;
import ch.numnia.iam.spi.AuditLogRepository;
import ch.numnia.iam.spi.ChildProfileRepository;
import ch.numnia.iam.spi.EmailGateway;
import ch.numnia.iam.spi.ParentAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** Unit tests for {@link DeletionService} (UC-011). */
class DeletionServiceTest {

    private final UUID parentId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private final UUID otherParentId = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private final UUID childId = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private final Instant t0 = Instant.parse("2026-06-01T08:00:00Z");
    private final String parentEmail = "parent@example.com";
    private final String correctPassword = "S3cretPass!";

    private ChildProfileRepository profiles;
    private ParentAccountRepository parents;
    private InMemoryDeletionRequestRepository deletionRequests;
    private AuditLogRepository auditLog;
    private EmailGateway emailGateway;
    private PasswordEncoder passwordEncoder;
    private RecordingPurger recordingPurger;
    private ChildProfile child;
    private ParentAccount parent;
    private MutableClock clock;
    private DeletionService service;

    @BeforeEach
    void setUp() {
        profiles = mock(ChildProfileRepository.class);
        parents = mock(ParentAccountRepository.class);
        auditLog = mock(AuditLogRepository.class);
        when(auditLog.save(any(AuditLogEntry.class))).thenAnswer(inv -> inv.getArgument(0));
        emailGateway = mock(EmailGateway.class);
        passwordEncoder = new BCryptPasswordEncoder(4);
        deletionRequests = new InMemoryDeletionRequestRepository();
        recordingPurger = new RecordingPurger(Set.of("learning-progress", "training-sessions"));
        clock = new MutableClock(t0);

        child = new ChildProfile(childId, "Astra", 2017, "fox", parentId);
        when(profiles.findById(childId)).thenReturn(Optional.of(child));
        when(profiles.findById(any(UUID.class))).thenAnswer(inv ->
                inv.getArgument(0).equals(childId) ? Optional.of(child) : Optional.empty());

        String hashedPassword = passwordEncoder.encode(correctPassword);
        parent = new ParentAccount(parentId, parentEmail, hashedPassword,
                "Erika", "Frau", true, true);
        when(parents.findById(parentId)).thenReturn(Optional.of(parent));

        service = new DeletionService(profiles, parents, deletionRequests,
                List.of(recordingPurger), auditLog, emailGateway, passwordEncoder, clock);
    }

    // ── requestDeletion ────────────────────────────────────────────────

    @Test
    void requestDeletion_requiresMatchingPassword_br001() {
        assertThatThrownBy(() -> service.requestDeletion(
                parentId, childId, "WRONG", "DELETE"))
                .isInstanceOf(InvalidPasswordException.class);
        verify(emailGateway, never()).sendDeletionConfirmationEmail(
                anyString(), anyString(), anyString());
    }

    @Test
    void requestDeletion_requiresExplicitDeleteWord_br001() {
        assertThatThrownBy(() -> service.requestDeletion(
                parentId, childId, correctPassword, "delete"))
                .isInstanceOf(InvalidConfirmationWordException.class);
        verify(emailGateway, never()).sendDeletionConfirmationEmail(
                anyString(), anyString(), anyString());
    }

    @Test
    void requestDeletion_byForeignParent_isRejected_nfrSec003() {
        assertThatThrownBy(() -> service.requestDeletion(
                otherParentId, childId, correctPassword, "DELETE"))
                .isInstanceOf(UnauthorizedDeletionAccessException.class);
    }

    @Test
    void requestDeletion_createsRequestWith24hCoolOff_br001() {
        DeletionRequest request = service.requestDeletion(
                parentId, childId, correctPassword, "DELETE");

        assertThat(request.status()).isEqualTo(DeletionStatus.PENDING);
        assertThat(request.expiresAt()).isEqualTo(t0.plus(Duration.ofHours(24)));
        assertThat(request.token()).hasSizeGreaterThanOrEqualTo(32);
        assertThat(request.childPseudonym()).isEqualTo("Astra");
        assertThat(deletionRequests.findByToken(request.token())).isPresent();
    }

    @Test
    void requestDeletion_writesDeletionRequestedAuditEntry() {
        DeletionRequest request = service.requestDeletion(
                parentId, childId, correctPassword, "DELETE");

        ArgumentCaptor<AuditLogEntry> captor = ArgumentCaptor.forClass(AuditLogEntry.class);
        verify(auditLog, atLeastOnce()).save(captor.capture());
        assertThat(captor.getAllValues())
                .anyMatch(e -> e.getAction() == AuditAction.DELETION_REQUESTED
                        && parentId.toString().equals(e.getParentRef())
                        && "Astra".equals(e.getChildRef())
                        && e.getDetails().contains(request.id().toString()));
    }

    @Test
    void requestDeletion_sendsConfirmationEmail() {
        DeletionRequest request = service.requestDeletion(
                parentId, childId, correctPassword, "DELETE");

        verify(emailGateway).sendDeletionConfirmationEmail(
                eq(parentEmail), eq("Astra"), eq(request.token()));
    }

    // ── confirmDeletion ────────────────────────────────────────────────

    @Test
    void confirmDeletion_purgesAndReturnsTouchedCategories_br002() {
        DeletionRequest request = service.requestDeletion(
                parentId, childId, correctPassword, "DELETE");

        DeletionService.DeletionRecord record = service.confirmDeletion(request.token());

        assertThat(recordingPurger.purgeCount.get()).isEqualTo(1);
        assertThat(record.dataCategories())
                .contains("learning-progress", "training-sessions", "child-profile");
        verify(profiles).deleteById(childId);
        assertThat(deletionRequests.findById(request.id()))
                .map(DeletionRequest::status)
                .contains(DeletionStatus.COMPLETED);
    }

    @Test
    void confirmDeletion_sendsDeletionRecordEmail() {
        DeletionRequest request = service.requestDeletion(
                parentId, childId, correctPassword, "DELETE");

        service.confirmDeletion(request.token());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Set<String>> categoriesCaptor =
                ArgumentCaptor.forClass(Set.class);
        verify(emailGateway).sendDeletionRecordEmail(
                eq(parentEmail), eq("Astra"), categoriesCaptor.capture(), eq(t0));
        assertThat(categoriesCaptor.getValue())
                .contains("learning-progress", "training-sessions", "child-profile");
    }

    @Test
    void confirmDeletion_writesDeletionConfirmedAuditWithCategories_br002() {
        DeletionRequest request = service.requestDeletion(
                parentId, childId, correctPassword, "DELETE");

        service.confirmDeletion(request.token());

        ArgumentCaptor<AuditLogEntry> captor = ArgumentCaptor.forClass(AuditLogEntry.class);
        verify(auditLog, atLeastOnce()).save(captor.capture());
        AuditLogEntry confirmed = captor.getAllValues().stream()
                .filter(e -> e.getAction() == AuditAction.DELETION_CONFIRMED)
                .findFirst()
                .orElseThrow();
        assertThat(confirmed.getParentRef()).isEqualTo(parentId.toString());
        assertThat(confirmed.getChildRef()).isEqualTo("Astra");
        assertThat(confirmed.getDetails()).contains("categories=");
        assertThat(confirmed.getDetails()).contains("learning-progress");
        assertThat(confirmed.getDetails()).contains("training-sessions");
        assertThat(confirmed.getDetails()).contains("child-profile");
    }

    @Test
    void confirmDeletion_ofExpiredLink_throwsAndAuditsDiscarded() {
        DeletionRequest request = service.requestDeletion(
                parentId, childId, correctPassword, "DELETE");
        clock.advance(Duration.ofHours(25));

        assertThatThrownBy(() -> service.confirmDeletion(request.token()))
                .isInstanceOf(DeletionLinkUnavailableException.class);

        assertThat(deletionRequests.findById(request.id()))
                .map(DeletionRequest::status)
                .contains(DeletionStatus.DISCARDED);
        verify(auditLog, atLeastOnce()).save(argThatAction(AuditAction.DELETION_DISCARDED));
        verify(profiles, never()).deleteById(childId);
    }

    @Test
    void confirmDeletion_doubleConfirm_isRejected() {
        DeletionRequest request = service.requestDeletion(
                parentId, childId, correctPassword, "DELETE");
        service.confirmDeletion(request.token());

        assertThatThrownBy(() -> service.confirmDeletion(request.token()))
                .isInstanceOf(DeletionLinkUnavailableException.class);
    }

    @Test
    void confirmDeletion_unknownToken_isRejected() {
        assertThatThrownBy(() -> service.confirmDeletion("does-not-exist"))
                .isInstanceOf(DeletionLinkUnavailableException.class);
    }

    // ── expirePending ──────────────────────────────────────────────────

    @Test
    void expirePending_marksExpiredRequestsDiscardedAndAudits() {
        DeletionRequest request = service.requestDeletion(
                parentId, childId, correctPassword, "DELETE");
        clock.advance(Duration.ofHours(25));

        int discarded = service.expirePending();

        assertThat(discarded).isEqualTo(1);
        assertThat(deletionRequests.findById(request.id()))
                .map(DeletionRequest::status)
                .contains(DeletionStatus.DISCARDED);
        verify(auditLog, atLeastOnce()).save(argThatAction(AuditAction.DELETION_DISCARDED));
    }

    @Test
    void expirePending_leavesUnexpiredRequestsAlone() {
        service.requestDeletion(parentId, childId, correctPassword, "DELETE");
        clock.advance(Duration.ofHours(1));

        int discarded = service.expirePending();

        assertThat(discarded).isZero();
    }

    // ── helpers ────────────────────────────────────────────────────────

    private static AuditLogEntry argThatAction(AuditAction action) {
        return org.mockito.ArgumentMatchers.argThat(e ->
                e != null && e.getAction() == action);
    }

    /** Records each invocation; returns a fixed category set on success. */
    private static class RecordingPurger implements ChildDataPurger {
        final AtomicInteger purgeCount = new AtomicInteger();
        final List<UUID> seenChildIds = new ArrayList<>();
        final Set<String> categories;

        RecordingPurger(Set<String> categories) {
            this.categories = new HashSet<>(categories);
        }

        @Override
        public Set<String> purge(UUID childId) {
            purgeCount.incrementAndGet();
            seenChildIds.add(childId);
            return categories;
        }
    }

    /** Mutable test clock so we can advance time across multiple operations. */
    private static class MutableClock extends Clock {
        private Instant now;
        MutableClock(Instant initial) { this.now = initial; }
        void advance(Duration d) { this.now = this.now.plus(d); }
        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }
}
