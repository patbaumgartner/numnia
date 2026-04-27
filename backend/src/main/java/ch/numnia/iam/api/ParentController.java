package ch.numnia.iam.api;

import ch.numnia.iam.api.dto.*;
import ch.numnia.iam.service.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * REST controller for parent account registration and child profile management.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>{@code POST /api/parents} — register a new parent account
 *   <li>{@code POST /api/parents/verify} — confirm primary email
 *   <li>{@code POST /api/parents/{parentId}/child-profiles} — create child profile
 *   <li>{@code POST /api/parents/{parentId}/child-profiles/{childId}/confirm} — confirm secondary email
 *   <li>{@code POST /api/parents/{parentId}/child-profiles/{childId}/pin} — set/change child PIN (UC-002)
 *   <li>{@code POST /api/parents/{parentId}/child-profiles/{childId}/release-lock} — release child lock (UC-002)
 *   <li>{@code GET  /api/parents/me} — placeholder parent area (UC-002 cross-area authz test)
 * </ul>
 *
 * <p>All inputs are validated server-side via Bean Validation (NFR-SEC-001).
 */
@RestController
@RequestMapping("/api/parents")
public class ParentController {

    private final ParentRegistrationService registrationService;
    private final ChildProfileService childProfileService;
    private final ChildSignInService childSignInService;

    public ParentController(ParentRegistrationService registrationService,
                            ChildProfileService childProfileService,
                            ChildSignInService childSignInService) {
        this.registrationService = registrationService;
        this.childProfileService = childProfileService;
        this.childSignInService = childSignInService;
    }

    /**
     * Registers a new parent account (UC-001 main flow steps 2-4).
     *
     * @return 201 Created with the new parent UUID, or 409 Conflict on duplicate email
     */
    @PostMapping
    public ResponseEntity<RegisterParentResponse> registerParent(
            @Valid @RequestBody RegisterParentRequest request) {
        UUID parentId = registrationService.register(
                request.email(),
                request.password(),
                request.firstName(),
                request.salutation(),
                request.privacyConsented(),
                request.termsAccepted()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new RegisterParentResponse(parentId));
    }

    /**
     * Confirms the parent's primary email address (UC-001 main flow step 5).
     *
     * @return 200 OK with the parent status, or 410 Gone if expired
     */
    @PostMapping("/verify")
    public ResponseEntity<VerifyEmailResponse> verifyEmail(
            @Valid @RequestBody VerifyEmailRequest request) {
        UUID parentId = registrationService.verifyPrimaryEmail(request.token());
        return ResponseEntity.ok(new VerifyEmailResponse(parentId, "EMAIL_VERIFIED"));
    }

    /**
     * Creates a child profile for a verified parent (UC-001 main flow steps 7-9).
     *
     * @return 201 Created with the child profile UUID and pseudonym
     */
    @PostMapping("/{parentId}/child-profiles")
    public ResponseEntity<CreateChildProfileResponse> createChildProfile(
            @PathVariable UUID parentId,
            @Valid @RequestBody CreateChildProfileRequest request) {
        UUID childId = childProfileService.createChildProfile(
                parentId,
                request.pseudonym(),
                request.yearOfBirth(),
                request.avatarBaseModel()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CreateChildProfileResponse(childId, request.pseudonym()));
    }

    /**
     * Confirms the secondary consent for a child profile (UC-001 main flow steps 10-11).
     *
     * @return 200 OK on success
     */
    @PostMapping("/{parentId}/child-profiles/{childId}/confirm")
    public ResponseEntity<Void> confirmChildProfile(
            @PathVariable UUID parentId,
            @PathVariable UUID childId,
            @Valid @RequestBody ConfirmChildProfileRequest request) {
        childProfileService.confirmChildProfile(request.token());
        return ResponseEntity.ok().build();
    }

    /**
     * Sets or changes the PIN for a child profile (UC-002 precondition).
     *
     * <p>Server-side authorization: the parentId in the path must own the childId
     * (enforced in {@link ChildSignInService#setPin}).
     *
     * @return 200 OK on success
     */
    @PostMapping("/{parentId}/child-profiles/{childId}/pin")
    public ResponseEntity<Void> setChildPin(
            @PathVariable UUID parentId,
            @PathVariable UUID childId,
            @Valid @RequestBody SetChildPinRequest request) {
        childSignInService.setPin(parentId, childId, request.pin());
        return ResponseEntity.ok().build();
    }

    /**
     * Releases a lock on a child profile (UC-002 alt flow 5a).
     *
     * <p>Server-side authorization: the parentId in the path must own the childId.
     *
     * @return 200 OK on success
     */
    @PostMapping("/{parentId}/child-profiles/{childId}/release-lock")
    public ResponseEntity<Void> releaseLock(
            @PathVariable UUID parentId,
            @PathVariable UUID childId) {
        childSignInService.releaseLock(parentId, childId);
        return ResponseEntity.ok().build();
    }

    /**
     * Minimal parent area placeholder (UC-002 cross-area authz test target).
     *
     * <p>A valid CHILD session calling this endpoint is blocked by
     * {@code SessionInterceptor} with 403 (UC-002 exception flow).
     * This endpoint is expanded in UC-009 with full parent authentication.
     *
     * @return 200 OK with greeting for authenticated parent callers
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, String>> getParentProfile() {
        return ResponseEntity.ok(Map.of("message", "Hallo, Elternteil!"));
    }
}

