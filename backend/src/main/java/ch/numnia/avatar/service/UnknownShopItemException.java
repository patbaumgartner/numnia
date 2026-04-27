package ch.numnia.avatar.service;

public class UnknownShopItemException extends RuntimeException {

    private final String itemId;

    public UnknownShopItemException(String itemId) {
        super("unknown shop item: " + itemId);
        this.itemId = itemId;
    }

    public String itemId() { return itemId; }
}
