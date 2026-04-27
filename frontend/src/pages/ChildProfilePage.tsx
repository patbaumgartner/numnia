/**
 * ChildProfilePage — wraps ChildProfileForm and handles post-creation routing.
 */
import { useNavigate } from 'react-router-dom';
import ChildProfileForm from '../components/ChildProfileForm';

export default function ChildProfilePage() {
  const navigate = useNavigate();
  const parentId = sessionStorage.getItem('numnia_parent_id') ?? '';

  const handleSuccess = (childId: string, pseudonym: string) => {
    sessionStorage.setItem('numnia_child_id', childId);
    sessionStorage.setItem('numnia_child_pseudonym', pseudonym);
    navigate('/onboarding/check-email');
  };

  return (
    <main>
      <ChildProfileForm parentId={parentId} onSuccess={handleSuccess} />
    </main>
  );
}
