/**
 * ChildSignInPage — routing wrapper for the child PIN sign-in form (UC-002).
 *
 * On success: stores the session token in sessionStorage and navigates to
 * the child shell (/child).
 * On locked: navigates to the locked-profile information screen.
 */
import { useNavigate } from 'react-router-dom';
import ChildSignInForm from '../components/ChildSignInForm';

export default function ChildSignInPage() {
  const navigate = useNavigate();

  function handleSuccess(sessionToken: string, childId: string) {
    sessionStorage.setItem('numnia_child_session_token', sessionToken);
    sessionStorage.setItem('numnia_child_id', childId);
    navigate('/child');
  }

  function handleLocked() {
    navigate('/sign-in/child/locked');
  }

  return (
    <main>
      <ChildSignInForm onSuccess={handleSuccess} onLocked={handleLocked} />
    </main>
  );
}
