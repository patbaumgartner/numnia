package ch.numnia.avatar.service;

import ch.numnia.avatar.domain.*;
import ch.numnia.avatar.infra.*;
import ch.numnia.avatar.spi.*;
import ch.numnia.iam.domain.ChildProfile;
import ch.numnia.iam.spi.ChildProfileRepository;
import ch.numnia.learning.infra.InMemoryStarPointsRepository;
import ch.numnia.learning.spi.StarPointsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static ch.numnia.avatar.infra.StaticShopItemCatalog.STAR_CAP_ID;
import static ch.numnia.avatar.infra.StaticShopItemCatalog.SUN_GLASSES_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AvatarService} (UC-007).
 *
 * <p>Tests are paired Red/Green per behaviour. Every business rule has at
 * least one happy- and one failure-path test; every audit branch is asserted.
 */
@ExtendWith(MockitoExtension.class)
class AvatarServiceTest {

    private static final Set<String> AVATAR_BASE_MODEL_CATALOG =
            Set.of("star", "moon", "sun", "cloud", "wave", "rock", "fire", "wind");

    @Mock
    private ChildProfileRepository childProfiles;

    private ShopItemCatalog catalog;
    private InventoryRepository inventory;
    private AvatarConfigurationRepository configs;
    private ShopAuditRepository audit;
    private StarPointsRepository starPoints;
    private AvatarService service;
    private UUID childId;
    private ChildProfile childProfile;
    private final Clock fixedClock = Clock.fixed(Instant.parse("2026-04-27T19:00:00Z"), ZoneOffset.UTC);

    @BeforeEach
    void setUp() {
        catalog = new StaticShopItemCatalog();
        inventory = new InMemoryInventoryRepository();
        configs = new InMemoryAvatarConfigurationRepository();
        audit = new InMemoryShopAuditRepository();
        starPoints = new InMemoryStarPointsRepository();
        childId = UUID.randomUUID();
        childProfile = new ChildProfile(childId, "Astra", 2017, "star", UUID.randomUUID());
        lenient().when(childProfiles.findById(childId)).thenReturn(Optional.of(childProfile));
        lenient().when(childProfiles.save(childProfile)).thenReturn(childProfile);

        service = new AvatarService(catalog, inventory, configs, audit, starPoints,
                childProfiles, AVATAR_BASE_MODEL_CATALOG, fixedClock);
    }

    // ── BR-002: prices are transparent and fixed ─────────────────────────────

    @Test
    void listShop_returnsCatalogueWithFixedPrices_BR002() {
        assertThat(service.listShop())
                .extracting(ShopItem::id, ShopItem::priceStarPoints)
                .contains(
                        org.assertj.core.api.Assertions.tuple(STAR_CAP_ID, 30),
                        org.assertj.core.api.Assertions.tuple(SUN_GLASSES_ID, 15));
    }

    // ── Successful purchase (UC-007 main scenario) ────────────────────────────

    @Test
    void purchase_withEnoughStarPoints_deductsPriceAndAddsToInventory() {
        starPoints.setBalance(childId, 50);

        PurchaseResult result = service.purchase(childId, STAR_CAP_ID);

        assertThat(result.itemId()).isEqualTo(STAR_CAP_ID);
        assertThat(result.starPointsBalance()).isEqualTo(20);
        assertThat(starPoints.balanceOf(childId)).isEqualTo(20);
        assertThat(inventory.ownedItemIds(childId)).contains(STAR_CAP_ID);
        assertThat(audit.actionsFor(childId)).contains(ShopAuditAction.ITEM_PURCHASED);
    }

    @Test
    void purchase_recordsPermanentInventoryEntryWithClockTime_BR003() {
        starPoints.setBalance(childId, 30);

        service.purchase(childId, STAR_CAP_ID);

        assertThat(inventory.entriesFor(childId))
                .singleElement()
                .satisfies(e -> {
                    assertThat(e.itemId()).isEqualTo(STAR_CAP_ID);
                    assertThat(e.purchasedAt()).isEqualTo(fixedClock.instant());
                });
    }

    // ── 4a: insufficient star points ─────────────────────────────────────────

    @Test
    void purchase_withInsufficientFunds_throwsAndDoesNotChangeBalanceOrInventory() {
        starPoints.setBalance(childId, 10);

        assertThatThrownBy(() -> service.purchase(childId, STAR_CAP_ID))
                .isInstanceOf(InsufficientStarPointsException.class)
                .satisfies(t -> {
                    InsufficientStarPointsException ex = (InsufficientStarPointsException) t;
                    assertThat(ex.balance()).isEqualTo(10);
                    assertThat(ex.required()).isEqualTo(30);
                    assertThat(ex.itemId()).isEqualTo(STAR_CAP_ID);
                });

        assertThat(starPoints.balanceOf(childId)).isEqualTo(10);
        assertThat(inventory.ownedItemIds(childId)).isEmpty();
        assertThat(audit.actionsFor(childId))
                .containsOnly(ShopAuditAction.PURCHASE_REJECTED_INSUFFICIENT_FUNDS);
    }

    // ── 5x: duplicate purchase ───────────────────────────────────────────────

    @Test
    void purchase_sameItemTwice_throwsDuplicateAndDoesNotDeductAgain() {
        starPoints.setBalance(childId, 100);
        service.purchase(childId, STAR_CAP_ID);
        int afterFirst = starPoints.balanceOf(childId);

        assertThatThrownBy(() -> service.purchase(childId, STAR_CAP_ID))
                .isInstanceOf(DuplicatePurchaseException.class);

        assertThat(starPoints.balanceOf(childId)).isEqualTo(afterFirst);
        assertThat(inventory.entriesFor(childId)).hasSize(1);
        assertThat(audit.actionsFor(childId))
                .contains(ShopAuditAction.PURCHASE_REJECTED_DUPLICATE);
    }

    // ── Unknown item ─────────────────────────────────────────────────────────

    @Test
    void purchase_withUnknownItem_throwsUnknownShopItem() {
        starPoints.setBalance(childId, 1000);
        assertThatThrownBy(() -> service.purchase(childId, "phantom"))
                .isInstanceOf(UnknownShopItemException.class);
        assertThat(audit.actionsFor(childId))
                .doesNotContain(ShopAuditAction.ITEM_PURCHASED);
    }

    // ── BR-001: API cannot mint star points ──────────────────────────────────

    @Test
    void avatarServiceApi_doesNotExposeAnyMethodToTopUpStarPoints_BR001() {
        // Sanity check: AvatarService must not expose any add/credit method.
        for (var m : AvatarService.class.getDeclaredMethods()) {
            assertThat(m.getName().toLowerCase())
                    .as("Method %s must not look like a star-points mint", m.getName())
                    .doesNotContain("addstar")
                    .doesNotContain("topup")
                    .doesNotContain("creditstar");
        }
    }

    // ── 5y: tamper attempt — equipping a non-owned item is rejected ──────────

    @Test
    void equip_withItemNotInInventory_isRejectedAndAudited_5y() {
        // No purchase was made.
        assertThatThrownBy(() -> service.equip(childId, STAR_CAP_ID))
                .isInstanceOf(ItemNotOwnedException.class);
        assertThat(audit.actionsFor(childId))
                .contains(ShopAuditAction.INVENTORY_TAMPER_REJECTED);
        assertThat(configs.equippedFor(childId)).isEmpty();
    }

    @Test
    void equip_withOwnedItem_setsEquippedSlotAndAudits() {
        starPoints.setBalance(childId, 50);
        service.purchase(childId, STAR_CAP_ID);

        AvatarConfiguration cfg = service.equip(childId, STAR_CAP_ID);

        assertThat(cfg.equipped()).containsEntry("head", STAR_CAP_ID);
        assertThat(audit.actionsFor(childId)).contains(ShopAuditAction.ITEM_EQUIPPED);
    }

    // ── BR-004: gender-neutral base model catalogue ─────────────────────────

    @Test
    void setBaseModel_withCatalogueModel_updatesProfileAndAudits_BR004() {
        AvatarConfiguration cfg = service.setBaseModel(childId, "moon");

        assertThat(cfg.baseModel()).isEqualTo("moon");
        assertThat(childProfile.getAvatarBaseModel()).isEqualTo("moon");
        assertThat(audit.actionsFor(childId))
                .contains(ShopAuditAction.AVATAR_BASE_MODEL_CHANGED);
    }

    @Test
    void setBaseModel_withUnknownModel_isRejected_BR004() {
        assertThatThrownBy(() -> service.setBaseModel(childId, "warrior"))
                .isInstanceOf(UnknownAvatarBaseModelException.class);
        assertThat(childProfile.getAvatarBaseModel()).isEqualTo("star"); // unchanged
    }

    @Test
    void setBaseModel_withNull_isRejected() {
        assertThatThrownBy(() -> service.setBaseModel(childId, null))
                .isInstanceOf(UnknownAvatarBaseModelException.class);
    }

    // ── getAvatar returns current snapshot ───────────────────────────────────

    @Test
    void getAvatar_returnsBaseModelAndEquippedItems() {
        starPoints.setBalance(childId, 100);
        service.purchase(childId, STAR_CAP_ID);
        service.equip(childId, STAR_CAP_ID);

        AvatarConfiguration cfg = service.getAvatar(childId);

        assertThat(cfg.childId()).isEqualTo(childId);
        assertThat(cfg.baseModel()).isEqualTo("star");
        assertThat(cfg.equipped()).containsEntry("head", STAR_CAP_ID);
    }

    @Test
    void purchase_nullChildId_isRejected() {
        assertThatThrownBy(() -> service.purchase(null, STAR_CAP_ID))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── ShopItem record validation ───────────────────────────────────────────

    @Test
    void shopItem_withSharpS_inDisplayName_isRejected_NFRI18N004() {
        assertThatThrownBy(() -> new ShopItem("x", "Straße", 10, "head"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shopItem_withZeroPrice_isRejected() {
        assertThatThrownBy(() -> new ShopItem("x", "Mütze", 0, "head"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shopItem_withNegativePrice_isRejected() {
        assertThatThrownBy(() -> new ShopItem("x", "Mütze", -5, "head"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void itemPrices_neverContainSharpS_NFRI18N004() {
        for (ShopItem item : catalog.listAll()) {
            assertThat(item.displayName()).doesNotContain("ß");
        }
    }
}
