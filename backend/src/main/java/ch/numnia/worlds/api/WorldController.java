package ch.numnia.worlds.api;

import ch.numnia.worlds.domain.PortalEntry;
import ch.numnia.worlds.domain.PortalType;
import ch.numnia.worlds.domain.World;
import ch.numnia.worlds.service.UnknownWorldException;
import ch.numnia.worlds.service.WorldService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for the world map and portal entry (UC-005).
 *
 * <p>Auth note: child-session enforcement will move behind Spring Security
 * with UC-009. The {@code X-Child-Id} header is treated as authoritative for
 * now (matches UC-003 placeholder approach).
 */
@RestController
@RequestMapping("/api/worlds")
public class WorldController {

    private final WorldService worldService;

    public WorldController(WorldService worldService) {
        this.worldService = worldService;
    }

    @GetMapping
    public List<Map<String, Object>> listWorlds() {
        return worldService.listWorlds().stream()
                .map(WorldController::toResponse)
                .toList();
    }

    @PostMapping("/{worldId}/portals/{portalType}/enter")
    public ResponseEntity<Map<String, Object>> enterPortal(
            @RequestHeader("X-Child-Id") UUID childId,
            @PathVariable String worldId,
            @PathVariable PortalType portalType) {
        PortalEntry entry = worldService.openPortal(childId, worldId, portalType);
        return ResponseEntity.ok(toResponse(entry));
    }

    @ExceptionHandler(UnknownWorldException.class)
    public ResponseEntity<Map<String, Object>> handleUnknownWorld(UnknownWorldException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "UNKNOWN_WORLD", "message", ex.getMessage()));
    }

    private static Map<String, Object> toResponse(World world) {
        return Map.of(
                "id", world.id(),
                "displayName", world.displayName(),
                "difficultyLevel", world.difficultyLevel(),
                "requiredLevel", world.requiredLevel());
    }

    private static Map<String, Object> toResponse(PortalEntry entry) {
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("worldId", entry.worldId());
        body.put("portalType", entry.portalType());
        body.put("locked", entry.locked());
        body.put("target", entry.target());
        body.put("messageCode", entry.messageCode());
        body.put("reducedMotion", entry.reducedMotion());
        return body;
    }
}
