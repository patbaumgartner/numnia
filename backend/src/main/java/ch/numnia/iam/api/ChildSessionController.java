package ch.numnia.iam.api;

import ch.numnia.iam.api.dto.SignInChildRequest;
import ch.numnia.iam.api.dto.SignInChildResponse;
import ch.numnia.iam.domain.ChildSession;
import ch.numnia.iam.service.ChildSignInService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for child sign-in and sign-out (UC-002).
 *
 * <p>Endpoints:
 * <ul>
 *   <li>{@code POST /api/child-sessions} — sign in with childId + PIN; returns session token
 *   <li>{@code DELETE /api/child-sessions/current} — sign out (revokes the current session)
 * </ul>
 *
 * <p>Authentication: the session token is passed as the {@code X-Numnia-Session} request
 * header in all subsequent calls. Sign-in is the only endpoint that does not require it.
 *
 * <p>All inputs are validated server-side via Bean Validation (NFR-SEC-001).
 */
@RestController
@RequestMapping("/api/child-sessions")
public class ChildSessionController {

    private final ChildSignInService childSignInService;

    public ChildSessionController(ChildSignInService childSignInService) {
        this.childSignInService = childSignInService;
    }

    /**
     * Signs a child in with the given PIN (UC-002 main flow steps 4-6).
     *
     * @return 201 Created with session token + role, 401 on wrong PIN, 423 on locked profile
     */
    @PostMapping
    public ResponseEntity<SignInChildResponse> signIn(
            @Valid @RequestBody SignInChildRequest request) {
        ChildSession session = childSignInService.signIn(request.childId(), request.pin());
        return ResponseEntity.status(HttpStatus.CREATED).body(new SignInChildResponse(
                session.getId(),
                session.getChildId(),
                session.getRole(),
                session.getExpiresAt()
        ));
    }

    /**
     * Signs the child out by revoking the current session (UC-002 main flow).
     *
     * @param sessionToken The session UUID from the {@code X-Numnia-Session} header
     * @return 204 No Content
     */
    @DeleteMapping("/current")
    public ResponseEntity<Void> signOut(
            @RequestHeader(value = "X-Numnia-Session", required = false) UUID sessionToken) {
        if (sessionToken != null) {
            childSignInService.signOut(sessionToken);
        }
        return ResponseEntity.noContent().build();
    }
}
