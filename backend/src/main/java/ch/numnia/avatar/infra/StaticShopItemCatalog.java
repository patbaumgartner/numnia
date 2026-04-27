package ch.numnia.avatar.infra;

import ch.numnia.avatar.domain.ShopItem;
import ch.numnia.avatar.spi.ShopItemCatalog;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Static shop catalogue for Release 1 (UC-007).
 *
 * <p>Prices are transparent and fixed (BR-002 / FR-GAM-003). The catalogue
 * will move to YAML once the content authoring pipeline lands (follow-up
 * tracked in {@code .ralph/usecase-progress.md}).
 */
@Component
public class StaticShopItemCatalog implements ShopItemCatalog {

    public static final String STAR_CAP_ID = "star-cap";
    public static final String MOON_CAPE_ID = "moon-cape";
    public static final String SUN_GLASSES_ID = "sun-glasses";

    private final List<ShopItem> items = List.of(
            new ShopItem(STAR_CAP_ID, "Sternenmuetze", 30, "head"),
            new ShopItem(MOON_CAPE_ID, "Mondumhang", 50, "body"),
            new ShopItem(SUN_GLASSES_ID, "Sonnenbrille", 15, "accessory"));

    @Override
    public List<ShopItem> listAll() {
        return items;
    }

    @Override
    public Optional<ShopItem> findById(String id) {
        return items.stream().filter(i -> i.id().equals(id)).findFirst();
    }
}
