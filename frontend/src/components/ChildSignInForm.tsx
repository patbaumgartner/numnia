/**
 * ChildSignInForm — PIN-entry form for UC-002 child sign-in.
 *
 * Accepts a child ID and a 4-6 digit numeric PIN.
 * Delegates to {@link signInChild} and propagates outcomes via callbacks.
 *
 * UI copy is Swiss High German with umlauts, without sharp s
 * (NFR-I18N-002, NFR-I18N-004).
 */
import { useState } from 'react';
import { signInChild, ApiError } from '../api/client';

interface Props {
  /** Called with (sessionToken, childId) on successful sign-in. */
  onSuccess: (sessionToken: string, childId: string) => void;
  /** Called when the server responds with 423 (profile locked). */
  onLocked: () => void;
}

export default function ChildSignInForm({ onSuccess, onLocked }: Props) {
  const [childId, setChildId] = useState('');
  const [pin, setPin] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      const resp = await signInChild({ childId, pin });
      onSuccess(resp.sessionToken, resp.childId);
    } catch (err) {
      if (err instanceof ApiError) {
        if (err.status === 423) {
          onLocked();
          return;
        }
        if (err.status === 401) {
          setError('Falscher PIN. Bitte erneut versuchen.');
          return;
        }
      }
      setError('Anmeldung fehlgeschlagen. Bitte spaeter erneut versuchen.');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} noValidate>
      <h2>Als Kind anmelden</h2>

      <div>
        <label htmlFor="childId">Kind-ID</label>
        <input
          id="childId"
          type="text"
          value={childId}
          onChange={e => setChildId(e.target.value)}
          required
          autoComplete="off"
        />
      </div>

      <div>
        <label htmlFor="pin">PIN</label>
        <input
          id="pin"
          type="password"
          value={pin}
          onChange={e => setPin(e.target.value)}
          required
          inputMode="numeric"
          autoComplete="current-password"
        />
      </div>

      {error && (
        <p role="alert" style={{ color: 'red' }}>
          {error}
        </p>
      )}

      <button type="submit" disabled={submitting}>
        Anmelden
      </button>
    </form>
  );
}
