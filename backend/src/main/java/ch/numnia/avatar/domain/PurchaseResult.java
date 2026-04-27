package ch.numnia.avatar.domain;

/**
 * Outcome of a successful purchase (UC-007 main flow step 5).
 */
public record PurchaseResult(String itemId, int starPointsBalance) {
}
