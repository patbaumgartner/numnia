/**
 * ShopPage — UC-007 child buys cosmetic items with star points.
 *
 * Renders the shop catalog with transparent fixed prices. The child can
 * purchase if enough star points are available; the backend deducts the
 * price atomically and adds the item permanently to the inventory
 * (BR-002, BR-003). On insufficient funds, a Swiss High German notice is
 * shown and no booking takes place (alt 4a).
 *
 * UI copy is Swiss High German with umlauts, without sharp s
 * (NFR-I18N-002, NFR-I18N-004).
 */
import { useEffect, useState } from 'react';
import { ApiError, getAvatar, listShopItems, purchaseShopItem } from '../api/client';
import type { AvatarResponse, ShopItemResponse } from '../api/types';

export default function ShopPage() {
  const childId = sessionStorage.getItem('numnia_child_id') ?? '';
  const [items, setItems] = useState<ShopItemResponse[]>([]);
  const [avatar, setAvatar] = useState<AvatarResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [actionMessage, setActionMessage] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  useEffect(() => {
    if (!childId) {
      setLoading(false);
      return;
    }
    let cancelled = false;
    (async () => {
      try {
        const [shop, av] = await Promise.all([
          listShopItems(),
          getAvatar(childId),
        ]);
        if (!cancelled) {
          setItems(shop.items);
          setAvatar(av);
          setLoading(false);
        }
      } catch {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [childId]);

  if (!childId) {
    return (
      <main>
        <h2>Shop</h2>
        <p>Bitte zuerst anmelden.</p>
      </main>
    );
  }

  async function handlePurchase(itemId: string) {
    setActionError(null);
    setActionMessage(null);
    try {
      const result = await purchaseShopItem(childId, itemId);
      setActionMessage('Glueckwunsch! Der Gegenstand ist nun in deinem Inventar.');
      setAvatar((prev) =>
        prev ? { ...prev, starPointsBalance: result.starPointsBalance } : prev,
      );
    } catch (err) {
      if (err instanceof ApiError && err.code === 'INSUFFICIENT_STAR_POINTS') {
        setActionError(
          'Sammle noch mehr Sternenpunkte, um diesen Gegenstand zu kaufen.',
        );
      } else if (err instanceof ApiError && err.code === 'ALREADY_IN_INVENTORY') {
        setActionError('Dieser Gegenstand ist bereits in deinem Inventar.');
      } else {
        setActionError('Der Kauf konnte nicht abgeschlossen werden.');
      }
    }
  }

  return (
    <main data-testid="shop">
      <h2>Avatar-Shop</h2>
      <p>Hier kannst du Sachen fuer deinen Avatar kaufen.</p>

      {avatar && (
        <p data-testid="balance">
          Du hast {avatar.starPointsBalance} Sternenpunkte.
        </p>
      )}

      {loading && <p>Shop wird geladen…</p>}

      {actionMessage && (
        <p role="status" data-testid="purchase-success">
          {actionMessage}
        </p>
      )}
      {actionError && (
        <p role="alert" data-testid="purchase-error">
          {actionError}
        </p>
      )}

      <ul data-testid="shop-list">
        {items.map((item) => (
          <li key={item.id} data-testid={`item-${item.id}`}>
            <h3>{item.displayName}</h3>
            <p data-testid={`price-${item.id}`}>
              {item.priceStarPoints} Sternenpunkte
            </p>
            <button
              type="button"
              data-testid={`buy-${item.id}`}
              onClick={() => handlePurchase(item.id)}
            >
              Jetzt kaufen
            </button>
          </li>
        ))}
      </ul>
    </main>
  );
}
