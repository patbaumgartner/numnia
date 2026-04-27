package ch.numnia.avatar.service;

/** UC-007 exception 5x: the same item is being purchased twice. */
public class DuplicatePurchaseException extends RuntimeException {

    private final String itemId;

    public DuplicatePurchaseException(String itemId) {
        super("item already in inventory: " + itemId);
        this.itemId = itemId;
    }

    public String itemId() { return itemId; }
}
