package ch.numnia.progress.api;

import ch.numnia.progress.domain.ColorPalette;
import ch.numnia.progress.domain.OperationProgress;
import ch.numnia.progress.domain.ProgressOverview;
import ch.numnia.progress.service.ProgressService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for the child's own learning progress (UC-008).
 *
 * <p>Auth note: {@code X-Child-Id} placeholder header until UC-009 wires
 * Spring Security at the HTTP layer. BR-001 is enforced server-side by
 * scoping all queries to the supplied child id.
 */
@RestController
@RequestMapping("/api/progress")
public class ProgressController {

    private final ProgressService progressService;

    public ProgressController(ProgressService progressService) {
        this.progressService = progressService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getProgress(@RequestHeader("X-Child-Id") UUID childId) {
        ProgressOverview overview = progressService.getOverview(childId);
        return ResponseEntity.ok(toResponse(overview));
    }

    @PutMapping("/preferences/palette")
    public ResponseEntity<Map<String, Object>> setPalette(
            @RequestHeader("X-Child-Id") UUID childId,
            @RequestBody PaletteRequest req) {
        progressService.setPalette(childId, req.palette());
        return ResponseEntity.ok(Map.of("palette", req.palette().name()));
    }

    private static Map<String, Object> toResponse(ProgressOverview overview) {
        return Map.of(
                "childId", overview.childId(),
                "palette", overview.palette().name(),
                "empty", overview.empty(),
                "entries", overview.entries().stream().map(ProgressController::toResponse).toList());
    }

    private static Map<String, Object> toResponse(OperationProgress entry) {
        return Map.of(
                "operation", entry.operation().name(),
                "totalSessions", entry.totalSessions(),
                "totalTasks", entry.totalTasks(),
                "correctTasks", entry.correctTasks(),
                "accuracy", entry.accuracy(),
                "masteryStatus", entry.masteryStatus().name(),
                "currentDifficulty", entry.currentDifficulty());
    }

    public record PaletteRequest(ColorPalette palette) {}
}
