package ch.numnia.iam.api;

import ch.numnia.iam.domain.TokenPurpose;
import ch.numnia.iam.domain.VerificationToken;
import ch.numnia.iam.spi.ParentAccountRepository;
import ch.numnia.iam.spi.VerificationTokenRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * E2E-only test helper endpoint for retrieving verification tokens by email address.
 *
 * <p><strong>Security note:</strong> This controller is only registered when
 * {@code numnia.e2e.enabled=true} (controlled via {@code application-test.yaml}
 * and {@code application-e2e.yaml}). It is <em>never</em> present in the
 * production profile ({@code application.yaml} has no such property).
 *
 * <p>The endpoint allows Cucumber / Playwright step definitions to retrieve the
 * most recent verification token for a given email without an actual email server,
 * simulating the email side-channel in test environments only.
 *
 * <p>Endpoint:
 * <pre>GET /api/test/verification-tokens?email=&lt;email&gt;&amp;purpose=EMAIL_PRIMARY|EMAIL_SECONDARY</pre>
 */
@RestController
@RequestMapping("/api/test")
@ConditionalOnProperty(name = "numnia.e2e.enabled", havingValue = "true")
public class TestTokenController {

    private final ParentAccountRepository parents;
    private final VerificationTokenRepository tokens;

    public TestTokenController(ParentAccountRepository parents,
                               VerificationTokenRepository tokens) {
        this.parents = parents;
        this.tokens = tokens;
    }

    /**
     * Returns the most recent unconsumed verification token for the given email.
     *
     * @param email   Parent email address
     * @param purpose {@code EMAIL_PRIMARY} or {@code EMAIL_SECONDARY} (default: EMAIL_PRIMARY)
     * @return 200 with {@code {"token": "<uuid>"}} or 404 if not found
     */
    @GetMapping("/verification-tokens")
    public ResponseEntity<Map<String, String>> getLatestToken(
            @RequestParam String email,
            @RequestParam(defaultValue = "EMAIL_PRIMARY") String purpose) {

        return parents.findByEmail(email)
                .flatMap(parent -> tokens
                        .findFirstByParentIdAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(
                                parent.getId(), TokenPurpose.valueOf(purpose)))
                .map(t -> ResponseEntity.ok(Map.of("token", t.getId().toString())))
                .orElse(ResponseEntity.notFound().build());
    }
}
