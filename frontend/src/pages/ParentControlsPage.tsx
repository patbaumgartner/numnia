/**
 * ParentControlsPage — UC-009: parent sets daily limit and risk mechanic.
 *
 * Swiss High German UI copy, umlauts, no sharp s (NFR-I18N-002/004).
 * No PII shown (NFR-PRIV-001); audit log is server-side only.
 */
import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { getChildControls, updateChildControls } from '../api/client';
import type { ChildControlsResponse } from '../api/types';

type Status = 'loading' | 'ready' | 'saving' | 'saved' | 'error' | 'no-limit-confirm';

export default function ParentControlsPage() {
  const { childId } = useParams<{ childId: string }>();
  const parentId =
    (typeof window !== 'undefined' && window.localStorage.getItem('parentId')) || '';

  const [controls, setControls] = useState<ChildControlsResponse | null>(null);
  const [dailyLimit, setDailyLimit] = useState<number | null>(30);
  const [breakMin, setBreakMin] = useState<number>(15);
  const [risk, setRisk] = useState<boolean>(false);
  const [status, setStatus] = useState<Status>('loading');
  const [errorMessage, setErrorMessage] = useState<string>('');

  useEffect(() => {
    if (!parentId || !childId) {
      setStatus('error');
      setErrorMessage('Bitte zuerst als Elternteil anmelden.');
      return;
    }
    getChildControls(parentId, childId)
      .then((c) => {
        setControls(c);
        setDailyLimit(c.dailyLimitMinutes);
        setBreakMin(c.breakRecommendationMinutes);
        setRisk(c.riskMechanicEnabled);
        setStatus('ready');
      })
      .catch(() => {
        setStatus('error');
        setErrorMessage('Einstellungen konnten nicht geladen werden.');
      });
  }, [parentId, childId]);

  async function save(confirmNoLimit: boolean) {
    if (!parentId || !childId) return;
    setStatus('saving');
    try {
      const updated = await updateChildControls(parentId, childId, {
        dailyLimitMinutes: dailyLimit,
        breakRecommendationMinutes: breakMin,
        riskMechanicEnabled: risk,
        confirmNoLimit,
      });
      setControls(updated);
      setStatus('saved');
    } catch (err) {
      setStatus('error');
      const e = err as { code?: string; message?: string };
      if (e.code === 'NO_LIMIT_CONFIRMATION_REQUIRED') {
        setStatus('no-limit-confirm');
      } else {
        setErrorMessage(e.message ?? 'Speichern fehlgeschlagen.');
      }
    }
  }

  function onSubmit(event: React.FormEvent) {
    event.preventDefault();
    if (dailyLimit === null) {
      setStatus('no-limit-confirm');
      return;
    }
    save(false);
  }

  if (status === 'loading') {
    return (
      <main>
        <h2>Einstellungen werden geladen ...</h2>
      </main>
    );
  }

  if (status === 'error' && !controls) {
    return (
      <main>
        <h2>Eltern-Einstellungen</h2>
        <p role="alert">{errorMessage}</p>
      </main>
    );
  }

  return (
    <main>
      <h2>Eltern-Einstellungen</h2>
      <p>
        Hier legen Sie fuer Ihr Kind das taegliche Zeitlimit, die Pause-Erinnerung
        und den Risiko-Modus fest.
      </p>

      <form onSubmit={onSubmit} aria-label="Eltern-Einstellungen">
        <fieldset>
          <legend>Taegliches Zeitlimit</legend>
          <label>
            <input
              type="radio"
              name="limit-mode"
              checked={dailyLimit !== null}
              onChange={() => setDailyLimit(controls?.dailyLimitMinutes ?? 30)}
            />{' '}
            Limit aktiv
          </label>
          <label>
            <input
              type="radio"
              name="limit-mode"
              checked={dailyLimit === null}
              onChange={() => setDailyLimit(null)}
            />{' '}
            Kein Limit
          </label>

          {dailyLimit !== null && (
            <label>
              Minuten pro Tag:{' '}
              <input
                type="number"
                min={5}
                max={240}
                step={5}
                value={dailyLimit}
                onChange={(e) => setDailyLimit(Number(e.target.value))}
                aria-label="Taegliches Zeitlimit in Minuten"
              />
            </label>
          )}
        </fieldset>

        <fieldset>
          <legend>Pause-Erinnerung</legend>
          <label>
            Alle{' '}
            <input
              type="number"
              min={5}
              max={60}
              step={5}
              value={breakMin}
              onChange={(e) => setBreakMin(Number(e.target.value))}
              aria-label="Pause-Erinnerung in Minuten"
            />{' '}
            Minuten
          </label>
        </fieldset>

        <fieldset>
          <legend>Risiko-Modus</legend>
          <label>
            <input
              type="checkbox"
              checked={risk}
              onChange={(e) => setRisk(e.target.checked)}
            />{' '}
            Risiko-Modus aktivieren
          </label>
          <p>
            Hinweis: Punkte und Items gehen nie dauerhaft verloren. Sie werden
            am Ende einer Partie zurueckgegeben.
          </p>
        </fieldset>

        <button type="submit" disabled={status === 'saving'}>
          Speichern
        </button>
      </form>

      {status === 'no-limit-confirm' && (
        <section role="alertdialog" aria-label="Kein Limit bestaetigen">
          <h3>Wirklich kein Tageslimit?</h3>
          <p>
            Ohne Tageslimit kann Ihr Kind unbegrenzt spielen. Bitte bestaetigen
            Sie diese Wahl.
          </p>
          <button type="button" onClick={() => save(true)}>
            Ja, kein Limit
          </button>{' '}
          <button
            type="button"
            onClick={() => {
              setDailyLimit(controls?.dailyLimitMinutes ?? 30);
              setStatus('ready');
            }}
          >
            Abbrechen
          </button>
        </section>
      )}

      {status === 'saved' && <p role="status">Einstellungen gespeichert.</p>}

      {status === 'error' && controls && (
        <p role="alert">{errorMessage}</p>
      )}

      <p>Aenderungen werden im Audit-Log mit Zeitstempel sicher protokolliert.</p>
    </main>
  );
}
