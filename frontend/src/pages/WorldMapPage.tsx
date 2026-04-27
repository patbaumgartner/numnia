/**
 * WorldMapPage — UC-005 child enters a world through a portal.
 *
 * Shows the three Release-1 worlds with difficulty hints and offers, per
 * world, a Training portal (openable) plus the locked R2 portal types
 * (Duell, Team, Event, Boss, Klasse, Saison) with a "Kommt spaeter" notice
 * (BR-001).
 *
 * The unlock check is performed server-side; the page only routes to the
 * practice stage when {@code locked === false}. Reduced-motion preference is
 * read from the backend response and applied as a CSS class so the 3D shell
 * can lower particle effects (NFR-A11Y-003).
 *
 * UI copy is Swiss High German with umlauts, without sharp s
 * (NFR-I18N-002, NFR-I18N-004).
 */
import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { listWorlds, enterPortal } from '../api/client';
import type {
  PortalEntryResponse,
  PortalType,
  WorldResponse,
} from '../api/types';

const LOCKED_PORTALS: Array<{ type: PortalType; label: string }> = [
  { type: 'DUEL', label: 'Duell' },
  { type: 'TEAM', label: 'Team' },
  { type: 'EVENT', label: 'Event' },
  { type: 'BOSS', label: 'Boss' },
  { type: 'CLASS', label: 'Klasse' },
  { type: 'SEASON', label: 'Saison' },
];

const LOCKED_MESSAGES: Record<string, string> = {
  WORLD_PORTAL_COMING_LATER: 'Kommt spaeter.',
  WORLD_PORTAL_LEVEL_TOO_LOW:
    'Diese Welt oeffnet sich, wenn du etwas mehr geuebt hast.',
  WORLD_PORTAL_TASK_POOL_MISSING:
    'Die Aufgaben werden bald bereitgestellt.',
};

export default function WorldMapPage() {
  const childId = sessionStorage.getItem('numnia_child_id') ?? '';
  const navigate = useNavigate();
  const [worlds, setWorlds] = useState<WorldResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [lastEntry, setLastEntry] = useState<PortalEntryResponse | null>(null);
  const [reducedMotion, setReducedMotion] = useState(false);

  useEffect(() => {
    if (!childId) {
      setLoading(false);
      return;
    }
    let cancelled = false;
    listWorlds()
      .then((list) => {
        if (!cancelled) {
          setWorlds(list);
          setLoading(false);
        }
      })
      .catch(() => {
        if (!cancelled) {
          setLoadError('Die Welten konnten nicht geladen werden.');
          setLoading(false);
        }
      });
    return () => {
      cancelled = true;
    };
  }, [childId]);

  if (!childId) {
    return (
      <main>
        <h2>Welten</h2>
        <p>Bitte zuerst anmelden.</p>
      </main>
    );
  }

  async function handleEnter(worldId: string, portalType: PortalType) {
    setLastEntry(null);
    try {
      const entry = await enterPortal(childId, worldId, portalType);
      setLastEntry(entry);
      if (entry.reducedMotion) {
        setReducedMotion(true);
      }
      if (!entry.locked && entry.target === 'PRACTICE_STAGE') {
        navigate('/training');
      }
    } catch {
      setLoadError('Beim Oeffnen ist ein Fehler aufgetreten.');
    }
  }

  return (
    <main
      data-testid="world-map"
      className={reducedMotion ? 'reduced-motion' : undefined}
    >
      <h2>Welten</h2>
      <p>Waehle eine Welt aus und oeffne ein Portal.</p>

      {loading && <p>Welten werden geladen…</p>}
      {loadError && <p role="alert">{loadError}</p>}

      <ul data-testid="worlds-list">
        {worlds.map((w) => (
          <li key={w.id} data-testid={`world-${w.id}`}>
            <h3>{w.displayName}</h3>
            <p data-testid={`difficulty-${w.id}`}>
              Schwierigkeit: Stufe {w.difficultyLevel}
            </p>
            <p>Empfohlen ab Niveau S{w.requiredLevel}.</p>
            <button
              type="button"
              data-testid={`enter-training-${w.id}`}
              onClick={() => handleEnter(w.id, 'TRAINING')}
            >
              Training oeffnen
            </button>
            <ul data-testid={`locked-portals-${w.id}`}>
              {LOCKED_PORTALS.map((p) => (
                <li key={p.type}>
                  <button
                    type="button"
                    data-testid={`enter-${p.type}-${w.id}`}
                    onClick={() => handleEnter(w.id, p.type)}
                  >
                    {p.label}
                  </button>
                  <span> — Kommt spaeter</span>
                </li>
              ))}
            </ul>
          </li>
        ))}
      </ul>

      {lastEntry?.locked && (
        <p role="status" data-testid="portal-message">
          {LOCKED_MESSAGES[lastEntry.messageCode ?? ''] ?? 'Kommt spaeter.'}
        </p>
      )}
    </main>
  );
}
