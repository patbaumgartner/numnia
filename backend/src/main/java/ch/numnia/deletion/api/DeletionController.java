package ch.numnia.deletion.api;

import ch.numnia.deletion.domain.DeletionRequest;
import ch.numnia.deletion.service.DeletionLinkUnavailableException;
import ch.numnia.deletion.service.DeletionService;
import ch.numnia.deletion.service.DeletionService.DeletionRecord;
import ch.numnia.deletion.service.InvalidConfirmationWordException;
import ch.numnia.deletion.service.InvalidPasswordException;
import ch.numnia.deletion.service.UnauthorizedDeletionAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * UC-011 — Parent self-service child-account deletion endpoint.
 *
 * <p>Auth: parent identification via {@code X-Parent-Id} header until
 * Spring Security finalises parent-session enforcement. Server-side
 * ownership and password checks are performed in {@link DeletionService}.
 */
@RestController
@RequestMapping("/api/parents/me")
public class DeletionController {

    private final DeletionService service;

    public DeletionController(DeletionService service) {
        this.service = service;
    }

    /** Trigger a deletion (cool-off mail). Returns the signed-link summary. */
    @PostMapping("/children/{childId}/deletion")
    public ResponseEntity<DeletionRequestSummary> trigger(
            @RequestHeader("X-Parent-Id") UUID parentId,
            @PathVariable UUID childId,
            @RequestBody DeletionTriggerRequest body) {
        String password = body != null ? body.password : null;
        String confirmationWord = body != null ? body.confirmationWord : null;
        DeletionRequest request = service.requestDeletion(
                parentId, childId, password, confirmationWord);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(DeletionRequestSummary.from(request));
    }

    /** Confirm a previously triggered deletion via the cool-off token. */
    @PostMapping("/deletions/{token}/confirm")
    public ResponseEntity<DeletionRecordResponse> confirm(
            @RequestHeader("X-Parent-Id") UUID parentId,
            @PathVariable String token) {
        DeletionRecord record = service.confirmDeletion(token);
        return ResponseEntity.ok(DeletionRecordResponse.from(record));
    }

    @ExceptionHandler(UnauthorizedDeletionAccessException.class)
    public ResponseEntity<Map<String, String>> onUnauthorized(UnauthorizedDeletionAccessException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "FORBIDDEN", "message", e.getMessage()));
    }

    @ExceptionHandler(InvalidPasswordException.class)
    public ResponseEntity<Map<String, String>> onInvalidPassword(InvalidPasswordException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "INVALID_PASSWORD", "message", e.getMessage()));
    }

    @ExceptionHandler(InvalidConfirmationWordException.class)
    public ResponseEntity<Map<String, String>> onInvalidConfirmation(InvalidConfirmationWordException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "INVALID_CONFIRMATION_WORD", "message", e.getMessage()));
    }

    @ExceptionHandler(DeletionLinkUnavailableException.class)
    public ResponseEntity<Map<String, String>> onUnavailable(DeletionLinkUnavailableException e) {
        return ResponseEntity.status(HttpStatus.GONE)
                .body(Map.of("error", "DELETION_LINK_UNAVAILABLE", "message", e.getMessage()));
    }

    public record DeletionTriggerRequest(String password, String confirmationWord) {
        // Lombok-style fields exposed for binding.
        public DeletionTriggerRequest { }
    }

    public record DeletionRequestSummary(
            UUID id,
            String token,
            String signedUrlPath,
            Instant expiresAt,
            String status) {

        public static DeletionRequestSummary from(DeletionRequest r) {
            return new DeletionRequestSummary(
                    r.id(),
                    r.token(),
                    "/api/parents/me/deletions/" + r.token() + "/confirm",
                    r.expiresAt(),
                    r.status().name());
        }
    }

    public record DeletionRecordResponse(
            UUID id,
            String childPseudonym,
            Instant completedAt,
            List<String> dataCategories) {

        public static DeletionRecordResponse from(DeletionRecord r) {
            Set<String> cats = r.dataCategories();
            return new DeletionRecordResponse(
                    r.id(),
                    r.childPseudonym(),
                    r.completedAt(),
                    cats == null ? List.of() : List.copyOf(cats));
        }
    }
}
