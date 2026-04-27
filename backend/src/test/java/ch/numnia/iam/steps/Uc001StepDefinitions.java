package ch.numnia.iam.steps;

import ch.numnia.iam.domain.AuditAction;
import ch.numnia.iam.domain.ChildStatus;
import ch.numnia.iam.domain.ParentStatus;
import ch.numnia.iam.domain.TokenPurpose;
import ch.numnia.iam.spi.AuditLogRepository;
import ch.numnia.iam.spi.ChildProfileRepository;
import ch.numnia.iam.spi.ParentAccountRepository;
import ch.numnia.iam.spi.VerificationTokenRepository;
import tools.jackson.databind.ObjectMapper;
import io.cucumber.java.Before;
import io.cucumber.java.en.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Cucumber step definitions for UC-001 scenarios.
 *
 * <p>Uses Java's built-in {@link HttpClient} against the embedded SpringBootTest server
 * (H2 in-memory, test profile). The Spring context is shared via
 * {@link ch.numnia.CucumberSpringConfiguration}.
 *
 * <p>The e2e test-helper endpoint ({@code GET /api/test/verification-tokens})
 * is used to retrieve tokens without a real mail server. It is only active when
 * {@code numnia.e2e.enabled=true} (set in {@code application-test.yaml}).
 */
public class Uc001StepDefinitions {

    @LocalServerPort
    private int port;

    @Autowired
    private ParentAccountRepository parentRepo;

    @Autowired
    private ChildProfileRepository childRepo;

    @Autowired
    private VerificationTokenRepository tokenRepo;

    @Autowired
    private AuditLogRepository auditRepo;

    @Autowired
    private ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    // ── Scenario state ────────────────────────────────────────────────────
    private String testEmail;
    private UUID parentId;
    private UUID childId;
    private int lastStatusCode;
    private Map<String, Object> lastResponseBody;

    @Before
    public void resetScenarioState() {
        testEmail = null;
        parentId = null;
        childId = null;
        lastStatusCode = 0;
        lastResponseBody = null;
    }

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    private HttpResponse<String> post(String path, Object body) throws Exception {
        String json = objectMapper.writeValueAsString(body);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl() + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseBody(HttpResponse<String> resp) throws Exception {
        if (resp.body() == null || resp.body().isBlank()) {
            return Map.of();
        }
        return objectMapper.readValue(resp.body(), Map.class);
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl() + path))
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    // ── Background steps ─────────────────────────────────────────────────

    @Given("the Numnia landing page is reachable over HTTPS")
    public void theLandingPageIsReachable() {
        // Precondition: the embedded test server is running. HTTPS enforcement
        // is handled by the reverse proxy in production (documented in arc42).
    }

    @Given("the language is Swiss High German without sharp s")
    public void theLanguageIsSwissHighGerman() {
        // Documented precondition: NFR-I18N-001/002. UI copy assertions are
        // covered in the frontend Vitest suite.
    }

    // ── Scenario 1: Successful registration with double opt-in ────────────

    @Given("a new parent with a valid email address")
    public void aNewParentWithValidEmail() {
        testEmail = "parent-" + UUID.randomUUID() + "@numnia-test.example.com";
    }

    @When("the parent fully completes the registration form")
    public void parentCompletesRegistrationForm() throws Exception {
        var body = Map.of(
                "firstName", "Anna",
                "salutation", "Frau",
                "email", testEmail,
                "password", "SecurePass123",
                "privacyConsented", true,
                "termsAccepted", true
        );
        var response = post("/api/parents", body);
        lastStatusCode = response.statusCode();
        lastResponseBody = parseBody(response);
        assertThat(lastStatusCode).as("registration should return 201").isEqualTo(201);
        parentId = UUID.fromString((String) lastResponseBody.get("parentId"));
    }

    @When("confirms the link from the first verification email")
    public void confirmsLinkFromFirstVerificationEmail() throws Exception {
        var tokenResp = get("/api/test/verification-tokens?email=" + testEmail
                + "&purpose=EMAIL_PRIMARY");
        assertThat(tokenResp.statusCode()).isEqualTo(200);
        @SuppressWarnings("unchecked")
        Map<String, String> tokenBody = objectMapper.readValue(tokenResp.body(), Map.class);
        String token = tokenBody.get("token");

        var verifyResp = post("/api/parents/verify", Map.of("token", token));
        assertThat(verifyResp.statusCode()).as("primary email verify should return 200").isEqualTo(200);
    }

    @When("creates a child profile with a fantasy name, year of birth 9 and avatar base model")
    public void createsChildProfile() throws Exception {
        int yearOfBirth = java.time.LocalDate.now().getYear() - 9;
        var body = Map.of(
                "pseudonym", "Luna",
                "yearOfBirth", yearOfBirth,
                "avatarBaseModel", "star"
        );
        var response = post("/api/parents/" + parentId + "/child-profiles", body);
        lastStatusCode = response.statusCode();
        lastResponseBody = parseBody(response);
        assertThat(lastStatusCode).as("child profile creation should return 201").isEqualTo(201);
        childId = UUID.fromString((String) lastResponseBody.get("childProfileId"));
    }

    @When("confirms the link from the second confirmation email")
    public void confirmsLinkFromSecondConfirmationEmail() throws Exception {
        var tokenResp = get("/api/test/verification-tokens?email=" + testEmail
                + "&purpose=EMAIL_SECONDARY");
        assertThat(tokenResp.statusCode()).isEqualTo(200);
        @SuppressWarnings("unchecked")
        Map<String, String> tokenBody = objectMapper.readValue(tokenResp.body(), Map.class);
        String token = tokenBody.get("token");

        var confirmResp = post(
                "/api/parents/" + parentId + "/child-profiles/" + childId + "/confirm",
                Map.of("token", token));
        assertThat(confirmResp.statusCode()).as("secondary confirm should return 200").isEqualTo(200);
    }

    @Then("the parent account is verified")
    public void theParentAccountIsVerified() {
        var parent = parentRepo.findById(parentId).orElseThrow();
        assertThat(parent.getStatus()).isEqualTo(ParentStatus.FULLY_CONSENTED);
    }

    @Then("the child profile exists under a pseudonym")
    public void theChildProfileExistsUnderPseudonym() {
        var profiles = childRepo.findAllByParentId(parentId);
        assertThat(profiles).hasSize(1);
        assertThat(profiles.get(0).getPseudonym()).isEqualTo("Luna");
        assertThat(profiles.get(0).getStatus()).isEqualTo(ChildStatus.ACTIVE);
    }

    @Then("the two-step consent is documented in the audit log")
    public void theTwoStepConsentIsDocumentedInAuditLog() {
        var entries = auditRepo.findAllByParentRefOrderByTimestampAsc(parentId.toString());
        var actions = entries.stream().map(e -> e.getAction()).toList();
        assertThat(actions).contains(
                AuditAction.ACCOUNT_CREATED,
                AuditAction.EMAIL_PRIMARY_VERIFIED,
                AuditAction.CHILD_PROFILE_CREATED,
                AuditAction.EMAIL_SECONDARY_CONFIRMED
        );
    }

    // ── Scenario 2: Year of birth outside target group ────────────────────

    @Given("a verified parent in the {string} step")
    public void aVerifiedParentInStep(String step) throws Exception {
        testEmail = "verified-" + UUID.randomUUID() + "@numnia-test.example.com";

        // Register
        var regBody = Map.of(
                "firstName", "Klaus",
                "salutation", "Herr",
                "email", testEmail,
                "password", "SecurePass123",
                "privacyConsented", true,
                "termsAccepted", true
        );
        var regResp = post("/api/parents", regBody);
        assertThat(regResp.statusCode()).isEqualTo(201);
        parentId = UUID.fromString(
                (String) parseBody(regResp).get("parentId"));

        // Verify primary email
        var tokenResp = get("/api/test/verification-tokens?email=" + testEmail
                + "&purpose=EMAIL_PRIMARY");
        @SuppressWarnings("unchecked")
        Map<String, String> tokenBody = objectMapper.readValue(tokenResp.body(), Map.class);
        post("/api/parents/verify", Map.of("token", tokenBody.get("token")));
    }

    @When("the parent picks a year of birth corresponding to an age below 7")
    public void parentPicksYearOfBirthBelowSeven() throws Exception {
        int yearForAge6 = java.time.LocalDate.now().getYear() - 6;
        var body = Map.of(
                "pseudonym", "Nova",
                "yearOfBirth", yearForAge6,
                "avatarBaseModel", "moon"
        );
        var response = post("/api/parents/" + parentId + "/child-profiles", body);
        lastStatusCode = response.statusCode();
        lastResponseBody = parseBody(response);
    }

    @Then("the system shows a notice about the 7-12 target group")
    public void systemShowsNoticeAboutTargetGroup() {
        assertThat(lastStatusCode).isEqualTo(422); // UNPROCESSABLE_ENTITY
        String message = (String) lastResponseBody.get("message");
        assertThat(message).containsIgnoringCase("7-12");
    }

    @Then("no child profile is created")
    public void noChildProfileIsCreated() {
        assertThat(childRepo.countByParentId(parentId)).isZero();
    }

    // ── Scenario 3: First verification email expired ──────────────────────

    @Given("a registered but unverified parent account")
    public void aRegisteredButUnverifiedParentAccount() throws Exception {
        testEmail = "unverified-" + UUID.randomUUID() + "@numnia-test.example.com";
        var body = Map.of(
                "firstName", "Maria",
                "salutation", "Frau",
                "email", testEmail,
                "password", "SecurePass123",
                "privacyConsented", true,
                "termsAccepted", true
        );
        var response = post("/api/parents", body);
        parentId = UUID.fromString((String) parseBody(response).get("parentId"));
    }

    @Given("the verification link is older than 24 hours")
    public void theVerificationLinkIsOlderThan24Hours() {
        // Mark the current primary token as consumed, then save a new one that
        // has already expired. This simulates an expired link scenario.
        tokenRepo.findFirstByParentIdAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(
                parentId, TokenPurpose.EMAIL_PRIMARY
        ).ifPresent(token -> {
            token.consume();
            tokenRepo.save(token);
        });
        // Save an expired (unconsumed) token
        var expiredToken = new ch.numnia.iam.domain.VerificationToken(
                parentId, null, TokenPurpose.EMAIL_PRIMARY,
                Instant.now().minusSeconds(25 * 60 * 60));
        tokenRepo.save(expiredToken);
    }

    @When("the parent opens the link")
    public void theParentOpensTheLink() throws Exception {
        // Find the expired unconsumed token directly from the repo
        var expiredToken = tokenRepo
                .findFirstByParentIdAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(
                        parentId, TokenPurpose.EMAIL_PRIMARY)
                .orElseThrow(() -> new AssertionError("Expected an expired token to be present"));
        var response = post("/api/parents/verify",
                Map.of("token", expiredToken.getId().toString()));
        lastStatusCode = response.statusCode();
        lastResponseBody = parseBody(response);
    }

    @Then("the system offers {string}")
    public void theSystemOffers(String offer) {
        assertThat(lastStatusCode).isEqualTo(410); // GONE
        assertThat(lastResponseBody.get("error")).isEqualTo("TOKEN_EXPIRED");
    }

    @Then("the account remains unverified")
    public void theAccountRemainsUnverified() {
        var parent = parentRepo.findById(parentId).orElseThrow();
        assertThat(parent.getStatus()).isEqualTo(ParentStatus.NOT_VERIFIED);
    }

    // ── Scenario 4: Duplicate registration is prevented ───────────────────

    @Given("an already registered email address")
    public void anAlreadyRegisteredEmailAddress() throws Exception {
        testEmail = "duplicate-" + UUID.randomUUID() + "@numnia-test.example.com";
        var body = Map.of(
                "firstName", "Paul",
                "salutation", "Herr",
                "email", testEmail,
                "password", "SecurePass123",
                "privacyConsented", true,
                "termsAccepted", true
        );
        post("/api/parents", body);
    }

    @When("another registration with the same address is attempted")
    public void anotherRegistrationWithSameAddress() throws Exception {
        var body = Map.of(
                "firstName", "Paula",
                "salutation", "Frau",
                "email", testEmail,
                "password", "AnotherPass456",
                "privacyConsented", true,
                "termsAccepted", true
        );
        var response = post("/api/parents", body);
        lastStatusCode = response.statusCode();
        lastResponseBody = parseBody(response);
    }

    @Then("the system shows a notice about the existing account")
    public void systemShowsNoticeAboutExistingAccount() {
        assertThat(lastStatusCode).isEqualTo(409); // CONFLICT
        assertThat(lastResponseBody.get("error")).isEqualTo("DUPLICATE_EMAIL");
    }

    @Then("no new account is created")
    public void noNewAccountIsCreated() {
        long count = parentRepo.findAll().stream()
                .filter(p -> p.getEmail().equals(testEmail))
                .count();
        assertThat(count).isEqualTo(1);
    }
}
