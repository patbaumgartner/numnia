package ch.numnia.parentcontrols.api;

import ch.numnia.parentcontrols.domain.ChildControls;
import ch.numnia.parentcontrols.service.NoLimitConfirmationRequiredException;
import ch.numnia.parentcontrols.service.ParentControlsService;
import ch.numnia.parentcontrols.service.UnauthorizedControlsAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * UC-009 — Parent self-service for play-time and risk mechanic controls.
 *
 * <p>Auth note: the {@code X-Parent-Id} header is the placeholder used
 * across the parent area until UC-009/UC-001 finishes wiring Spring
 * Security; it is treated as authoritative for tests only and is rejected
 * if it does not match the child's owning parent (BR-001 server-side
 * enforcement, NFR-SEC-003).
 */
@RestController
@RequestMapping("/api/parents/me/children")
public class ParentControlsController {

    private final ParentControlsService service;

    public ParentControlsController(ParentControlsService service) {
        this.service = service;
    }

    @GetMapping("/{childId}/controls")
    public ChildControls get(@RequestHeader("X-Parent-Id") UUID parentId,
                             @PathVariable UUID childId) {
        return service.getOrDefault(parentId, childId);
    }

    @PutMapping("/{childId}/controls")
    public ChildControls update(@RequestHeader("X-Parent-Id") UUID parentId,
                                @PathVariable UUID childId,
                                @RequestBody UpdateRequest body) {
        return service.updateControls(parentId, childId,
                body.dailyLimitMinutes,
                body.breakRecommendationMinutes,
                body.riskMechanicEnabled,
                body.confirmNoLimit);
    }

    @ExceptionHandler(UnauthorizedControlsAccessException.class)
    public ResponseEntity<Map<String, String>> onUnauthorized(UnauthorizedControlsAccessException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "FORBIDDEN", "message", e.getMessage()));
    }

    @ExceptionHandler(NoLimitConfirmationRequiredException.class)
    public ResponseEntity<Map<String, String>> onNoLimit(NoLimitConfirmationRequiredException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "NO_LIMIT_CONFIRMATION_REQUIRED", "message", e.getMessage()));
    }

    public record UpdateRequest(
            Integer dailyLimitMinutes,
            int breakRecommendationMinutes,
            boolean riskMechanicEnabled,
            boolean confirmNoLimit) {
    }
}
