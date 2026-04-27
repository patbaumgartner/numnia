package ch.numnia.avatar.spi;

import ch.numnia.avatar.domain.ShopItem;

import java.util.List;
import java.util.Optional;

/** Catalogue of cosmetic shop items (UC-007, FR-GAM-003). */
public interface ShopItemCatalog {

    List<ShopItem> listAll();

    Optional<ShopItem> findById(String id);
}
