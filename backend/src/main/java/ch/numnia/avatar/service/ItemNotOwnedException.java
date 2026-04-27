package ch.numnia.avatar.service;

public class ItemNotOwnedException extends RuntimeException {

    private final String itemId;

    public ItemNotOwnedException(String itemId) {
        super("item not owned: " + itemId);
        this.itemId = itemId;
    }

    public String itemId() { return itemId; }
}
