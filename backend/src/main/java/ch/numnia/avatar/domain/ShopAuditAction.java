package ch.numnia.avatar.domain;

/**
 * Audit-log action types for UC-007.
 *
 * <p>Only the action and the affected item id are persisted; never any PII.
 */
public enum ShopAuditAction {
    ITEM_PURCHASED,
    PURCHASE_REJECTED_INSUFFICIENT_FUNDS,
    PURCHASE_REJECTED_DUPLICATE,
    INVENTORY_TAMPER_REJECTED,
    AVATAR_BASE_MODEL_CHANGED,
    ITEM_EQUIPPED
}
