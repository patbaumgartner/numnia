package ch.numnia.parentcontrols.steps;

import ch.numnia.iam.domain.ChildSession;
import ch.numnia.iam.spi.ChildSessionRepository;
import ch.numnia.parentcontrols.domain.ChildControls;
import ch.numnia.parentcontrols.domain.ControlsAction;
import ch.numnia.parentcontrols.domain.ControlsAuditEntry;
import ch.numnia.parentcontrols.domain.RoundPoolSnapshot;
import ch.numnia.parentcontrols.service.ParentControlsService;
import ch.numnia.parentcontrols.service.RiskMechanicService;
import ch.numnia.parentcontrols.spi.ChildControlsRepository;
import ch.numnia.parentcontrols.spi.ControlsAuditRepository;
import ch.numnia.parentcontrols.spi.PlayTimeLedger;
import ch.numnia.test.TestScenarioContext;
import io.cucumber.java.Before;
import io.cucumber.java.en.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cucumber step definitions for UC-009 — Parent sets daily limit and risk
 * mechanic. The Background step "Given a verified parent account with at
 * least one child profile" reuses the {@link TestScenarioContext} populated
 * by {@code Uc002StepDefinitions} via {@code /api/test/child-setup}.
 */
public class Uc009StepDefinitions {

    @Autowired private ParentControlsService controlsService;
    @Autowired private RiskMechanicService riskService;
    @Autowired private ChildControlsRepository controlsRepo;
    @Autowired private ControlsAuditRepository auditRepo;
    @Autowired private PlayTimeLedger ledger;
    @Autowired private ChildSessionRepository sessionRepo;
    @Autowired private TestScenarioContext context;
    @Autowired private ObjectMapper objectMapper;
    @LocalServerPort private int port;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    private ChildControls openedSettings;
    private RoundPoolSnapshot lastRestored;
    private int starsBeforeWrong;
    private int itemsBeforeWrong;

    @Before
    public void reset() {
        openedSettings = null;
        lastRestored = null;
        starsBeforeWrong = 0;
        itemsBeforeWrong = 0;
    }

    private UUID parentId() {
        UUID p = context.parentId();
        assertThat(p).as("parentId must be set by the Background step").isNotNull();
        return p;
    }

    private UUID childId() {
        UUID c = context.childId();
        assertThat(c).as("childId must be set by the Background step").isNotNull();
        return c;
    }

    @Given("a verified parent account with at least one child profile")
    public void aVerifiedParentAccountWithAtLeastOneChildProfile() throws Exception {
        if (context.parentId() == null || context.childId() == null) {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/test/child-setup?pin=1234"))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            assertThat(resp.statusCode()).isEqualTo(200);
            Map<String, Object> body = objectMapper.readValue(resp.body(), Map.class);
            context.setParentId(UUID.fromString((String) body.get("parentId")));
            context.setChildId(UUID.fromString((String) body.get("childId")));
        }
        controlsRepo.findByChildId(childId()).orElseGet(() -> {
            ChildControls defaults = ChildControls.defaults(childId(), parentId());
            controlsRepo.save(defaults);
            return defaults;
        });
    }

    // ── Scenario 1: Daily limit takes effect immediately ───────────────────

    @Given("the child has already played {int} minutes today")
    public void theChildHasAlreadyPlayedMinutesToday(int minutes) {
        ledger.addMinutes(childId(), LocalDate.now(ZoneOffset.UTC), minutes);
        // Insert an active session into the IAM repo so it can be revoked.
        ChildSession session = new ChildSession(childId(), parentId());
        sessionRepo.save(session);
    }

    @When("the parent sets the daily limit to {int} minutes")
    public void theParentSetsTheDailyLimitToMinutes(int minutes) {
        controlsService.updateControls(parentId(), childId(), minutes, 15, false, false);
    }

    @Then("the system terminates the running child session cleanly")
    public void theSystemTerminatesTheRunningChildSessionCleanly() {
        var active = sessionRepo
                .findFirstByChildIdAndRevokedAtIsNullOrderByCreatedAtDesc(childId());
        assertThat(active).as("no active session must remain").isEmpty();
        assertThat(auditRepo.findByChildId(childId()))
                .anyMatch(e -> e.action() == ControlsAction.SESSION_TERMINATED_BY_LIMIT);
    }

    @Then("a new child session can no longer be started today")
    public void aNewChildSessionCanNoLongerBeStartedToday() {
        assertThat(controlsService.canStartSession(childId())).isFalse();
    }

    // ── Scenario 2: Risk mechanic disabled by default ──────────────────────

    @Given("a newly created child profile")
    public void aNewlyCreatedChildProfile() {
        // Make sure no controls record yet — defaults must apply.
    }

    @When("the parent opens the play-time settings")
    public void theParentOpensThePlayTimeSettings() {
        openedSettings = controlsService.getOrDefault(parentId(), childId());
    }

    @Then("the risk mechanic is marked as disabled")
    public void theRiskMechanicIsMarkedAsDisabled() {
        assertThat(openedSettings).isNotNull();
        assertThat(openedSettings.riskMechanicEnabled()).isFalse();
    }

    // ── Scenario 3: Risk mechanic causes no permanent loss ────────────────

    @Given("an enabled risk mechanic")
    public void anEnabledRiskMechanic() {
        controlsService.updateControls(parentId(), childId(), 30, 15, true, false);
        starsBeforeWrong = 12;
        itemsBeforeWrong = 2;
    }

    @When("the child answers a task wrong")
    public void theChildAnswersATaskWrong() {
        riskService.recordWrongAnswer(childId(), 3, 1);
        lastRestored = riskService.endMatch(childId());
    }

    @Then("star points and items remain unchanged or are restored within the same match")
    public void starPointsAndItemsRemainUnchangedOrAreRestoredWithinTheSameMatch() {
        assertThat(lastRestored).as("end-of-match restore must run").isNotNull();
        // Whatever was put into the round pool comes back: net zero loss.
        int starsAfter = starsBeforeWrong - 3 + lastRestored.starPointsInPool();
        int itemsAfter = itemsBeforeWrong - 1 + lastRestored.itemsInPool();
        assertThat(starsAfter).isEqualTo(starsBeforeWrong);
        assertThat(itemsAfter).isEqualTo(itemsBeforeWrong);
    }

    // ── Scenario 4: Auditable change ───────────────────────────────────────

    @Given("the parent changes the daily limit from {int} to {int} minutes")
    public void theParentChangesTheDailyLimitFromTo(int from, int to) {
        controlsRepo.save(new ChildControls(childId(), parentId(), from, 15, false));
        controlsService.updateControls(parentId(), childId(), to, 15, false, false);
    }

    @Then("the audit log contains an entry with before and after value as well as a timestamp")
    public void theAuditLogContainsAnEntryWithBeforeAndAfterValueAsWellAsATimestamp() {
        List<ControlsAuditEntry> all = auditRepo.findByChildId(childId());
        assertThat(all).anySatisfy(e -> {
            assertThat(e.action()).isEqualTo(ControlsAction.CONTROLS_UPDATED);
            assertThat(e.field()).isEqualTo("dailyLimitMinutes");
            assertThat(e.beforeValue()).isNotBlank();
            assertThat(e.afterValue()).isNotBlank();
            assertThat(e.timestamp()).isNotNull();
        });
    }
}
