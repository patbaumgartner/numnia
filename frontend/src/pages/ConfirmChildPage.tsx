/**
 * ConfirmChildPage — handles the secondary confirmation link click.
 *
 * Reads the `token` query parameter and calls the backend confirmation endpoint.
 * On success, routes to the done page.
 *
 * Swiss High German copy, no sharp s.
 */
import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { confirmChildProfile, ApiError } from '../api/client';

type State = 'confirming' | 'success' | 'expired' | 'error';

export default function ConfirmChildPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const [state, setState] = useState<State>('confirming');
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    const token = searchParams.get('token');
    const parentId = sessionStorage.getItem('numnia_parent_id');
    const childId = sessionStorage.getItem('numnia_child_id');

    if (!token || !parentId || !childId) {
      setState('error');
      setErrorMessage('Ungueltige Bestaetigung: fehlende Parameter.');
      return;
    }

    confirmChildProfile(parentId, childId, { token })
      .then(() => {
        setState('success');
        navigate('/onboarding/done', { replace: true });
      })
      .catch(err => {
        if (err instanceof ApiError && err.status === 410) {
          setState('expired');
        } else {
          setState('error');
          setErrorMessage('Es ist ein Fehler aufgetreten. Bitte versuchen Sie es erneut.');
        }
      });
  }, [searchParams, navigate]);

  if (state === 'confirming') {
    return (
      <main>
        <p>Bestaetigung wird verarbeitet…</p>
      </main>
    );
  }

  if (state === 'expired') {
    return (
      <main>
        <h2>Link abgelaufen</h2>
        <p>
          Der Bestaetigung link ist abgelaufen. Das Kinderprofil und das Konto
          bleiben erhalten; sensible Funktionen bleiben gesperrt. Sie koennen
          einen neuen Bestaetigung link anfordern.
        </p>
      </main>
    );
  }

  if (state === 'error') {
    return (
      <main>
        <h2>Fehler</h2>
        <p role="alert">{errorMessage}</p>
      </main>
    );
  }

  return null;
}
