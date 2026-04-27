/**
 * AvatarPage — UC-007 child customizes its avatar.
 *
 * Renders the current avatar configuration (base model + equipped items),
 * shows the inventory and offers a link to the shop. The child can change
 * the base model from the vetted catalog (FR-CRE-005) and equip any owned
 * item.
 *
 * UI copy is Swiss High German with umlauts, without sharp s
 * (NFR-I18N-002, NFR-I18N-004).
 */
import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  equipAvatarItem,
  getAvatar,
  getInventory,
  setAvatarBaseModel,
} from '../api/client';
import type { AvatarResponse, InventoryItemResponse } from '../api/types';

const BASE_MODELS = [
  'avatar-fox',
  'avatar-owl',
  'avatar-rabbit',
  'avatar-bear',
  'avatar-cat',
  'avatar-otter',
  'avatar-hedgehog',
  'avatar-deer',
];

export default function AvatarPage() {
  const childId = sessionStorage.getItem('numnia_child_id') ?? '';
  const [avatar, setAvatar] = useState<AvatarResponse | null>(null);
  const [inventory, setInventory] = useState<InventoryItemResponse[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!childId) {
      setLoading(false);
      return;
    }
    let cancelled = false;
    (async () => {
      try {
        const [av, inv] = await Promise.all([
          getAvatar(childId),
          getInventory(childId),
        ]);
        if (!cancelled) {
          setAvatar(av);
          setInventory(inv.items);
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
        <h2>Mein Avatar</h2>
        <p>Bitte zuerst anmelden.</p>
      </main>
    );
  }

  async function handleBaseModel(model: string) {
    if (!avatar) return;
    const res = await setAvatarBaseModel(childId, model);
    setAvatar({ ...avatar, baseModel: res.baseModel, equipped: res.equipped });
  }

  async function handleEquip(itemId: string) {
    if (!avatar) return;
    const res = await equipAvatarItem(childId, itemId);
    setAvatar({ ...avatar, baseModel: res.baseModel, equipped: res.equipped });
  }

  return (
    <main data-testid="avatar">
      <h2>Mein Avatar</h2>
      <p>Stell deinen Avatar nach deinem Geschmack zusammen.</p>

      {loading && <p>Avatar wird geladen…</p>}

      {avatar && (
        <>
          <section>
            <h3>Grundfigur</h3>
            <p data-testid="base-model">Aktuell: {avatar.baseModel}</p>
            <ul data-testid="base-model-list">
              {BASE_MODELS.map((m) => (
                <li key={m}>
                  <button
                    type="button"
                    data-testid={`base-${m}`}
                    onClick={() => handleBaseModel(m)}
                  >
                    {m}
                  </button>
                </li>
              ))}
            </ul>
          </section>

          <section>
            <h3>Inventar</h3>
            {inventory.length === 0 ? (
              <p data-testid="inventory-empty">
                Du hast noch keine Gegenstaende. Schau im Shop vorbei!
              </p>
            ) : (
              <ul data-testid="inventory-list">
                {inventory.map((entry) => (
                  <li key={entry.itemId} data-testid={`inv-${entry.itemId}`}>
                    {entry.itemId}
                    <button
                      type="button"
                      data-testid={`equip-${entry.itemId}`}
                      onClick={() => handleEquip(entry.itemId)}
                    >
                      Anziehen
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </section>

          <p>
            <Link to="/shop" data-testid="to-shop">
              Zum Shop
            </Link>
          </p>
        </>
      )}
    </main>
  );
}
