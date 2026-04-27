/**
 * ProgressPage — UC-008: child views own learning progress.
 *
 * Swiss High German UI copy with umlauts, no sharp s (NFR-I18N-002, NFR-I18N-004).
 * No leaderboards (BR-002). Color-blind palette applied via CSS class on the
 * container so the entire visualization adapts (NFR-A11Y-003).
 */
import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  getProgress,
  setProgressPalette,
} from '../api/client';
import type {
  ColorPalette,
  MasteryStatus,
  OperationProgressResponse,
  ProgressOverviewResponse,
  ProgressOperation,
} from '../api/types';

const OPERATION_LABELS: Record<ProgressOperation, string> = {
  ADDITION: 'Plus',
  SUBTRACTION: 'Minus',
  MULTIPLICATION: 'Mal',
  DIVISION: 'Geteilt (mit Rest)',
};

const MASTERY_LABELS: Record<MasteryStatus, string> = {
  NOT_STARTED: 'Noch nicht gestartet',
  IN_CONSOLIDATION: 'Im Aufbau',
  MASTERED: 'Sicher beherrscht',
};

const PALETTE_LABELS: Record<ColorPalette, string> = {
  DEFAULT: 'Standardfarben',
  DEUTERANOPIA: 'Deuteranopie',
  PROTANOPIA: 'Protanopie',
  TRITANOPIA: 'Tritanopie',
};

function paletteClass(p: ColorPalette): string {
  return `palette-${p.toLowerCase()}`;
}

export default function ProgressPage() {
  const childId =
    typeof window !== 'undefined' ? window.localStorage.getItem('childId') : null;
  const [overview, setOverview] = useState<ProgressOverviewResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!childId) return;
    getProgress(childId)
      .then(setOverview)
      .catch(() => setError('Daten sind nicht aktuell.'));
  }, [childId]);

  async function changePalette(palette: ColorPalette) {
    if (!childId) return;
    await setProgressPalette(childId, palette);
    const refreshed = await getProgress(childId);
    setOverview(refreshed);
  }

  if (!childId) {
    return (
      <main>
        <h2>Mein Fortschritt</h2>
        <p>Bitte zuerst anmelden.</p>
        <Link to="/sign-in/child">Zur Anmeldung</Link>
      </main>
    );
  }

  if (error && !overview) {
    return (
      <main>
        <h2>Mein Fortschritt</h2>
        <p data-testid="error-banner">{error}</p>
      </main>
    );
  }

  if (!overview) {
    return (
      <main>
        <h2>Mein Fortschritt</h2>
        <p>Wird geladen ...</p>
      </main>
    );
  }

  return (
    <main className={paletteClass(overview.palette)} data-testid="progress-root">
      <h2>Mein Fortschritt</h2>

      {overview.empty ? (
        <p data-testid="empty-banner">
          Leg los und sammle deine ersten Sterne!
        </p>
      ) : (
        <ul data-testid="progress-list">
          {overview.entries.map((entry) => (
            <ProgressEntry key={entry.operation} entry={entry} />
          ))}
        </ul>
      )}

      <section aria-label="Farbprofil">
        <h3>Farbprofil</h3>
        <p>Aktuell: {PALETTE_LABELS[overview.palette]}</p>
        <label>
          Farbprofil waehlen
          <select
            data-testid="palette-select"
            value={overview.palette}
            onChange={(e) => changePalette(e.target.value as ColorPalette)}
          >
            <option value="DEFAULT">Standardfarben</option>
            <option value="DEUTERANOPIA">Deuteranopie</option>
            <option value="PROTANOPIA">Protanopie</option>
            <option value="TRITANOPIA">Tritanopie</option>
          </select>
        </label>
      </section>
    </main>
  );
}

function ProgressEntry({ entry }: { entry: OperationProgressResponse }) {
  const accuracyPct = Math.round(entry.accuracy * 100);
  return (
    <li data-testid={`progress-${entry.operation}`}>
      <h4>{OPERATION_LABELS[entry.operation]}</h4>
      <progress
        data-testid={`bar-${entry.operation}`}
        value={accuracyPct}
        max={100}
        aria-label={`Fortschritt ${OPERATION_LABELS[entry.operation]}`}
      >
        {accuracyPct}%
      </progress>
      <p>
        Genauigkeit: {accuracyPct}% &middot; {entry.totalSessions} Spielrunden
      </p>
      <p data-testid={`mastery-${entry.operation}`}>
        Status: {MASTERY_LABELS[entry.masteryStatus]}
      </p>
    </li>
  );
}
