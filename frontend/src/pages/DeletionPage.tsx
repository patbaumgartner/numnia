/**
 * UC-011 — DeletionPage: parent self-service deletion of a child account.
 *
 * Two-step flow with cool-off:
 *   1. Trigger:  password + confirmation word "DELETE" → email link.
 *   2. Confirm:  parent opens the link from email (token in URL).
 *
 * Swiss High German UI copy, umlauts, NO sharp s (NFR-I18N-002/004).
 * Cross-link to UC-010 export so parents can save data first (BR-001).
 * No PII shown (NFR-PRIV-001).
 */
import { useEffect, useState } from 'react';
import { useParams, useSearchParams, Link } from 'react-router-dom';
import {
  confirmChildDeletion,
  requestChildDeletion,
} from '../api/client';
import type {
  DeletionRecordResponse,
  DeletionRequestSummary,
} from '../api/types';

type Status = 'idle' | 'loading' | 'triggered' | 'confirmed' | 'error';

export default function DeletionPage() {
  const { childId } = useParams<{ childId: string }>();
  const [search] = useSearchParams();
  const token = search.get('token') ?? '';

  const parentId =
    (typeof window !== 'undefined' && window.localStorage.getItem('parentId')) || '';

  const [password, setPassword] = useState('');
  const [confirmationWord, setConfirmationWord] = useState('');
  const [status, setStatus] = useState<Status>('idle');
  const [error, setError] = useState('');
  const [pending, setPending] = useState<DeletionRequestSummary | null>(null);
  const [record, setRecord] = useState<DeletionRecordResponse | null>(null);

  // If a token is present in the URL the parent landed here via the
  // confirmation email — auto-confirm.
  useEffect(() => {
    if (!parentId || !token || status !== 'idle') return;
    setStatus('loading');
    confirmChildDeletion(parentId, token)
      .then((r) => {
        setRecord(r);
        setStatus('confirmed');
      })
      .catch(() => {
        setStatus('error');
        setError(
          'Der Bestaetigungslink ist abgelaufen oder bereits verwendet.',
        );
      });
  }, [parentId, token, status]);

  if (!parentId) {
    return (
      <main>
        <h2>Konto loeschen</h2>
        <p role="alert">Bitte zuerst als Elternteil anmelden.</p>
      </main>
    );
  }

  async function onSubmit(event: React.FormEvent) {
    event.preventDefault();
    if (!childId) return;
    setStatus('loading');
    setError('');
    try {
      const result = await requestChildDeletion(
        parentId,
        childId,
        password,
        confirmationWord,
      );
      setPending(result);
      setStatus('triggered');
      setPassword('');
      setConfirmationWord('');
    } catch {
      setStatus('error');
      setError(
        'Loeschung konnte nicht gestartet werden. Bitte Passwort und Bestaetigungswort pruefen.',
      );
    }
  }

  return (
    <main>
      <h2>Konto loeschen</h2>

      <p role="note">
        <strong>Achtung:</strong> Diese Aktion entfernt alle persoenlichen
        Daten des Kinderprofils unwiderruflich. Speichern Sie vorher bei
        Bedarf einen{' '}
        <Link to={`/parents/exports/${childId ?? ''}`}>Daten-Export</Link>.
      </p>

      {status !== 'confirmed' && (
        <form onSubmit={onSubmit} aria-label="Loeschung anfordern">
          <label>
            <span>Eltern-Passwort</span>
            <input
              type="password"
              name="password"
              autoComplete="current-password"
              required
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
          </label>
          <label>
            <span>Geben Sie zur Bestaetigung das Wort DELETE ein</span>
            <input
              type="text"
              name="confirmationWord"
              required
              value={confirmationWord}
              onChange={(e) => setConfirmationWord(e.target.value)}
            />
          </label>

          <button type="submit" disabled={status === 'loading'}>
            Loeschung anfordern
          </button>
        </form>
      )}

      {status === 'loading' && <p role="status">Bitte warten ...</p>}

      {status === 'triggered' && pending && (
        <section role="status" aria-label="Loeschung angefordert">
          <h3>Bestaetigung per E-Mail</h3>
          <p>
            Wir haben Ihnen einen Bestaetigungslink gesendet. Der Link bleibt
            24 Stunden gueltig. Falls Sie den Link nicht oeffnen, wird die
            Anfrage automatisch verworfen.
          </p>
        </section>
      )}

      {status === 'confirmed' && record && (
        <section aria-label="Loeschung abgeschlossen">
          <h3>Loeschung abgeschlossen</h3>
          <p>
            Alle persoenlichen Daten wurden entfernt. Sie erhalten eine
            schriftliche Bestaetigung mit Datum und betroffenen Datenkategorien.
          </p>
          <dl>
            <dt>Abgeschlossen am</dt>
            <dd>{new Date(record.completedAt).toLocaleString('de-CH')}</dd>
            <dt>Datenkategorien</dt>
            <dd>{record.dataCategories.join(', ')}</dd>
          </dl>
        </section>
      )}

      {status === 'error' && <p role="alert">{error}</p>}

      <p>
        Hinweis: Jede Anfrage und jede Bestaetigung wird sicher im Audit-Log
        protokolliert.
      </p>
    </main>
  );
}
