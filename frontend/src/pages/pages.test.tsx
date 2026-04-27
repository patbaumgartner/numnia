/**
 * Smoke tests for all pages (UC-001).
 *
 * Pages are thin routing wrappers. These tests verify rendering and
 * basic behaviour without testing component-level detail (those tests
 * are in RegistrationForm.test.tsx / ChildProfileForm.test.tsx).
 */
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';

import RegisterPage from './RegisterPage';
import CheckEmailPage from './CheckEmailPage';
import VerifyPage from './VerifyPage';
import ChildProfilePage from './ChildProfilePage';
import OnboardingCheckEmailPage from './OnboardingCheckEmailPage';
import ConfirmChildPage from './ConfirmChildPage';
import DonePage from './DonePage';
import ChildSignInPage from './ChildSignInPage';
import ChildLockedPage from './ChildLockedPage';
import ChildShellPage from './ChildShellPage';
import ParentDashboardPage from './ParentDashboardPage';
import * as client from '../api/client';

vi.mock('../api/client', () => ({
  registerParent: vi.fn(),
  verifyEmail: vi.fn(),
  createChildProfile: vi.fn(),
  confirmChildProfile: vi.fn(),
  signInChild: vi.fn(),
  signOutChild: vi.fn(),
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

const mockedVerifyEmail = vi.mocked(client.verifyEmail);
const mockedConfirmChildProfile = vi.mocked(client.confirmChildProfile);

beforeEach(() => {
  sessionStorage.clear();
});

afterEach(() => {
  vi.clearAllMocks();
});

// Helper: wrap in router
function renderIn(
  element: React.ReactNode,
  initialPath = '/',
  routePath = '/',
) {
  return render(
    <MemoryRouter initialEntries={[initialPath]}>
      <Routes>
        <Route path={routePath} element={element} />
        <Route path="*" element={<div data-testid="navigated" />} />
      </Routes>
    </MemoryRouter>,
  );
}

describe('RegisterPage', () => {
  it('renders the RegistrationForm heading', () => {
    renderIn(<RegisterPage />, '/register', '/register');
    expect(screen.getByRole('heading', { name: /Konto erstellen/i })).toBeInTheDocument();
  });

  it('does not contain sharp s', () => {
    renderIn(<RegisterPage />, '/register', '/register');
    expect(document.body.textContent).not.toContain('ß');
  });
});

describe('CheckEmailPage', () => {
  it('renders check-email message', () => {
    renderIn(<CheckEmailPage />, '/check', '/check');
    expect(screen.getByRole('heading', { name: /E-Mail pruefen/i })).toBeInTheDocument();
  });

  it('does not contain sharp s', () => {
    renderIn(<CheckEmailPage />, '/check', '/check');
    expect(document.body.textContent).not.toContain('ß');
  });
});

describe('VerifyPage', () => {
  it('shows verifying message initially', () => {
    mockedVerifyEmail.mockImplementation(() => new Promise(() => {})); // never resolves
    renderIn(<VerifyPage />, '/verify?token=tok', '/verify');
    expect(screen.getByText(/bestaetigt/i)).toBeInTheDocument();
  });

  it('shows error when no token in query string', async () => {
    renderIn(<VerifyPage />, '/verify', '/verify');
    expect(
      await screen.findByText(/kein Token gefunden/i),
    ).toBeInTheDocument();
  });

  it('shows expired message on 410 response', async () => {
    const { ApiError } = await import('../api/client');
    mockedVerifyEmail.mockRejectedValueOnce(
      new ApiError(410, 'TOKEN_EXPIRED', 'Expired'),
    );
    renderIn(<VerifyPage />, '/verify?token=expired-tok', '/verify');
    expect(await screen.findByText(/Link abgelaufen/i)).toBeInTheDocument();
  });

  it('shows generic error on unexpected failure', async () => {
    mockedVerifyEmail.mockRejectedValueOnce(new Error('Network error'));
    renderIn(<VerifyPage />, '/verify?token=bad-tok', '/verify');
    expect(await screen.findByRole('alert')).toBeInTheDocument();
  });

  it('navigates away on success', async () => {
    mockedVerifyEmail.mockResolvedValueOnce({ parentId: 'p-1', status: 'ACTIVE' });
    renderIn(<VerifyPage />, '/verify?token=good-tok', '/verify');
    await waitFor(() =>
      expect(screen.getByTestId('navigated')).toBeInTheDocument(),
    );
  });
});

describe('ChildProfilePage', () => {
  it('renders the child-profile form heading', () => {
    sessionStorage.setItem('numnia_parent_id', 'parent-1');
    renderIn(<ChildProfilePage />, '/onboarding/child', '/onboarding/child');
    expect(
      screen.getByRole('heading', { name: /Kinderprofil erstellen/i }),
    ).toBeInTheDocument();
  });
});

describe('OnboardingCheckEmailPage', () => {
  it('renders onboarding check-email message', () => {
    renderIn(<OnboardingCheckEmailPage />, '/onboarding/check', '/onboarding/check');
    expect(
      screen.getByRole('heading', { name: /Bestaetigung/i }),
    ).toBeInTheDocument();
  });

  it('does not contain sharp s', () => {
    renderIn(<OnboardingCheckEmailPage />, '/onboarding/check', '/onboarding/check');
    expect(document.body.textContent).not.toContain('ß');
  });
});

describe('ConfirmChildPage', () => {
  it('shows confirming message initially', () => {
    sessionStorage.setItem('numnia_parent_id', 'p-1');
    sessionStorage.setItem('numnia_child_id', 'c-1');
    mockedConfirmChildProfile.mockImplementation(() => new Promise(() => {}));
    renderIn(<ConfirmChildPage />, '/onboarding/confirm?token=t', '/onboarding/confirm');
    expect(screen.getByText(/verarbeitet/i)).toBeInTheDocument();
  });

  it('shows error when session values are missing', async () => {
    renderIn(<ConfirmChildPage />, '/onboarding/confirm?token=t', '/onboarding/confirm');
    expect(await screen.findByText(/fehlende Parameter/i)).toBeInTheDocument();
  });

  it('shows expired message on 410 response', async () => {
    const { ApiError } = await import('../api/client');
    sessionStorage.setItem('numnia_parent_id', 'p-1');
    sessionStorage.setItem('numnia_child_id', 'c-1');
    mockedConfirmChildProfile.mockRejectedValueOnce(
      new ApiError(410, 'TOKEN_EXPIRED', 'Expired'),
    );
    renderIn(<ConfirmChildPage />, '/onboarding/confirm?token=x', '/onboarding/confirm');
    expect(await screen.findByText(/Link abgelaufen/i)).toBeInTheDocument();
  });

  it('shows generic error on unexpected failure', async () => {
    sessionStorage.setItem('numnia_parent_id', 'p-1');
    sessionStorage.setItem('numnia_child_id', 'c-1');
    mockedConfirmChildProfile.mockRejectedValueOnce(new Error('Network error'));
    renderIn(<ConfirmChildPage />, '/onboarding/confirm?token=x', '/onboarding/confirm');
    expect(await screen.findByRole('alert')).toBeInTheDocument();
  });

  it('navigates to done on success', async () => {
    sessionStorage.setItem('numnia_parent_id', 'p-1');
    sessionStorage.setItem('numnia_child_id', 'c-1');
    mockedConfirmChildProfile.mockResolvedValueOnce(undefined);
    renderIn(<ConfirmChildPage />, '/onboarding/confirm?token=good', '/onboarding/confirm');
    await waitFor(() =>
      expect(screen.getByTestId('navigated')).toBeInTheDocument(),
    );
  });
});

describe('DonePage', () => {
  it('renders done message', () => {
    sessionStorage.setItem('numnia_child_pseudonym', 'Luna');
    renderIn(<DonePage />, '/onboarding/done', '/onboarding/done');
    expect(screen.getByRole('heading', { name: /Alles bereit/i })).toBeInTheDocument();
    expect(screen.getByText(/Luna/)).toBeInTheDocument();
  });

  it('does not contain sharp s', () => {
    renderIn(<DonePage />, '/done', '/done');
    expect(document.body.textContent).not.toContain('ß');
  });
});

// ── UC-002 pages ─────────────────────────────────────────────────────────────

const mockedSignInChild = vi.mocked(client.signInChild);
const mockedSignOutChild = vi.mocked(client.signOutChild);

describe('ChildSignInPage', () => {
  it('renders the sign-in form heading', () => {
    renderIn(<ChildSignInPage />, '/sign-in/child', '/sign-in/child');
    expect(screen.getByRole('heading', { name: /Als Kind anmelden/i })).toBeInTheDocument();
  });

  it('does not contain sharp s (NFR-I18N-004)', () => {
    renderIn(<ChildSignInPage />, '/sign-in/child', '/sign-in/child');
    expect(document.body.textContent).not.toContain('ß');
  });

  it('navigates to /child on successful sign-in', async () => {
    mockedSignInChild.mockResolvedValueOnce({
      sessionToken: 'tok',
      childId: 'c-1',
      role: 'CHILD',
      expiresAt: '2099-01-01T00:00:00Z',
    });
    const { default: userEvent } = await import('@testing-library/user-event');
    const user = userEvent.setup();
    renderIn(<ChildSignInPage />, '/sign-in/child', '/sign-in/child');
    await user.type(screen.getByLabelText(/Kind-ID/i), 'c-1');
    await user.type(screen.getByLabelText(/PIN/i), '1234');
    await user.click(screen.getByRole('button', { name: /Anmelden/i }));
    await waitFor(() =>
      expect(screen.getByTestId('navigated')).toBeInTheDocument(),
    );
    expect(sessionStorage.getItem('numnia_child_session_token')).toBe('tok');
    expect(sessionStorage.getItem('numnia_child_id')).toBe('c-1');
  });

  it('navigates to /sign-in/child/locked on 423', async () => {
    const { ApiError } = await import('../api/client');
    mockedSignInChild.mockRejectedValueOnce(
      new ApiError(423, 'PROFILE_LOCKED', 'Locked'),
    );
    const { default: userEvent } = await import('@testing-library/user-event');
    const user = userEvent.setup();
    renderIn(<ChildSignInPage />, '/sign-in/child', '/sign-in/child');
    await user.type(screen.getByLabelText(/Kind-ID/i), 'c-1');
    await user.type(screen.getByLabelText(/PIN/i), '0000');
    await user.click(screen.getByRole('button', { name: /Anmelden/i }));
    await waitFor(() =>
      expect(screen.getByTestId('navigated')).toBeInTheDocument(),
    );
  });
});

describe('ChildLockedPage', () => {
  it('renders locked message', () => {
    renderIn(<ChildLockedPage />, '/sign-in/child/locked', '/sign-in/child/locked');
    expect(screen.getByRole('heading', { name: /Profil gesperrt/i })).toBeInTheDocument();
  });

  it('does not contain sharp s', () => {
    renderIn(<ChildLockedPage />, '/sign-in/child/locked', '/sign-in/child/locked');
    expect(document.body.textContent).not.toContain('ß');
  });
});

describe('ChildShellPage', () => {
  it('renders child welcome heading', () => {
    sessionStorage.setItem('numnia_child_session_token', 'tok');
    renderIn(<ChildShellPage />, '/child', '/child');
    expect(
      screen.getByRole('heading', { name: /Willkommen/i }),
    ).toBeInTheDocument();
  });

  it('renders sign-out button', () => {
    sessionStorage.setItem('numnia_child_session_token', 'tok');
    renderIn(<ChildShellPage />, '/child', '/child');
    expect(screen.getByRole('button', { name: /Abmelden/i })).toBeInTheDocument();
  });

  it('navigates to /sign-in/child after sign-out', async () => {
    sessionStorage.setItem('numnia_child_session_token', 'tok');
    mockedSignOutChild.mockResolvedValueOnce(undefined);
    const { default: userEvent } = await import('@testing-library/user-event');
    const user = userEvent.setup();
    renderIn(<ChildShellPage />, '/child', '/child');
    await user.click(screen.getByRole('button', { name: /Abmelden/i }));
    await waitFor(() =>
      expect(screen.getByTestId('navigated')).toBeInTheDocument(),
    );
  });

  it('does not contain sharp s', () => {
    renderIn(<ChildShellPage />, '/child', '/child');
    expect(document.body.textContent).not.toContain('ß');
  });
});

describe('ParentDashboardPage', () => {
  it('renders parent greeting', () => {
    renderIn(<ParentDashboardPage />, '/parents/me', '/parents/me');
    expect(
      screen.getByRole('heading', { name: /Hallo, Elternteil/i }),
    ).toBeInTheDocument();
  });

  it('does not contain sharp s', () => {
    renderIn(<ParentDashboardPage />, '/parents/me', '/parents/me');
    expect(document.body.textContent).not.toContain('ß');
  });
});
