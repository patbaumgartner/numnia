package ch.numnia.avatar.domain;

/**
 * Cosmetic shop item (UC-007, FR-CRE-006, FR-GAM-003).
 *
 * <p>BR-002: prices are transparent and fixed; the price is part of the
 * catalogue definition and is never mutated at runtime.
 *
 * @param id           stable slug used as REST path parameter (e.g. "star-cap")
 * @param displayName  Swiss High German label with umlauts, no sharp s
 * @param priceStarPoints fixed price in star points (must be {@code &gt; 0})
 * @param slot         avatar slot the item occupies (e.g. "head", "body")
 */
public record ShopItem(
        String id,
        String displayName,
        int priceStarPoints,
        String slot) {

    public ShopItem {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("item id must not be blank");
        }
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("item displayName must not be blank");
        }
        if (displayName.contains("ß")) {
            throw new IllegalArgumentException("displayName must not contain sharp s");
        }
        if (priceStarPoints <= 0) {
            throw new IllegalArgumentException("item price must be positive");
        }
        if (slot == null || slot.isBlank()) {
            throw new IllegalArgumentException("slot must not be blank");
        }
    }
}
