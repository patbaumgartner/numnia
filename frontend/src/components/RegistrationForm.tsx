/**
 * RegistrationForm — parent account registration.
 *
 * UI copy is Swiss High German with umlauts, no sharp s (NFR-I18N-002, NFR-I18N-004).
 * All validation is also performed server-side (NFR-SEC-001).
 */
import { useState } from 'react';
import { registerParent, ApiError } from '../api/client';

export interface RegistrationFormProps {
  onSuccess: (parentId: string) => void;
}

interface FormValues {
  firstName: string;
  salutation: string;
  email: string;
  password: string;
  privacyConsented: boolean;
  termsAccepted: boolean;
}

interface FieldErrors {
  firstName?: string;
  salutation?: string;
  email?: string;
  password?: string;
  privacyConsented?: string;
  termsAccepted?: string;
}

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

function validateForm(values: FormValues): FieldErrors {
  const errors: FieldErrors = {};

  if (!values.firstName.trim()) {
    errors.firstName = 'Vorname ist erforderlich.';
  }
  if (!values.salutation) {
    errors.salutation = 'Anrede ist erforderlich.';
  }
  if (!values.email.trim()) {
    errors.email = 'E-Mail-Adresse ist erforderlich.';
  } else if (!EMAIL_RE.test(values.email)) {
    errors.email = 'E-Mail-Adresse ist ungueltig.';
  }
  if (!values.password) {
    errors.password = 'Passwort ist erforderlich.';
  } else if (values.password.length < 8) {
    errors.password = 'Passwort muss mindestens 8 Zeichen lang sein.';
  }
  if (!values.privacyConsented) {
    errors.privacyConsented = 'Datenschutzeinwilligung ist erforderlich.';
  }
  if (!values.termsAccepted) {
    errors.termsAccepted = 'Nutzungsbedingungen muessen akzeptiert werden.';
  }

  return errors;
}

export default function RegistrationForm({ onSuccess }: RegistrationFormProps) {
  const [values, setValues] = useState<FormValues>({
    firstName: '',
    salutation: '',
    email: '',
    password: '',
    privacyConsented: false,
    termsAccepted: false,
  });

  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});
  const [serverError, setServerError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleChange = (
    e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>,
  ) => {
    const { name, type, value } = e.target;
    const checked =
      type === 'checkbox' ? (e.target as HTMLInputElement).checked : undefined;
    setValues(prev => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value,
    }));
    // Clear field error on change
    setFieldErrors(prev => ({ ...prev, [name]: undefined }));
    setServerError(null);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    const errors = validateForm(values);
    if (Object.keys(errors).length > 0) {
      setFieldErrors(errors);
      return;
    }

    setIsSubmitting(true);
    setServerError(null);

    try {
      const response = await registerParent(values);
      onSuccess(response.parentId);
    } catch (err) {
      if (err instanceof ApiError && err.status === 409) {
        setServerError(
          'Diese E-Mail-Adresse ist bereits registriert. Bitte melden Sie sich an.',
        );
      } else {
        setServerError(
          'Es ist ein Fehler aufgetreten. Bitte versuchen Sie es erneut.',
        );
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} noValidate aria-label="Registrierungsformular">
      <h2>Konto erstellen</h2>

      <div>
        <label htmlFor="salutation">Anrede</label>
        <select
          id="salutation"
          name="salutation"
          value={values.salutation}
          onChange={handleChange}
          aria-invalid={!!fieldErrors.salutation}
          aria-describedby={fieldErrors.salutation ? 'salutation-error' : undefined}
        >
          <option value="">Bitte waehlen</option>
          <option value="Frau">Frau</option>
          <option value="Herr">Herr</option>
          <option value="Divers">Divers</option>
        </select>
        {fieldErrors.salutation && (
          <span id="salutation-error" role="alert">
            {fieldErrors.salutation}
          </span>
        )}
      </div>

      <div>
        <label htmlFor="firstName">Vorname</label>
        <input
          id="firstName"
          name="firstName"
          type="text"
          value={values.firstName}
          onChange={handleChange}
          aria-invalid={!!fieldErrors.firstName}
          aria-describedby={fieldErrors.firstName ? 'firstName-error' : undefined}
        />
        {fieldErrors.firstName && (
          <span id="firstName-error" role="alert">
            {fieldErrors.firstName}
          </span>
        )}
      </div>

      <div>
        <label htmlFor="email">E-Mail-Adresse</label>
        <input
          id="email"
          name="email"
          type="email"
          value={values.email}
          onChange={handleChange}
          aria-invalid={!!fieldErrors.email}
          aria-describedby={fieldErrors.email ? 'email-error' : undefined}
        />
        {fieldErrors.email && (
          <span id="email-error" role="alert">
            {fieldErrors.email}
          </span>
        )}
      </div>

      <div>
        <label htmlFor="password">Passwort</label>
        <input
          id="password"
          name="password"
          type="password"
          value={values.password}
          onChange={handleChange}
          aria-invalid={!!fieldErrors.password}
          aria-describedby={fieldErrors.password ? 'password-error' : undefined}
        />
        {fieldErrors.password && (
          <span id="password-error" role="alert">
            {fieldErrors.password}
          </span>
        )}
      </div>

      <div>
        <input
          id="privacyConsented"
          name="privacyConsented"
          type="checkbox"
          checked={values.privacyConsented}
          onChange={handleChange}
          aria-invalid={!!fieldErrors.privacyConsented}
        />
        <label htmlFor="privacyConsented">
          Ich stimme der Datenschutzerklaerung zu.
        </label>
        {fieldErrors.privacyConsented && (
          <span role="alert">{fieldErrors.privacyConsented}</span>
        )}
      </div>

      <div>
        <input
          id="termsAccepted"
          name="termsAccepted"
          type="checkbox"
          checked={values.termsAccepted}
          onChange={handleChange}
          aria-invalid={!!fieldErrors.termsAccepted}
        />
        <label htmlFor="termsAccepted">
          Ich akzeptiere die Nutzungsbedingungen.
        </label>
        {fieldErrors.termsAccepted && (
          <span role="alert">{fieldErrors.termsAccepted}</span>
        )}
      </div>

      {serverError && <p role="alert">{serverError}</p>}

      <button type="submit" disabled={isSubmitting}>
        {isSubmitting ? 'Bitte warten…' : 'Konto erstellen'}
      </button>
    </form>
  );
}
