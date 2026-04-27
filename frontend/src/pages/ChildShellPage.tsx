/**
 * ChildShellPage — minimal post-sign-in shell for the child area (UC-002).
 *
 * Provides a sign-out action that calls {@link signOutChild}, clears the
 * session from sessionStorage, and redirects to the child sign-in page.
 *
 * UI copy is Swiss High German with umlauts, without sharp s
 * (NFR-I18N-002, NFR-I18N-004).
 */
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { signOutChild } from '../api/client';

export default function ChildShellPage() {
  const navigate = useNavigate();
  const [signingOut, setSigningOut] = useState(false);

  async function handleSignOut() {
    const token = sessionStorage.getItem('numnia_child_session_token') ?? '';
    setSigningOut(true);
    try {
      await signOutChild(token);
    } finally {
      sessionStorage.removeItem('numnia_child_session_token');
      sessionStorage.removeItem('numnia_child_id');
      setSigningOut(false);
      navigate('/sign-in/child');
    }
  }

  return (
    <main>
      <h2>Willkommen, kleiner Rechner!</h2>
      <p>Hier kommen bald spannende Lernspiele.</p>
      <button onClick={handleSignOut} disabled={signingOut}>
        Abmelden
      </button>
    </main>
  );
}
