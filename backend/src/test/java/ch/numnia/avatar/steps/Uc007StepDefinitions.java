package ch.numnia.avatar.steps;

import ch.numnia.avatar.domain.ShopAuditAction;
import ch.numnia.avatar.domain.ShopItem;
import ch.numnia.avatar.infra.StaticShopItemCatalog;
import ch.numnia.avatar.service.AvatarService;
import ch.numnia.avatar.service.DuplicatePurchaseException;
import ch.numnia.avatar.service.InsufficientStarPointsException;
import ch.numnia.avatar.service.ItemNotOwnedException;
import ch.numnia.avatar.spi.InventoryRepository;
import ch.numnia.avatar.spi.ShopAuditRepository;
import ch.numnia.avatar.spi.ShopItemCatalog;
import ch.numnia.learning.spi.StarPointsRepository;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cucumber step definitions for UC-007 — avatar customization and shop.
 *
 * <p>The shared "Given an active child session" step is contributed by
 * {@code Uc002StepDefinitions}.
 */
public class Uc007StepDefinitions {

    @Autowired private AvatarService avatarService;
    @Autowired private ShopItemCatalog shopCatalog;
    @Autowired private InventoryRepository inventory;
    @Autowired private StarPointsRepository starPoints;
    @Autowired private ShopAuditRepository shopAudit;
    @Autowired private ch.numnia.test.TestScenarioContext scenarioContext;

    private ShopItem currentItem;
    private Throwable lastError;

    @Before
    public void resetState() {
        currentItem = null;
        lastError = null;
    }

    private UUID childId() {
        UUID id = scenarioContext.childId();
        if (id == null) {
            id = UUID.randomUUID();
            scenarioContext.setChildId(id);
        }
        return id;
    }

    @And("a configured shop catalog")
    public void aConfiguredShopCatalog() {
        assertThat(shopCatalog.listAll()).isNotEmpty();
    }

    @And("the item {string} costs {int} star points")
    public void theNamedItemCosts(String displayName, int price) {
        currentItem = shopCatalog.findById(StaticShopItemCatalog.STAR_CAP_ID).orElseThrow();
        assertThat(currentItem.displayName()).isEqualTo("Sternenmuetze");
        assertThat(currentItem.priceStarPoints()).isEqualTo(price);
        assertThat(displayName).isEqualTo("Star Cap");
    }

    @And("the item costs {int} star points")
    public void theItemCosts(int price) {
        currentItem = shopCatalog.findById(StaticShopItemCatalog.STAR_CAP_ID).orElseThrow();
        assertThat(currentItem.priceStarPoints()).isEqualTo(price);
    }

    @When("the child confirms the purchase")
    public void theChildConfirmsPurchase() {
        avatarService.purchase(childId(), currentItem.id());
    }

    @Then("the star points balance is reduced to {int}")
    public void starPointsBalanceIsReducedTo(int expected) {
        assertThat(starPoints.balanceOf(childId())).isEqualTo(expected);
    }

    @And("the Star Cap is permanently in the inventory")
    public void starCapIsPermanentlyInInventory() {
        assertThat(inventory.ownedItemIds(childId()))
                .contains(StaticShopItemCatalog.STAR_CAP_ID);
        assertThat(avatarService.getInventory(childId()))
                .extracting(e -> e.itemId())
                .contains(StaticShopItemCatalog.STAR_CAP_ID);
        assertThat(shopAudit.actionsFor(childId()))
                .contains(ShopAuditAction.ITEM_PURCHASED);
    }

    @When("the child tries the purchase")
    public void theChildTriesThePurchase() {
        try {
            avatarService.purchase(childId(), currentItem.id());
        } catch (Throwable t) {
            lastError = t;
        }
    }

    @Then("the system shows a notice about collecting more star points")
    public void systemShowsCollectMoreNotice() {
        assertThat(lastError).isInstanceOf(InsufficientStarPointsException.class);
        InsufficientStarPointsException ex = (InsufficientStarPointsException) lastError;
        assertThat(ex.balance()).isLessThan(ex.required());
    }

    @And("no booking takes place")
    public void noBookingTakesPlace() {
        assertThat(inventory.ownedItemIds(childId())).isEmpty();
        assertThat(shopAudit.actionsFor(childId()))
                .containsOnly(ShopAuditAction.PURCHASE_REJECTED_INSUFFICIENT_FUNDS);
    }

    // ── Scenario 3: tamper attempt ──────────────────────────────────────────

    @When("the client tries to unlock an item without payment")
    public void clientTriesToUnlockWithoutPayment() {
        try {
            avatarService.equip(childId(), StaticShopItemCatalog.STAR_CAP_ID);
        } catch (Throwable t) {
            lastError = t;
        }
    }

    @Then("the server responds with an error status")
    public void serverRespondsWithErrorStatus() {
        assertThat(lastError).isInstanceOfAny(
                ItemNotOwnedException.class,
                DuplicatePurchaseException.class,
                InsufficientStarPointsException.class);
    }

    @And("the incident is documented in the shop audit log")
    public void incidentIsDocumentedInShopAuditLog() {
        assertThat(shopAudit.actionsFor(childId()))
                .contains(ShopAuditAction.INVENTORY_TAMPER_REJECTED);
    }
}
