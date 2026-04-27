/**
 * GalleryPage — UC-006 child unlocks creature and picks companion.
 *
 * On mount, the page calls {@code processCreatureUnlocks} so newly mastered
 * domains are reflected immediately, then renders the gallery
 * (locked + unlocked entries). The child can pick any unlocked creature as
 * companion (BR-003, swap any time). Picking a locked creature is impossible
 * from the UI (button disabled); the backend additionally guards with 409.
 *
 * UI copy is Swiss High German with umlauts, without sharp s
 * (NFR-I18N-002, NFR-I18N-004).
 */
import { useEffect, useState } from 'react';
import {
  getCreatureGallery,
  processCreatureUnlocks,
  pickCompanion,
} from '../api/client';
import type {
  CreatureResponse,
  GalleryResponse,
} from '../api/types';

export default function GalleryPage() {
  const childId = sessionStorage.getItem('numnia_child_id') ?? '';
  const [gallery, setGallery] = useState<GalleryResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [unlockBanner, setUnlockBanner] = useState<CreatureResponse[] | null>(null);
  const [consolation, setConsolation] = useState<number | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  useEffect(() => {
    if (!childId) {
      setLoading(false);
      return;
    }
    let cancelled = false;
    (async () => {
      try {
        const result = await processCreatureUnlocks(childId);
        if (cancelled) return;
        if (result.newlyUnlocked.length > 0) {
          setUnlockBanner(result.newlyUnlocked);
        }
        if (result.consolationAwarded) {
          setConsolation(result.starPointsAwarded);
        }
        const g = await getCreatureGallery(childId);
        if (!cancelled) {
          setGallery(g);
          setLoading(false);
        }
      } catch {
        if (!cancelled) {
          setLoadError('Die Galerie konnte nicht geladen werden.');
          setLoading(false);
        }
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [childId]);

  if (!childId) {
    return (
      <main>
        <h2>Galerie</h2>
        <p>Bitte zuerst anmelden.</p>
      </main>
    );
  }

  async function handlePick(creatureId: string) {
    setActionError(null);
    try {
      await pickCompanion(childId, creatureId);
      const g = await getCreatureGallery(childId);
      setGallery(g);
    } catch {
      setActionError('Diese Kreatur ist noch nicht freigeschaltet.');
    }
  }

  return (
    <main data-testid="gallery">
      <h2>Galerie</h2>
      <p>Hier siehst du deine Kreaturen.</p>

      {loading && <p>Galerie wird geladen…</p>}
      {loadError && <p role="alert">{loadError}</p>}

      {unlockBanner && unlockBanner.length > 0 && (
        <p
          role="status"
          data-testid="unlock-banner"
          aria-live="polite"
        >
          Neu freigeschaltet:{' '}
          {unlockBanner.map((c) => c.displayName).join(', ')}!
        </p>
      )}

      {consolation !== null && (
        <p role="status" data-testid="consolation-banner">
          Du hast schon alle Kreaturen. Als Belohnung bekommst du{' '}
          {consolation} Sternenpunkte.
        </p>
      )}

      {actionError && (
        <p role="alert" data-testid="action-error">
          {actionError}
        </p>
      )}

      {gallery && (
        <ul data-testid="gallery-list">
          {gallery.entries.map((entry) => (
            <li key={entry.id} data-testid={`creature-${entry.id}`}>
              <h3>{entry.displayName}</h3>
              <p data-testid={`status-${entry.id}`}>
                {entry.unlocked ? 'Freigeschaltet' : 'Noch nicht freigeschaltet'}
              </p>
              {entry.isCompanion && (
                <p data-testid={`companion-badge-${entry.id}`}>
                  Aktiver Begleiter
                </p>
              )}
              <button
                type="button"
                data-testid={`pick-${entry.id}`}
                disabled={!entry.unlocked || entry.isCompanion}
                onClick={() => handlePick(entry.id)}
              >
                Als Begleiter waehlen
              </button>
            </li>
          ))}
        </ul>
      )}
    </main>
  );
}
