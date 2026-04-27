/**
 * RegisterPage — wraps RegistrationForm and handles post-registration routing.
 */
import { useNavigate } from 'react-router-dom';
import RegistrationForm from '../components/RegistrationForm';

export default function RegisterPage() {
  const navigate = useNavigate();

  const handleSuccess = (parentId: string) => {
    // Store parentId in sessionStorage for the verification flow
    sessionStorage.setItem('numnia_parent_id', parentId);
    navigate('/register/check-email');
  };

  return (
    <main>
      <RegistrationForm onSuccess={handleSuccess} />
    </main>
  );
}
