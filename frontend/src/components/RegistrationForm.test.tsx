/**
 * Unit tests for RegistrationForm (UC-001, FR-001, FR-005, NFR-I18N-002,
 * NFR-I18N-004).
 *
 * Tests follow TDD Red→Green: all were written before the component existed.
 */
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import RegistrationForm from './RegistrationForm';
import * as client from '../api/client';

// Keep a reference so we can spy on the module
vi.mock('../api/client', () => ({
  registerParent: vi.fn(),
  ApiError: class ApiError extends Error {
    status: number;
    code: string;
    constructor(status: number, code: string, message: string) {
      super(message);
      this.name = 'ApiError';
      this.status = status;
      this.code = code;
    }
  },
}));

const mockedRegisterParent = vi.mocked(client.registerParent);

const fillValidForm = async (user: ReturnType<typeof userEvent.setup>) => {
  await user.selectOptions(screen.getByLabelText('Anrede'), 'Frau');
  await user.type(screen.getByLabelText('Vorname'), 'Anna');
  await user.type(screen.getByLabelText('E-Mail-Adresse'), 'anna@example.com');
  await user.type(screen.getByLabelText('Passwort'), 'securePass1');
  await user.click(screen.getByLabelText(/Datenschutzerklaerung/i));
  await user.click(screen.getByLabelText(/Nutzungsbedingungen/i));
};

describe('RegistrationForm', () => {
  const onSuccess = vi.fn();
  const user = userEvent.setup();

  beforeEach(() => {
    vi.clearAllMocks();
    render(<RegistrationForm onSuccess={onSuccess} />);
  });

  it('renders all required fields', () => {
    expect(screen.getByLabelText('Anrede')).toBeInTheDocument();
    expect(screen.getByLabelText('Vorname')).toBeInTheDocument();
    expect(screen.getByLabelText('E-Mail-Adresse')).toBeInTheDocument();
    expect(screen.getByLabelText('Passwort')).toBeInTheDocument();
    expect(
      screen.getByLabelText(/Datenschutzerklaerung/i),
    ).toBeInTheDocument();
    expect(
      screen.getByLabelText(/Nutzungsbedingungen/i),
    ).toBeInTheDocument();
  });

  it('does not contain the sharp s (ß) in any label or message', () => {
    const { container } = screen.getByRole('form', {
      name: 'Registrierungsformular',
    }).closest('body') as Element;
    expect(document.body.textContent).not.toContain('ß');
  });

  it('shows required-field errors when submitted empty', async () => {
    await user.click(screen.getByRole('button', { name: /Konto erstellen/i }));
    expect(await screen.findByText('Vorname ist erforderlich.')).toBeInTheDocument();
    expect(screen.getByText('Anrede ist erforderlich.')).toBeInTheDocument();
    expect(screen.getByText('E-Mail-Adresse ist erforderlich.')).toBeInTheDocument();
    expect(screen.getByText('Passwort ist erforderlich.')).toBeInTheDocument();
    expect(
      screen.getByText('Datenschutzeinwilligung ist erforderlich.'),
    ).toBeInTheDocument();
    expect(
      screen.getByText('Nutzungsbedingungen muessen akzeptiert werden.'),
    ).toBeInTheDocument();
  });

  it('shows validation error for invalid email format', async () => {
    await user.type(screen.getByLabelText('E-Mail-Adresse'), 'not-an-email');
    await user.click(screen.getByRole('button', { name: /Konto erstellen/i }));
    expect(
      await screen.findByText('E-Mail-Adresse ist ungueltig.'),
    ).toBeInTheDocument();
  });

  it('shows validation error for password shorter than 8 characters', async () => {
    await user.type(screen.getByLabelText('Passwort'), 'short');
    await user.click(screen.getByRole('button', { name: /Konto erstellen/i }));
    expect(
      await screen.findByText('Passwort muss mindestens 8 Zeichen lang sein.'),
    ).toBeInTheDocument();
  });

  it('calls registerParent with correct payload on valid submission', async () => {
    mockedRegisterParent.mockResolvedValueOnce({ parentId: 'uuid-123' });

    await fillValidForm(user);
    await user.click(screen.getByRole('button', { name: /Konto erstellen/i }));

    await waitFor(() => {
      expect(mockedRegisterParent).toHaveBeenCalledWith({
        firstName: 'Anna',
        salutation: 'Frau',
        email: 'anna@example.com',
        password: 'securePass1',
        privacyConsented: true,
        termsAccepted: true,
      });
    });
  });

  it('calls onSuccess with parentId on successful registration', async () => {
    mockedRegisterParent.mockResolvedValueOnce({ parentId: 'uuid-456' });

    await fillValidForm(user);
    await user.click(screen.getByRole('button', { name: /Konto erstellen/i }));

    await waitFor(() => {
      expect(onSuccess).toHaveBeenCalledWith('uuid-456');
    });
  });

  it('shows Swiss German duplicate-email error on 409 response', async () => {
    const { ApiError } = await import('../api/client');
    mockedRegisterParent.mockRejectedValueOnce(
      new ApiError(409, 'DUPLICATE_EMAIL', 'Email already registered'),
    );

    await fillValidForm(user);
    await user.click(screen.getByRole('button', { name: /Konto erstellen/i }));

    expect(
      await screen.findByText(/E-Mail-Adresse ist bereits registriert/i),
    ).toBeInTheDocument();
    expect(document.body.textContent).not.toContain('ß');
  });

  it('shows generic error message on unexpected server error', async () => {
    mockedRegisterParent.mockRejectedValueOnce(new Error('Network failure'));

    await fillValidForm(user);
    await user.click(screen.getByRole('button', { name: /Konto erstellen/i }));

    expect(
      await screen.findByText(/Fehler aufgetreten/i),
    ).toBeInTheDocument();
  });

  it('disables submit button while submitting', async () => {
    mockedRegisterParent.mockImplementation(
      () => new Promise(resolve => setTimeout(() => resolve({ parentId: 'x' }), 100)),
    );

    await fillValidForm(user);
    fireEvent.submit(screen.getByRole('form', { name: 'Registrierungsformular' }));

    expect(screen.getByRole('button')).toBeDisabled();
  });
});
