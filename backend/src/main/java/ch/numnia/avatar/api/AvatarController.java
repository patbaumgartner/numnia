package ch.numnia.avatar.api;

import ch.numnia.avatar.domain.AvatarConfiguration;
import ch.numnia.avatar.domain.InventoryEntry;
import ch.numnia.avatar.domain.PurchaseResult;
import ch.numnia.avatar.domain.ShopItem;
import ch.numnia.avatar.service.*;
import ch.numnia.learning.spi.StarPointsRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for UC-007 — avatar customization and shop.
 *
 * <p>Auth note: child-session enforcement will move behind Spring Security
 * with UC-009. The {@code X-Child-Id} header is treated as authoritative
 * for now (matches UC-003/UC-005/UC-006 placeholder approach).
 */
@RestController
@RequestMapping("/api")
public class AvatarController {

    private final AvatarService avatar;
    private final StarPointsRepository starPoints;

    public AvatarController(AvatarService avatar, StarPointsRepository starPoints) {
        this.avatar = avatar;
        this.starPoints = starPoints;
    }

    @GetMapping("/avatar")
    public Map<String, Object> getAvatar(@RequestHeader("X-Child-Id") UUID childId) {
        AvatarConfiguration cfg = avatar.getAvatar(childId);
        return Map.of(
                "childId", cfg.childId().toString(),
                "baseModel", cfg.baseModel(),
                "equipped", cfg.equipped(),
                "starPointsBalance", starPoints.balanceOf(childId));
    }

    public record SetBaseModelRequest(String baseModel) {}

    @PutMapping("/avatar/base-model")
    public Map<String, Object> setBaseModel(
            @RequestHeader("X-Child-Id") UUID childId,
            @RequestBody SetBaseModelRequest request) {
        AvatarConfiguration cfg = avatar.setBaseModel(childId, request.baseModel());
        return Map.of(
                "baseModel", cfg.baseModel(),
                "equipped", cfg.equipped());
    }

    public record EquipRequest(String itemId) {}

    @PostMapping("/avatar/equipped")
    public Map<String, Object> equip(
            @RequestHeader("X-Child-Id") UUID childId,
            @RequestBody EquipRequest request) {
        AvatarConfiguration cfg = avatar.equip(childId, request.itemId());
        return Map.of(
                "baseModel", cfg.baseModel(),
                "equipped", cfg.equipped());
    }

    @GetMapping("/avatar/inventory")
    public Map<String, Object> inventory(@RequestHeader("X-Child-Id") UUID childId) {
        List<InventoryEntry> entries = avatar.getInventory(childId);
        return Map.of(
                "items", entries.stream()
                        .map(e -> Map.of(
                                "itemId", e.itemId(),
                                "purchasedAt", e.purchasedAt().toString()))
                        .toList());
    }

    @GetMapping("/shop/items")
    public Map<String, Object> shopItems() {
        List<Map<String, Object>> items = avatar.listShop().stream()
                .map(AvatarController::toItemResponse)
                .toList();
        return Map.of("items", items);
    }

    @PostMapping("/shop/items/{itemId}/purchase")
    public Map<String, Object> purchase(
            @RequestHeader("X-Child-Id") UUID childId,
            @PathVariable String itemId) {
        PurchaseResult result = avatar.purchase(childId, itemId);
        return Map.of(
                "itemId", result.itemId(),
                "starPointsBalance", result.starPointsBalance());
    }

    @ExceptionHandler(InsufficientStarPointsException.class)
    public ResponseEntity<Map<String, Object>> handleInsufficient(InsufficientStarPointsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of(
                        "error", "INSUFFICIENT_STAR_POINTS",
                        "message", "Sammle noch mehr Sternenpunkte, um diesen Gegenstand zu kaufen.",
                        "itemId", ex.itemId(),
                        "balance", ex.balance(),
                        "required", ex.required()));
    }

    @ExceptionHandler(DuplicatePurchaseException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicate(DuplicatePurchaseException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of(
                        "error", "ALREADY_IN_INVENTORY",
                        "message", "Dieser Gegenstand ist bereits in deinem Inventar.",
                        "itemId", ex.itemId()));
    }

    @ExceptionHandler(UnknownShopItemException.class)
    public ResponseEntity<Map<String, Object>> handleUnknownItem(UnknownShopItemException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "UNKNOWN_ITEM", "message", ex.getMessage()));
    }

    @ExceptionHandler(UnknownAvatarBaseModelException.class)
    public ResponseEntity<Map<String, Object>> handleUnknownBaseModel(UnknownAvatarBaseModelException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "UNKNOWN_BASE_MODEL", "message", ex.getMessage()));
    }

    @ExceptionHandler(ItemNotOwnedException.class)
    public ResponseEntity<Map<String, Object>> handleNotOwned(ItemNotOwnedException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of(
                        "error", "ITEM_NOT_OWNED",
                        "message", "Dieser Gegenstand gehoert dir noch nicht.",
                        "itemId", ex.itemId()));
    }

    private static Map<String, Object> toItemResponse(ShopItem item) {
        return Map.of(
                "id", item.id(),
                "displayName", item.displayName(),
                "priceStarPoints", item.priceStarPoints(),
                "slot", item.slot());
    }
}
