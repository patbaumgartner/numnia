package ch.numnia.dataexport.service;

import ch.numnia.avatar.domain.InventoryEntry;
import ch.numnia.avatar.infra.InMemoryInventoryRepository;
import ch.numnia.creatures.domain.CreatureUnlock;
import ch.numnia.creatures.infra.InMemoryCreatureInventoryRepository;
import ch.numnia.dataexport.domain.ExportFile;
import ch.numnia.dataexport.domain.ExportFormat;
import ch.numnia.dataexport.domain.ExportStatus;
import ch.numnia.dataexport.infra.InMemoryExportFileRepository;
import ch.numnia.iam.domain.AuditAction;
import ch.numnia.iam.domain.AuditLogEntry;
import ch.numnia.iam.domain.ChildProfile;
import ch.numnia.iam.spi.AuditLogRepository;
import ch.numnia.iam.spi.ChildProfileRepository;
import ch.numnia.learning.domain.LearningProgress;
import ch.numnia.learning.domain.MasteryStatus;
import ch.numnia.learning.domain.Operation;
import ch.numnia.learning.domain.TrainingSession;
import ch.numnia.learning.infra.InMemoryLearningProgressRepository;
import ch.numnia.learning.infra.InMemoryStarPointsRepository;
import ch.numnia.learning.infra.InMemoryTrainingSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Unit tests for {@link ExportService} (UC-010). */
class ExportServiceTest {

    private final UUID parentId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private final UUID otherParentId = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private final UUID childId = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private final Instant t0 = Instant.parse("2026-05-01T08:00:00Z");

    private ChildProfileRepository profiles;
    private InMemoryLearningProgressRepository learning;
    private InMemoryTrainingSessionRepository sessions;
    private InMemoryStarPointsRepository starPoints;
    private InMemoryInventoryRepository inventory;
    private InMemoryCreatureInventoryRepository creatureInventory;
    private AuditLogRepository auditLog;
    private InMemoryExportFileRepository exportFiles;
    private ChildProfile child;
    private MutableClock clock;
    private ExportService service;

    @BeforeEach
    void setUp() {
        profiles = mock(ChildProfileRepository.class);
        auditLog = mock(AuditLogRepository.class);
        when(auditLog.save(any(AuditLogEntry.class))).thenAnswer(inv -> inv.getArgument(0));
        learning = new InMemoryLearningProgressRepository();
        sessions = new InMemoryTrainingSessionRepository();
        starPoints = new InMemoryStarPointsRepository();
        inventory = new InMemoryInventoryRepository();
        creatureInventory = new InMemoryCreatureInventoryRepository();
        exportFiles = new InMemoryExportFileRepository();
        clock = new MutableClock(t0);
        child = new ChildProfile(childId, "Astra", 2017, "fox", parentId);
        when(profiles.findById(childId)).thenReturn(Optional.of(child));
        when(profiles.findById(any(UUID.class))).thenAnswer(inv ->
                inv.getArgument(0).equals(childId) ? Optional.of(child) : Optional.empty());
        service = new ExportService(profiles, learning, sessions, starPoints, inventory,
                creatureInventory, auditLog, exportFiles, clock);
    }

    @Test
    void requestExport_jsonIncludesAllRequiredSections_brCompleteness() throws Exception {
        seedSampleData();

        List<ExportFile> files = service.requestExport(parentId, childId, ExportFormat.JSON);

        assertThat(files).hasSize(1);
        ExportFile file = files.get(0);
        assertThat(file.format()).isEqualTo(ExportFormat.JSON);
        assertThat(file.status()).isEqualTo(ExportStatus.AVAILABLE);
        assertThat(file.token()).isNotBlank();
        assertThat(file.expiresAt()).isEqualTo(t0.plus(Duration.ofDays(7)));

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(file.content());
        assertThat(root.has("profile")).isTrue();
        assertThat(root.has("learningHistory")).isTrue();
        assertThat(root.has("masteryStatus")).isTrue();
        assertThat(root.has("inventory")).isTrue();
        assertThat(root.has("starPointMovements")).isTrue();
        assertThat(root.has("consentHistory")).isTrue();
        assertThat(root.has("auditLogSummary")).isTrue();
        assertThat(root.path("profile").path("pseudonym").asString()).isEqualTo("Astra");
        assertThat(root.path("learningHistory").size()).isPositive();
        assertThat(root.path("masteryStatus").path("ADDITION").asString())
                .isEqualTo("MASTERED");
        assertThat(root.path("inventory").path("creatures").size()).isPositive();
        assertThat(root.path("starPointMovements").path("currentBalance").asInt())
                .isEqualTo(75);
    }

    @Test
    void requestExport_writesExportTriggeredAuditEntry_br003() {
        List<ExportFile> files = service.requestExport(parentId, childId, ExportFormat.JSON);
        assertThat(files).hasSize(1);

        ArgumentCaptor<AuditLogEntry> captor = ArgumentCaptor.forClass(AuditLogEntry.class);
        verify(auditLog, atLeastOnce()).save(captor.capture());
        assertThat(captor.getAllValues())
                .anyMatch(e -> e.getAction() == AuditAction.EXPORT_TRIGGERED
                        && e.getParentRef().equals(parentId.toString())
                        && "Astra".equals(e.getChildRef()));
    }

    @Test
    void requestExport_byForeignParent_isRejected_nfrSec003() {
        assertThatThrownBy(() -> service.requestExport(otherParentId, childId, ExportFormat.JSON))
                .isInstanceOf(UnauthorizedExportAccessException.class);
        verify(auditLog, never()).save(argThat(e ->
                e != null && e.getAction() == AuditAction.EXPORT_TRIGGERED));
    }

    @Test
    void requestExport_pdfFormat_producesValidPdfHeaderAndEofMarker() {
        List<ExportFile> files = service.requestExport(parentId, childId, ExportFormat.PDF);

        assertThat(files).hasSize(1);
        byte[] bytes = files.get(0).content();
        String head = new String(bytes, 0, Math.min(8, bytes.length));
        assertThat(head).startsWith("%PDF-");
        assertThat(new String(bytes)).endsWith("%%EOF\n");
    }

    @Test
    void requestExport_bothFormat_producesTwoFiles_oneJsonOnePdf() {
        List<ExportFile> files = service.requestExport(parentId, childId, ExportFormat.BOTH);

        assertThat(files).hasSize(2);
        assertThat(files).extracting(ExportFile::format)
                .containsExactlyInAnyOrder(ExportFormat.JSON, ExportFormat.PDF);
    }

    @Test
    void download_firstTime_writesExportDownloadedAuditEntry() {
        ExportFile file = service.requestExport(parentId, childId, ExportFormat.JSON).get(0);
        clock.advance(Duration.ofMinutes(5));

        ExportFile downloaded = service.download(file.token());

        assertThat(downloaded.downloadedAt()).isEqualTo(clock.instant());
        assertThat(downloaded.status()).isEqualTo(ExportStatus.DOWNLOADED);

        ArgumentCaptor<AuditLogEntry> captor = ArgumentCaptor.forClass(AuditLogEntry.class);
        verify(auditLog, atLeastOnce()).save(captor.capture());
        assertThat(captor.getAllValues())
                .filteredOn(e -> e.getAction() == AuditAction.EXPORT_DOWNLOADED)
                .hasSize(1);
    }

    @Test
    void download_secondTime_doesNotDoubleAudit() {
        ExportFile file = service.requestExport(parentId, childId, ExportFormat.JSON).get(0);
        service.download(file.token());
        clearInvocations(auditLog);

        service.download(file.token());

        verify(auditLog, never()).save(argThat(e ->
                e != null && e.getAction() == AuditAction.EXPORT_DOWNLOADED));
    }

    @Test
    void download_afterDeadline_isRejected_brExpired() {
        ExportFile file = service.requestExport(parentId, childId, ExportFormat.JSON).get(0);
        clock.advance(Duration.ofDays(8));

        assertThatThrownBy(() -> service.download(file.token()))
                .isInstanceOf(ExportLinkUnavailableException.class);
    }

    @Test
    void download_unknownToken_isRejected() {
        assertThatThrownBy(() -> service.download("does-not-exist"))
                .isInstanceOf(ExportLinkUnavailableException.class);
    }

    @Test
    void purgeExpired_removesUnDownloadedExpiredFilesAndAudits_br004() {
        ExportFile file = service.requestExport(parentId, childId, ExportFormat.JSON).get(0);
        clock.advance(Duration.ofDays(8));
        clearInvocations(auditLog);

        int purged = service.purgeExpired();

        assertThat(purged).isEqualTo(1);
        assertThat(exportFiles.findByToken(file.token())).isEmpty();
        ArgumentCaptor<AuditLogEntry> captor = ArgumentCaptor.forClass(AuditLogEntry.class);
        verify(auditLog).save(captor.capture());
        assertThat(captor.getValue().getAction()).isEqualTo(AuditAction.EXPORT_FILE_EXPIRED);
        assertThat(captor.getValue().getParentRef()).isEqualTo(parentId.toString());
    }

    @Test
    void purgeExpired_doesNotDeleteDownloadedFiles() {
        ExportFile file = service.requestExport(parentId, childId, ExportFormat.JSON).get(0);
        service.download(file.token());
        clock.advance(Duration.ofDays(8));

        int purged = service.purgeExpired();

        assertThat(purged).isZero();
    }

    @Test
    void purgeExpired_skipsFilesNotYetExpired() {
        ExportFile file = service.requestExport(parentId, childId, ExportFormat.JSON).get(0);
        clock.advance(Duration.ofDays(3));

        int purged = service.purgeExpired();

        assertThat(purged).isZero();
        assertThat(exportFiles.findByToken(file.token())).isPresent();
    }

    @Test
    void listExports_returnsOnlyOwningParents() {
        service.requestExport(parentId, childId, ExportFormat.JSON);

        assertThat(service.listExports(parentId)).hasSize(1);
        assertThat(service.listExports(otherParentId)).isEmpty();
    }

    @Test
    void requestExport_nullArguments_areRejected() {
        assertThatThrownBy(() -> service.requestExport(null, childId, ExportFormat.JSON))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> service.requestExport(parentId, null, ExportFormat.JSON))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> service.requestExport(parentId, childId, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void download_nullToken_isRejected() {
        assertThatThrownBy(() -> service.download(null))
                .isInstanceOf(NullPointerException.class);
    }

    // ── helpers ─────────────────────────────────────────────────────────

    private void seedSampleData() {
        LearningProgress lp = new LearningProgress(childId, Operation.ADDITION, 1, 1);
        lp.setMasteryStatus(MasteryStatus.MASTERED);
        learning.save(lp);

        TrainingSession session = new TrainingSession(UUID.randomUUID(), childId,
                Operation.ADDITION, 1, 1, t0.minusSeconds(3600));
        session.end(t0.minusSeconds(2400));
        sessions.save(session);

        starPoints.setBalance(childId, 75);
        inventory.recordPurchase(new InventoryEntry(childId, "star-cap", t0.minusSeconds(7200)));
        creatureInventory.recordUnlock(new CreatureUnlock(childId, "pilzar", t0.minusSeconds(5400)));

        AuditLogEntry consent = new AuditLogEntry(AuditAction.EMAIL_SECONDARY_CONFIRMED,
                parentId.toString(), null, "double opt-in confirmed");
        when(auditLog.findAllByParentRefOrderByTimestampAsc(parentId.toString()))
                .thenReturn(List.of(consent));
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
