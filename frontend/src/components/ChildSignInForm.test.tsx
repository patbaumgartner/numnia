/**
 * Unit tests for ChildSignInForm (UC-002, FR-006, FR-007, BR-001, BR-003,
 * NFR-I18N-002, NFR-I18N-004).
 *
 * Written BEFORE the component existed (TDD Red→Green).
 */
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import ChildSignInForm from './ChildSignInForm';
import * as client from '../api/client';

vi.mock('../api/client', () => ({
  signInChild: vi.fn(),
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

const mockedSignInChild = vi.mocked(client.signInChild);

describe('ChildSignInForm', () => {
  const onSuccess = vi.fn();
  const onLocked = vi.fn();
  const user = userEvent.setup();

  beforeEach(() => {
    vi.clearAllMocks();
    render(<ChildSignInForm onSuccess={onSuccess} onLocked={onLocked} />);
  });

  it('renders Kind-ID and PIN fields with Anmelden button', () => {
    expect(screen.getByLabelText(/Kind-ID/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/PIN/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Anmelden/i })).toBeInTheDocument();
  });

  it('does not contain sharp s (NFR-I18N-004)', () => {
    expect(document.body.textContent).not.toContain('ß');
  });

  it('calls signInChild with childId and PIN on submit', async () => {
    const resp = {
      sessionToken: 'tok-1',
      childId: 'c-1',
      role: 'CHILD',
      expiresAt: '2099-01-01T00:00:00Z',
    };
    mockedSignInChild.mockResolvedValueOnce(resp);

    await user.type(screen.getByLabelText(/Kind-ID/i), 'c-1');
    await user.type(screen.getByLabelText(/PIN/i), '1234');
    await user.click(screen.getByRole('button', { name: /Anmelden/i }));

    await waitFor(() => expect(mockedSignInChild).toHaveBeenCalledWith({ childId: 'c-1', pin: '1234' }));
    expect(onSuccess).toHaveBeenCalledWith('tok-1', 'c-1');
  });

  it('shows error message on wrong PIN (401)', async () => {
    const { ApiError } = await import('../api/client');
    mockedSignInChild.mockRejectedValueOnce(
      new ApiError(401, 'INVALID_PIN', 'Wrong PIN'),
    );

    await user.type(screen.getByLabelText(/Kind-ID/i), 'c-1');
    await user.type(screen.getByLabelText(/PIN/i), '0000');
    await user.click(screen.getByRole('button', { name: /Anmelden/i }));

    expect(await screen.findByRole('alert')).toBeInTheDocument();
    expect(screen.getByText(/Falscher PIN/i)).toBeInTheDocument();
    expect(onSuccess).not.toHaveBeenCalled();
  });

  it('calls onLocked when profile is locked (423)', async () => {
    const { ApiError } = await import('../api/client');
    mockedSignInChild.mockRejectedValueOnce(
      new ApiError(423, 'PROFILE_LOCKED', 'Locked'),
    );

    await user.type(screen.getByLabelText(/Kind-ID/i), 'c-1');
    await user.type(screen.getByLabelText(/PIN/i), '1234');
    await user.click(screen.getByRole('button', { name: /Anmelden/i }));

    await waitFor(() => expect(onLocked).toHaveBeenCalled());
    expect(onSuccess).not.toHaveBeenCalled();
  });

  it('shows generic error on unexpected failure', async () => {
    mockedSignInChild.mockRejectedValueOnce(new Error('Network error'));

    await user.type(screen.getByLabelText(/Kind-ID/i), 'c-1');
    await user.type(screen.getByLabelText(/PIN/i), '1234');
    await user.click(screen.getByRole('button', { name: /Anmelden/i }));

    expect(await screen.findByRole('alert')).toBeInTheDocument();
    expect(onSuccess).not.toHaveBeenCalled();
  });

  it('disables the button while submitting', async () => {
    mockedSignInChild.mockImplementation(() => new Promise(() => {})); // never resolves

    await user.type(screen.getByLabelText(/Kind-ID/i), 'c-1');
    await user.type(screen.getByLabelText(/PIN/i), '1234');
    await user.click(screen.getByRole('button', { name: /Anmelden/i }));

    expect(screen.getByRole('button', { name: /Anmelden/i })).toBeDisabled();
  });
});
