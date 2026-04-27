package ch.numnia.avatar.service;

/** Star points balance is not enough to cover the requested item (UC-007 alt 4a). */
public class InsufficientStarPointsException extends RuntimeException {

    private final String itemId;
    private final int balance;
    private final int required;

    public InsufficientStarPointsException(String itemId, int balance, int required) {
        super("not enough star points for item " + itemId
                + " (balance=" + balance + ", required=" + required + ")");
        this.itemId = itemId;
        this.balance = balance;
        this.required = required;
    }

    public String itemId() { return itemId; }
    public int balance() { return balance; }
    public int required() { return required; }
}
