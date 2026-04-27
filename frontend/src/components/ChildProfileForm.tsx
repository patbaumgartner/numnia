/**
 * ChildProfileForm — create a child profile with fantasy name, year of birth,
 * and avatar selection.
 *
 * UI copy is Swiss High German with umlauts, no sharp s (NFR-I18N-002).
 * Age range 7-12 is enforced client-side and server-side (BR-002 / flow 7a).
 *
 * Fantasy names (BR-002) and avatar models (BR-003) are selected from predefined
 * lists. In production these lists are fetched from the backend; for simplicity
 * in UC-001 they are inlined here and match the backend catalog.
 */
import { useState } from 'react';
import { createChildProfile, ApiError } from '../api/client';

export const FANTASY_NAMES = [
  'Astra', 'Blitz', 'Comet', 'Deva', 'Echo',
  'Flair', 'Glint', 'Halo', 'Iris', 'Jade',
  'Kite', 'Luna', 'Miro', 'Nova', 'Orion',
  'Pixel', 'Quest', 'Rho', 'Sol', 'Terra',
  'Uno', 'Vega', 'Wave', 'Xeno', 'Yuki', 'Zara',
];

export const AVATAR_MODELS = [
  'cloud', 'fire', 'moon', 'rock', 'star', 'sun', 'wave', 'wind',
];

const CURRENT_YEAR = new Date().getFullYear();
const MIN_AGE = 7;
const MAX_AGE = 12;
const MIN_YEAR = CURRENT_YEAR - MAX_AGE;
const MAX_YEAR = CURRENT_YEAR - MIN_AGE;

export interface ChildProfileFormProps {
  parentId: string;
  onSuccess: (childId: string, pseudonym: string) => void;
}

interface FormValues {
  pseudonym: string;
  yearOfBirth: string;
  avatarBaseModel: string;
}

interface FieldErrors {
  pseudonym?: string;
  yearOfBirth?: string;
  avatarBaseModel?: string;
}

function validateForm(values: FormValues): FieldErrors {
  const errors: FieldErrors = {};

  if (!values.pseudonym) {
    errors.pseudonym = 'Bitte waehle einen Fantasienamen aus.';
  }

  if (!values.yearOfBirth) {
    errors.yearOfBirth = 'Geburtsjahr ist erforderlich.';
  } else {
    const year = parseInt(values.yearOfBirth, 10);
    if (isNaN(year) || year < MIN_YEAR || year > MAX_YEAR) {
      errors.yearOfBirth =
        `Numnia ist fuer Kinder im Alter von ${MIN_AGE} bis ${MAX_AGE} Jahren.`;
    }
  }

  if (!values.avatarBaseModel) {
    errors.avatarBaseModel = 'Bitte waehle einen Avatar aus.';
  }

  return errors;
}

export default function ChildProfileForm({
  parentId,
  onSuccess,
}: ChildProfileFormProps) {
  const [values, setValues] = useState<FormValues>({
    pseudonym: '',
    yearOfBirth: '',
    avatarBaseModel: '',
  });

  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});
  const [serverError, setServerError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleChange = (
    e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>,
  ) => {
    const { name, value } = e.target;
    setValues(prev => ({ ...prev, [name]: value }));
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
      const response = await createChildProfile(parentId, {
        pseudonym: values.pseudonym,
        yearOfBirth: parseInt(values.yearOfBirth, 10),
        avatarBaseModel: values.avatarBaseModel,
      });
      onSuccess(response.childProfileId, response.pseudonym);
    } catch (err) {
      if (err instanceof ApiError && err.status === 422) {
        setServerError(err.message);
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
    <form onSubmit={handleSubmit} noValidate aria-label="Kinderprofil erstellen">
      <h2>Kinderprofil erstellen</h2>

      <div>
        <label htmlFor="pseudonym">Fantasiename</label>
        <select
          id="pseudonym"
          name="pseudonym"
          value={values.pseudonym}
          onChange={handleChange}
          aria-invalid={!!fieldErrors.pseudonym}
          aria-describedby={fieldErrors.pseudonym ? 'pseudonym-error' : undefined}
        >
          <option value="">Bitte waehlen</option>
          {FANTASY_NAMES.map(name => (
            <option key={name} value={name}>
              {name}
            </option>
          ))}
        </select>
        {fieldErrors.pseudonym && (
          <span id="pseudonym-error" role="alert">
            {fieldErrors.pseudonym}
          </span>
        )}
      </div>

      <div>
        <label htmlFor="yearOfBirth">Geburtsjahr</label>
        <input
          id="yearOfBirth"
          name="yearOfBirth"
          type="number"
          min={MIN_YEAR}
          max={MAX_YEAR}
          value={values.yearOfBirth}
          onChange={handleChange}
          aria-invalid={!!fieldErrors.yearOfBirth}
          aria-describedby={fieldErrors.yearOfBirth ? 'yearOfBirth-error' : undefined}
        />
        {fieldErrors.yearOfBirth && (
          <span id="yearOfBirth-error" role="alert">
            {fieldErrors.yearOfBirth}
          </span>
        )}
      </div>

      <div>
        <label htmlFor="avatarBaseModel">Avatar</label>
        <select
          id="avatarBaseModel"
          name="avatarBaseModel"
          value={values.avatarBaseModel}
          onChange={handleChange}
          aria-invalid={!!fieldErrors.avatarBaseModel}
          aria-describedby={
            fieldErrors.avatarBaseModel ? 'avatarBaseModel-error' : undefined
          }
        >
          <option value="">Bitte waehlen</option>
          {AVATAR_MODELS.map(model => (
            <option key={model} value={model}>
              {model}
            </option>
          ))}
        </select>
        {fieldErrors.avatarBaseModel && (
          <span id="avatarBaseModel-error" role="alert">
            {fieldErrors.avatarBaseModel}
          </span>
        )}
      </div>

      {serverError && <p role="alert">{serverError}</p>}

      <button type="submit" disabled={isSubmitting}>
        {isSubmitting ? 'Bitte warten…' : 'Profil erstellen'}
      </button>
    </form>
  );
}
