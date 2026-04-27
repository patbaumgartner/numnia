package ch.numnia.avatar.service;

import ch.numnia.avatar.domain.*;
import ch.numnia.avatar.spi.AvatarConfigurationRepository;
import ch.numnia.avatar.spi.InventoryRepository;
import ch.numnia.avatar.spi.ShopAuditRepository;
import ch.numnia.avatar.spi.ShopItemCatalog;
import ch.numnia.iam.domain.ChildProfile;
import ch.numnia.iam.spi.ChildProfileRepository;
import ch.numnia.learning.spi.StarPointsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Orchestrator for UC-007 — avatar customization and shop purchases.
 *
 * <p>Server-side rules (no client trust, FR-GAM-002, exception 5y):
 * <ul>
 *   <li>BR-001: star points are only ever debited via gameplay rewards
 *       and shop purchases — they cannot be set or topped up by the API.</li>
 *   <li>BR-002: prices are read from the catalogue and never overridden.</li>
 *   <li>BR-003: items are permanent (never removed from inventory) and
 *       cannot be lost through gameplay errors (FR-GAM-005).</li>
 *   <li>BR-004: avatar base models are validated against the
 *       gender-neutral catalogue.</li>
 * </ul>
 *
 * <p>Audit-only logging (no PII; child identification by UUID only).
 */
@Service
public class AvatarService {

    private static final Logger log = LoggerFactory.getLogger(AvatarService.class);

    private final ShopItemCatalog catalog;
    private final InventoryRepository inventory;
    private final AvatarConfigurationRepository configs;
    private final ShopAuditRepository audit;
    private final StarPointsRepository starPoints;
    private final ChildProfileRepository childProfiles;
    private final Set<String> avatarBaseModelCatalog;
    private final Clock clock;

    @Autowired
    public AvatarService(ShopItemCatalog catalog,
                         InventoryRepository inventory,
                         AvatarConfigurationRepository configs,
                         ShopAuditRepository audit,
                         StarPointsRepository starPoints,
                         ChildProfileRepository childProfiles,
                         @Qualifier("avatarBaseModelCatalog") Set<String> avatarBaseModelCatalog) {
        this(catalog, inventory, configs, audit, starPoints, childProfiles,
                avatarBaseModelCatalog, Clock.systemUTC());
    }

    public AvatarService(ShopItemCatalog catalog,
                         InventoryRepository inventory,
                         AvatarConfigurationRepository configs,
                         ShopAuditRepository audit,
                         StarPointsRepository starPoints,
                         ChildProfileRepository childProfiles,
                         Set<String> avatarBaseModelCatalog,
                         Clock clock) {
        this.catalog = catalog;
        this.inventory = inventory;
        this.configs = configs;
        this.audit = audit;
        this.starPoints = starPoints;
        this.childProfiles = childProfiles;
        this.avatarBaseModelCatalog = avatarBaseModelCatalog;
        this.clock = clock;
    }

    /** UC-007 step 1: snapshot current avatar (base model + equipped items). */
    public AvatarConfiguration getAvatar(UUID childId) {
        requireChildId(childId);
        ChildProfile profile = childProfiles.findById(childId)
                .orElseThrow(() -> new IllegalArgumentException("unknown child: " + childId));
        return new AvatarConfiguration(
                childId, profile.getAvatarBaseModel(), configs.equippedFor(childId));
    }

    /** UC-007 step 2: change to another gender-neutral base model (BR-004). */
    @Transactional
    public AvatarConfiguration setBaseModel(UUID childId, String baseModel) {
        requireChildId(childId);
        if (baseModel == null || !avatarBaseModelCatalog.contains(baseModel)) {
            throw new UnknownAvatarBaseModelException(baseModel);
        }
        ChildProfile profile = childProfiles.findById(childId)
                .orElseThrow(() -> new IllegalArgumentException("unknown child: " + childId));
        profile.setAvatarBaseModel(baseModel);
        childProfiles.save(profile);
        audit.append(new ShopAuditEntry(
                childId, ShopAuditAction.AVATAR_BASE_MODEL_CHANGED, baseModel, clock.instant()));
        return new AvatarConfiguration(childId, baseModel, configs.equippedFor(childId));
    }

    /** UC-007 step 3: list shop items with transparent fixed prices. */
    public List<ShopItem> listShop() {
        return catalog.listAll();
    }

    /**
     * UC-007 step 4 / 5: buy an item with star points.
     *
     * <p>Failure modes:
     * <ul>
     *   <li>{@link UnknownShopItemException} (404) — item id not in catalogue.</li>
     *   <li>{@link DuplicatePurchaseException} (409) — item already owned (5x).</li>
     *   <li>{@link InsufficientStarPointsException} (409) — not enough stars (4a).</li>
     * </ul>
     */
    @Transactional
    public PurchaseResult purchase(UUID childId, String itemId) {
        requireChildId(childId);
        ShopItem item = catalog.findById(itemId)
                .orElseThrow(() -> new UnknownShopItemException(itemId));
        if (inventory.ownedItemIds(childId).contains(item.id())) {
            audit.append(new ShopAuditEntry(
                    childId, ShopAuditAction.PURCHASE_REJECTED_DUPLICATE,
                    item.id(), clock.instant()));
            throw new DuplicatePurchaseException(item.id());
        }
        int balance = starPoints.balanceOf(childId);
        if (balance < item.priceStarPoints()) {
            audit.append(new ShopAuditEntry(
                    childId, ShopAuditAction.PURCHASE_REJECTED_INSUFFICIENT_FUNDS,
                    item.id(), clock.instant()));
            throw new InsufficientStarPointsException(item.id(), balance, item.priceStarPoints());
        }
        int newBalance = starPoints.addPoints(childId, -item.priceStarPoints());
        inventory.recordPurchase(new InventoryEntry(childId, item.id(), clock.instant()));
        audit.append(new ShopAuditEntry(
                childId, ShopAuditAction.ITEM_PURCHASED, item.id(), clock.instant()));
        log.info("child={} purchased item={} balance={}", childId, item.id(), newBalance);
        return new PurchaseResult(item.id(), newBalance);
    }

    /** UC-007 step 6: equip a previously purchased item. */
    @Transactional
    public AvatarConfiguration equip(UUID childId, String itemId) {
        requireChildId(childId);
        ShopItem item = catalog.findById(itemId)
                .orElseThrow(() -> new UnknownShopItemException(itemId));
        if (!inventory.ownedItemIds(childId).contains(item.id())) {
            audit.append(new ShopAuditEntry(
                    childId, ShopAuditAction.INVENTORY_TAMPER_REJECTED,
                    item.id(), clock.instant()));
            throw new ItemNotOwnedException(item.id());
        }
        configs.equip(childId, item.slot(), item.id());
        audit.append(new ShopAuditEntry(
                childId, ShopAuditAction.ITEM_EQUIPPED, item.id(), clock.instant()));
        return getAvatar(childId);
    }

    public List<InventoryEntry> getInventory(UUID childId) {
        requireChildId(childId);
        return inventory.entriesFor(childId);
    }

    private static void requireChildId(UUID childId) {
        if (childId == null) {
            throw new IllegalArgumentException("childId must not be null");
        }
    }
}
