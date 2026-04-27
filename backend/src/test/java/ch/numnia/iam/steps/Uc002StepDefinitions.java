package ch.numnia.iam.steps;

import ch.numnia.iam.domain.AuditAction;
import ch.numnia.iam.spi.AuditLogRepository;
import ch.numnia.iam.spi.ChildProfileRepository;
import ch.numnia.iam.spi.ChildSessionRepository;
import tools.jackson.databind.ObjectMapper;
import io.cucumber.java.Before;
import io.cucumber.java.en.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Cucumber step definitions for UC-002 scenarios.
 *
 * <p>Uses the E2E test helper endpoints ({@code /api/test/child-setup},
 * {@code /api/test/child-session}) to set up the UC-002 precondition state
 * (verified parent + active child + PIN) without going through the email flow.
 *
 * <p>Privacy: all identifiers referenced as UUIDs or pseudonyms only (NFR-PRIV-001).
 */
public class Uc002StepDefinitions {

    @LocalServerPort
    private int port;

    @Autowired
    private ChildProfileRepository childProfileRepo;

    @Autowired
    private ChildSessionRepository childSessionRepo;

    @Autowired
    private AuditLogRepository auditRepo;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ch.numnia.test.TestScenarioContext scenarioContext;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    // ── Scenario state ────────────────────────────────────────────────────
    private UUID parentId;
    private UUID childId;
    private String pin;
    private String sessionToken;
    private int lastStatusCode;
    private Map<String, Object> lastResponseBody;

    @Before
    public void resetScenarioState() {
        parentId = null;
        childId = null;
        pin = null;
        sessionToken = null;
        lastStatusCode = 0;
        lastResponseBody = null;
        scenarioContext.reset();
    }

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    private HttpResponse<String> post(String path, Object body) throws Exception {
        String json = objectMapper.writeValueAsString(body);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl() + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        return httpClient.send(req, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> postWithSession(String path, Object body,
                                                  String token) throws Exception {
        String json = objectMapper.writeValueAsString(body);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl() + path))
                .header("Content-Type", "application/json")
                .header("X-Numnia-Session", token)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        return httpClient.send(req, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> getWithSession(String path, String token) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl() + path))
                .header("X-Numnia-Session", token)
                .GET()
                .build();
        return httpClient.send(req, HttpResponse.BodyHandlers.ofString());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseBody(HttpResponse<String> resp) throws Exception {
        if (resp.body() == null || resp.body().isBlank()) return Map.of();
        return objectMapper.readValue(resp.body(), Map.class);
    }

    // ── Background steps ──────────────────────────────────────────────────

    @Given("a verified parent account with a ready-to-play child profile")
    public void aVerifiedParentAccountWithReadyToPlayChildProfile() throws Exception {
        // Use the E2E helper to create the full precondition state
        var resp = post("/api/test/child-setup?pin=1234", null);
        assertThat(resp.statusCode()).as("child-setup should succeed").isEqualTo(200);
        var body = parseBody(resp);
        parentId = UUID.fromString((String) body.get("parentId"));
        childId = UUID.fromString((String) body.get("childId"));
        pin = (String) body.get("pin");
    }

    @Given("a PIN set by the parent")
    public void aPinSetByTheParent() {
        // PIN is set as part of the child-setup — precondition already satisfied
        assertThat(pin).as("PIN must have been set in Background").isNotNull();
    }

    // ── Scenario 1: Successful sign-in ────────────────────────────────────

    @Given("the child opens the landing page")
    public void theChildOpensTheLandingPage() {
        // Precondition check: child profile exists and is ready
        assertThat(childId).isNotNull();
    }

    @When("it picks its avatar and enters the correct PIN")
    public void itPicksAvatarAndEntersCorrectPin() throws Exception {
        var body = Map.of("childId", childId.toString(), "pin", pin);
        var resp = post("/api/child-sessions", body);
        lastStatusCode = resp.statusCode();
        lastResponseBody = parseBody(resp);
        assertThat(lastStatusCode).as("sign-in with correct PIN should return 201").isEqualTo(201);
        sessionToken = (String) lastResponseBody.get("sessionToken");
    }

    @Then("the system creates a child session with restricted rights")
    public void theSystemCreatesChildSessionWithRestrictedRights() {
        assertThat(sessionToken).as("session token must be present").isNotNull();
        assertThat(lastResponseBody.get("role")).isEqualTo("CHILD");
    }

    @Then("the main menu is visible")
    public void theMainMenuIsVisible() {
        // Backend postcondition: session exists in the repository
        UUID tokenId = UUID.fromString(sessionToken);
        assertThat(childSessionRepo.findById(tokenId)).isPresent();
        assertThat(childSessionRepo.findById(tokenId).get().isValid()).isTrue();
    }

    // ── Scenario 2: Profile locked after five failed attempts ─────────────

    @Given("a child profile with a valid PIN")
    public void aChildProfileWithAValidPin() {
        // Already set up in Background — re-assert precondition
        assertThat(childId).isNotNull();
        assertThat(pin).isNotNull();
    }

    @When("a wrong PIN is entered five times in a row")
    public void aWrongPinIsEnteredFiveTimesInARow() throws Exception {
        for (int i = 0; i < 5; i++) {
            var body = Map.of("childId", childId.toString(), "pin", "0000");
            var resp = post("/api/child-sessions", body);
            // First 4 should be 401 UNAUTHORIZED (wrong PIN), 5th should be 423 LOCKED
            lastStatusCode = resp.statusCode();
        }
    }

    @Then("the child profile is locked until the parent releases it")
    public void theChildProfileIsLockedUntilParentReleasesIt() {
        var profile = childProfileRepo.findById(childId).orElseThrow();
        assertThat(profile.isLocked()).as("profile must be locked after 5 failed attempts").isTrue();
    }

    @Then("the parent receives a notification email")
    public void theParentReceivesANotificationEmail() {
        // Audit trail confirms the lock event was recorded (email is no-op in tests)
        var entries = auditRepo.findAllByParentRefOrderByTimestampAsc(parentId.toString());
        var actions = entries.stream().map(e -> e.getAction()).toList();
        assertThat(actions).contains(AuditAction.CHILD_PROFILE_LOCKED);
    }

    // ── Scenario 3: Child session must not call a parent endpoint ──────────

    @Given("an active child session")
    public void anActiveChildSession() throws Exception {
        if (childId == null) {
            // Bootstrap a fresh parent + child + PIN for scenarios that start cold
            var setup = post("/api/test/child-setup", null);
            assertThat(setup.statusCode()).as("child-setup should succeed").isEqualTo(200);
            var setupBody = parseBody(setup);
            parentId = UUID.fromString((String) setupBody.get("parentId"));
            childId = UUID.fromString((String) setupBody.get("childId"));
            pin = (String) setupBody.get("pin");
        }
        scenarioContext.setChildId(childId);
        // Create a fresh child session via the E2E test helper
        var resp = post("/api/test/child-session?childId=" + childId, null);
        assertThat(resp.statusCode()).as("creating child session should succeed").isEqualTo(200);
        var body = parseBody(resp);
        sessionToken = (String) body.get("sessionToken");
        assertThat(sessionToken).isNotNull();
    }

    @When("an attempt is made to call a parent endpoint")
    public void anAttemptIsMadeToCallAParentEndpoint() throws Exception {
        var resp = getWithSession("/api/parents/me", sessionToken);
        lastStatusCode = resp.statusCode();
        lastResponseBody = parseBody(resp);
    }

    @Then("the server responds with status 403")
    public void theServerRespondsWith403() {
        assertThat(lastStatusCode).as("child session must receive 403 on parent endpoint")
                .isEqualTo(403);
    }

    @Then("the incident is documented in the audit log")
    public void theIncidentIsDocumentedInAuditLog() {
        var entries = auditRepo.findAllByParentRefOrderByTimestampAsc(parentId.toString());
        var actions = entries.stream().map(e -> e.getAction()).toList();
        assertThat(actions).contains(AuditAction.PARENT_ENDPOINT_DENIED_FOR_CHILD);
    }
}
