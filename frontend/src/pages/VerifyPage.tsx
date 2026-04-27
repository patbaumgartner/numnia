/**
 * VerifyPage — handles the primary email verification link click.
 *
 * Reads the `token` query parameter from the URL and calls the backend
 * verification endpoint. On success, routes to the child profile creation step.
 *
 * Swiss High German copy, no sharp s.
 */
import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { verifyEmail, ApiError } from '../api/client';

type State = 'verifying' | 'success' | 'expired' | 'error';

export default function VerifyPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const [state, setState] = useState<State>('verifying');
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    const token = searchParams.get('token');
    if (!token) {
      setState('error');
      setErrorMessage('Ungueltige Bestaetigung: kein Token gefunden.');
      return;
    }

    verifyEmail({ token })
      .then(response => {
        sessionStorage.setItem('numnia_parent_id', response.parentId);
        setState('success');
        navigate('/onboarding/child', { replace: true });
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

  if (state === 'verifying') {
    return (
      <main>
        <p>E-Mail wird bestaetigt…</p>
      </main>
    );
  }

  if (state === 'expired') {
    return (
      <main>
        <h2>Link abgelaufen</h2>
        <p>
          Der Bestaetigung link ist abgelaufen (gueltig fuer 24 Stunden). Bitte
          fordern Sie einen neuen Bestaetigung link an.
        </p>
        <a href="/register" data-testid="request-new-link">
          Neuen Bestaetigung link anfordern
        </a>
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

  // success — navigating
  return null;
}
