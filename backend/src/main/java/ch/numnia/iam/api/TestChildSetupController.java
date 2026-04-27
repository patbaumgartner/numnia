package ch.numnia.iam.api;

import ch.numnia.iam.domain.*;
import ch.numnia.iam.service.ChildSignInService;
import ch.numnia.iam.service.ParentRegistrationService;
import ch.numnia.iam.spi.ChildProfileRepository;
import ch.numnia.iam.spi.ChildSessionRepository;
import ch.numnia.iam.spi.ParentAccountRepository;
import ch.numnia.iam.spi.VerificationTokenRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * E2E-only test helper for UC-002 scenarios.
 *
 * <p>Sets up the UC-002 precondition state (verified parent + active child profile + PIN)
 * without going through the email verification flow, and directly creates child sessions
 * for testing.
 *
 * <p><strong>Security note:</strong> Only active when {@code numnia.e2e.enabled=true}.
 * Never present in production.
 *
 * <p>Endpoints:
 * <pre>
 *   POST /api/test/child-setup    — creates parent + child + sets PIN; returns IDs
 *   POST /api/test/child-session  — creates a child session directly (bypasses PIN)
 * </pre>
 */
@RestController
@RequestMapping("/api/test")
@ConditionalOnProperty(name = "numnia.e2e.enabled", havingValue = "true")
public class TestChildSetupController {

    private final ParentAccountRepository parents;
    private final ChildProfileRepository childProfiles;
    private final VerificationTokenRepository tokens;
    private final ChildSessionRepository sessions;
    private final PasswordEncoder passwordEncoder;
    private final ChildSignInService childSignInService;
    private final ParentRegistrationService parentRegistrationService;

    private static final Set<String> VETTED_NAMES = Set.of(
            "Astra", "Blitz", "Comet", "Deva", "Echo",
            "Flair", "Glint", "Halo", "Iris", "Jade",
            "Kite", "Luna", "Miro", "Nova", "Orion",
            "Pixel", "Quest", "Rho", "Sol", "Terra",
            "Uno", "Vega", "Wave", "Xeno", "Yuki", "Zara");

    public TestChildSetupController(ParentAccountRepository parents,
                                    ChildProfileRepository childProfiles,
                                    VerificationTokenRepository tokens,
                                    ChildSessionRepository sessions,
                                    PasswordEncoder passwordEncoder,
                                    ChildSignInService childSignInService,
                                    ParentRegistrationService parentRegistrationService) {
        this.parents = parents;
        this.childProfiles = childProfiles;
        this.tokens = tokens;
        this.sessions = sessions;
        this.passwordEncoder = passwordEncoder;
        this.childSignInService = childSignInService;
        this.parentRegistrationService = parentRegistrationService;
    }

    /**
     * Creates a fully set-up UC-002 precondition: verified parent, active child, PIN set.
     *
     * <p>Accepts optional {@code pin} query param (default {@code "1234"}).
     *
     * @return JSON with {@code parentId}, {@code childId}, {@code parentEmail}, {@code pin}
     */
    @PostMapping("/child-setup")
    public ResponseEntity<Map<String, String>> setupChildProfile(
            @RequestParam(defaultValue = "1234") String pin) {

        // Create parent (bypassing email verification)
        UUID parentId = UUID.randomUUID();
        String email = "e2e-parent-" + parentId + "@numnia-test.example.com";
        String hashedPassword = passwordEncoder.encode("E2eTestPass123");
        ParentAccount parent = new ParentAccount(
                parentId, email, hashedPassword, "E2E", "Frau", true, true);
        parent.markEmailVerified();
        parent.markFullyConsented();
        parents.save(parent);

        // Create active child profile
        UUID childId = UUID.randomUUID();
        int yearOfBirth = LocalDate.now().getYear() - 9;
        ChildProfile profile = new ChildProfile(childId, "Luna", yearOfBirth, "star", parentId);
        profile.activate();
        childProfiles.save(profile);

        // Set the PIN directly (bypasses service validation for test setup speed)
        profile.setPinHash(passwordEncoder.encode(pin));
        childProfiles.save(profile);

        return ResponseEntity.ok(Map.of(
                "parentId", parentId.toString(),
                "childId", childId.toString(),
                "parentEmail", email,
                "pin", pin
        ));
    }

    /**
     * Creates a CHILD session directly — for E2E scenarios that test behavior
     * <em>after</em> a successful sign-in (e.g., cross-area authz test).
     *
     * @param childId UUID of an existing child profile
     * @return JSON with {@code sessionToken}
     */
    @PostMapping("/child-session")
    public ResponseEntity<Map<String, String>> createChildSession(
            @RequestParam UUID childId) {

        ChildProfile profile = childProfiles.findById(childId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Child profile not found: " + childId));

        ChildSession session = new ChildSession(childId, profile.getParentId());
        sessions.save(session);

        return ResponseEntity.ok(Map.of("sessionToken", session.getId().toString()));
    }
}
