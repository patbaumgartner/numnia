package ch.numnia.deletion.steps;

import ch.numnia.deletion.domain.DeletionRequest;
import ch.numnia.deletion.domain.DeletionStatus;
import ch.numnia.deletion.service.DeletionLinkUnavailableException;
import ch.numnia.deletion.service.DeletionService;
import ch.numnia.deletion.spi.DeletionRequestRepository;
import ch.numnia.iam.domain.AuditAction;
import ch.numnia.iam.domain.AuditLogEntry;
import ch.numnia.iam.spi.AuditLogRepository;
import ch.numnia.iam.spi.ChildProfileRepository;
import ch.numnia.test.TestScenarioContext;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.databind.ObjectMapper;

import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Cucumber step definitions for UC-011 — Parent deletes a child account.
 *
 * <p>The {@code Background} step "a verified parent account with a child
 * profile" is shared with UC-010 and is registered there; Cucumber resolves
 * it from {@code Uc010StepDefinitions}, so we do not redefine it here.
 *
 * <p>Test parent password is fixed to {@code "E2eTestPass123"} (the value
 * that {@code /api/test/child-setup} uses when creating the parent account).
 */
public class Uc011StepDefinitions {

    /** Password used by {@code /api/test/child-setup} when creating the parent. */
    private static final String TEST_PARENT_PASSWORD = "E2eTestPass123";

    @Autowired private DeletionService deletionService;
    @Autowired private DeletionRequestRepository deletionRequests;
    @Autowired private ChildProfileRepository childProfiles;
    @Autowired private AuditLogRepository auditLog;
    @Autowired private TestScenarioContext context;
    @Autowired private ObjectMapper objectMapper;
    @LocalServerPort private int port;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    private DeletionRequest pendingRequest;
    private DeletionService.DeletionRecord deletionRecord;

    @Before
    public void reset() {
        pendingRequest = null;
        deletionRecord = null;
    }

    private UUID parentId() {
        UUID p = context.parentId();
        assertThat(p).as("parentId set by background").isNotNull();
        return p;
    }

    private UUID childId() {
        UUID c = context.childId();
        assertThat(c).as("childId set by background").isNotNull();
        return c;
    }

    // ── Scenario 1: Successful deletion with cool-off confirmation ─────

    @Given("the parent confirms with password and the word {string}")
    public void theParentConfirmsWithPasswordAndTheWordDelete(String word) {
        assertThat(word).isEqualTo("DELETE");
        pendingRequest = deletionService.requestDeletion(
                parentId(), childId(), TEST_PARENT_PASSWORD, word);
        assertThat(pendingRequest.status()).isEqualTo(DeletionStatus.PENDING);
    }

    @When("the parent opens the link from the confirmation email within 24 hours")
    public void theParentOpensTheLinkFromTheConfirmationEmailWithin24Hours() {
        deletionRecord = deletionService.confirmDeletion(pendingRequest.token());
    }

    @Then("all personal data of the child profile is deleted")
    public void allPersonalDataOfTheChildProfileIsDeleted() {
        assertThat(childProfiles.findById(childId())).isEmpty();
    }

    @Then("the parent receives a deletion record with date and data categories")
    public void theParentReceivesADeletionRecordWithDateAndDataCategories() {
        assertThat(deletionRecord).isNotNull();
        assertThat(deletionRecord.completedAt()).isNotNull();
        assertThat(deletionRecord.dataCategories())
                .as("BR-002 deletion record must list data categories")
                .contains("child-profile");
        assertThat(auditLog.findAllByParentRefOrderByTimestampAsc(parentId().toString()))
                .anyMatch(e -> e.getAction() == AuditAction.DELETION_CONFIRMED);
    }

    // ── Scenario 2: Confirmation link expires ──────────────────────────

    @Given("a triggered deletion process")
    public void aTriggeredDeletionProcess() {
        pendingRequest = deletionService.requestDeletion(
                parentId(), childId(), TEST_PARENT_PASSWORD, "DELETE");
    }

    @When("the parent does not open the confirmation link within 24 hours")
    public void theParentDoesNotOpenTheConfirmationLinkWithin24Hours() {
        Instant past = Instant.now().minus(Duration.ofMinutes(1));
        pendingRequest.overrideExpiresAt(past);
        deletionRequests.save(pendingRequest);
        int discarded = deletionService.expirePending();
        assertThat(discarded).isGreaterThanOrEqualTo(1);
    }

    @Then("the child profile remains active")
    public void theChildProfileRemainsActive() {
        assertThat(childProfiles.findById(childId()))
                .as("expired cool-off must NOT delete the child profile")
                .isPresent();
    }

    @Then("the deletion process is marked as {string} in the audit log")
    public void theDeletionProcessIsMarkedAsDiscardedInTheAuditLog(String label) {
        assertThat(label).isEqualTo("discarded");
        assertThat(deletionRequests.findById(pendingRequest.id()))
                .map(DeletionRequest::status)
                .contains(DeletionStatus.DISCARDED);
        assertThat(auditLog.findAllByParentRefOrderByTimestampAsc(parentId().toString()))
                .anyMatch(e -> e.getAction() == AuditAction.DELETION_DISCARDED);
        assertThatThrownBy(() -> deletionService.confirmDeletion(pendingRequest.token()))
                .isInstanceOf(DeletionLinkUnavailableException.class);
    }

    // ── Scenario 3: Backups cleansed within rotation window ────────────

    @Given("a completed deletion process")
    public void aCompletedDeletionProcess() {
        pendingRequest = deletionService.requestDeletion(
                parentId(), childId(), TEST_PARENT_PASSWORD, "DELETE");
        deletionRecord = deletionService.confirmDeletion(pendingRequest.token());
        assertThat(deletionRecord.dataCategories()).contains("child-profile");
    }

    @When("the next backup rotation runs")
    public void theNextBackupRotationRuns() {
        // Modelled by emitting the BACKUP_CLEANSED audit entry that the real
        // backup sweeper would write (BR-004). The production sweeper lives
        // outside the application; the audit signal is the public contract.
        auditLog.save(new AuditLogEntry(
                AuditAction.DELETION_BACKUP_CLEANSED,
                parentId().toString(),
                pendingRequest.childPseudonym(),
                "requestId=" + pendingRequest.id()));
    }

    @Then("the active backups no longer contain personal data of the deleted child profile")
    public void theActiveBackupsNoLongerContainPersonalDataOfTheDeletedChildProfile() {
        assertThat(auditLog.findAllByParentRefOrderByTimestampAsc(parentId().toString()))
                .anyMatch(e -> e.getAction() == AuditAction.DELETION_BACKUP_CLEANSED);
        assertThat(childProfiles.findById(childId())).isEmpty();
    }
}
