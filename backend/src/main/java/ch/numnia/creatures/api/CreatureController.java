package ch.numnia.creatures.api;

import ch.numnia.creatures.domain.Creature;
import ch.numnia.creatures.domain.CreatureUnlockResult;
import ch.numnia.creatures.domain.GalleryEntry;
import ch.numnia.creatures.service.CompanionNotUnlockedException;
import ch.numnia.creatures.service.CreatureService;
import ch.numnia.creatures.service.UnknownCreatureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for UC-006 — creatures gallery, unlock processing,
 * companion selection.
 *
 * <p>Auth note: child-session enforcement will move behind Spring Security
 * with UC-009. The {@code X-Child-Id} header is treated as authoritative
 * for now (matches UC-003/UC-005 placeholder approach).
 */
@RestController
@RequestMapping("/api/creatures")
public class CreatureController {

    private final CreatureService service;

    public CreatureController(CreatureService service) {
        this.service = service;
    }

    @GetMapping
    public Map<String, Object> gallery(@RequestHeader("X-Child-Id") UUID childId) {
        List<Map<String, Object>> entries = service.listGallery(childId).stream()
                .map(CreatureController::toEntryResponse)
                .toList();
        Map<String, Object> body = new HashMap<>();
        body.put("entries", entries);
        body.put("companion", service.currentCompanion(childId).orElse(null));
        return body;
    }

    @PostMapping("/unlocks")
    public Map<String, Object> processUnlocks(@RequestHeader("X-Child-Id") UUID childId) {
        CreatureUnlockResult result = service.processUnlocks(childId);
        Map<String, Object> body = new HashMap<>();
        body.put("newlyUnlocked", result.newlyUnlocked().stream()
                .map(CreatureController::toCreatureResponse)
                .toList());
        body.put("consolationAwarded", result.consolationAwarded());
        body.put("starPointsAwarded", result.starPointsAwarded());
        return body;
    }

    @PostMapping("/{creatureId}/companion")
    public ResponseEntity<Map<String, Object>> pickCompanion(
            @RequestHeader("X-Child-Id") UUID childId,
            @PathVariable String creatureId) {
        service.pickCompanion(childId, creatureId);
        Map<String, Object> body = new HashMap<>();
        body.put("companion", creatureId);
        return ResponseEntity.ok(body);
    }

    @ExceptionHandler(CompanionNotUnlockedException.class)
    public ResponseEntity<Map<String, Object>> handleNotUnlocked(CompanionNotUnlockedException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of(
                        "error", "CREATURE_NOT_UNLOCKED",
                        "message", ex.getMessage(),
                        "creatureId", ex.creatureId()));
    }

    @ExceptionHandler(UnknownCreatureException.class)
    public ResponseEntity<Map<String, Object>> handleUnknown(UnknownCreatureException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "UNKNOWN_CREATURE", "message", ex.getMessage()));
    }

    private static Map<String, Object> toCreatureResponse(Creature c) {
        return Map.of(
                "id", c.id(),
                "displayName", c.displayName(),
                "operation", c.operation().name(),
                "sourceWorldId", c.sourceWorldId());
    }

    private static Map<String, Object> toEntryResponse(GalleryEntry e) {
        Map<String, Object> body = new HashMap<>();
        body.putAll(toCreatureResponse(e.creature()));
        body.put("unlocked", e.unlocked());
        body.put("isCompanion", e.isCompanion());
        return body;
    }
}
