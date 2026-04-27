/**
 * UC-011 — DeletionPage tests.
 *
 * Verifies sign-in gate, Swiss High German copy without sharp s,
 * trigger flow with password + DELETE word, auto-confirmation by token,
 * cool-off explanation, audit-info hint, and UC-010 cross-link.
 */
import { describe, expect, it, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import DeletionPage from './DeletionPage';
import * as client from '../api/client';
import type {
  DeletionRecordResponse,
  DeletionRequestSummary,
} from '../api/types';

const PARENT_ID = '00000000-0000-0000-0000-0000000000a1';
const CHILD_ID = '00000000-0000-0000-0000-0000000000b1';

function renderPage(initialPath = `/parents/deletion/${CHILD_ID}`) {
  return render(
    <MemoryRouter initialEntries={[initialPath]}>
      <Routes>
        <Route path="/parents/deletion/:childId" element={<DeletionPage />} />
        <Route
          path="/parents/exports/:childId"
          element={<div>EXPORT PAGE</div>}
        />
      </Routes>
    </MemoryRouter>,
  );
}

describe('DeletionPage (UC-011)', () => {
  beforeEach(() => {
    window.localStorage.setItem('parentId', PARENT_ID);
  });
  afterEach(() => {
    window.localStorage.clear();
    vi.restoreAllMocks();
  });

  it('shows the sign-in gate when no parentId is set', () => {
    window.localStorage.clear();
    renderPage();
    expect(
      screen.getByText(/Bitte zuerst als Elternteil anmelden/i),
    ).toBeInTheDocument();
  });

  it('renders Swiss High German copy without sharp s', () => {
    renderPage();
    const main = screen.getByRole('main');
    expect(main.textContent ?? '').not.toContain('ß');
    expect(
      screen.getByRole('heading', { name: /Konto loeschen/i }),
    ).toBeInTheDocument();
  });

  it('cross-links to the UC-010 data export', () => {
    renderPage();
    const link = screen.getByRole('link', { name: /Daten-Export/i });
    expect(link).toHaveAttribute('href', `/parents/exports/${CHILD_ID}`);
  });

  it('triggers a deletion with password + DELETE word and shows cool-off message', async () => {
    const user = userEvent.setup();
    const summary: DeletionRequestSummary = {
      id: '00000000-0000-0000-0000-000000000010',
      token: 'tok-deletion-1',
      signedUrlPath: '/api/parents/me/deletions/tok-deletion-1/confirm',
      expiresAt: new Date(Date.now() + 24 * 3600_000).toISOString(),
      status: 'PENDING',
    };
    const spy = vi
      .spyOn(client, 'requestChildDeletion')
      .mockResolvedValue(summary);

    renderPage();
    await user.type(
      screen.getByLabelText(/Eltern-Passwort/i),
      'E2eTestPass123',
    );
    await user.type(
      screen.getByLabelText(/Bestaetigung das Wort DELETE/i),
      'DELETE',
    );
    await user.click(screen.getByRole('button', { name: /Loeschung anfordern/i }));

    await waitFor(() => {
      expect(spy).toHaveBeenCalledWith(
        PARENT_ID,
        CHILD_ID,
        'E2eTestPass123',
        'DELETE',
      );
    });
    expect(
      screen.getByRole('heading', { name: /Bestaetigung per E-Mail/i }),
    ).toBeInTheDocument();
    expect(screen.getByText(/24 Stunden gueltig/i)).toBeInTheDocument();
  });

  it('shows an error when the trigger call fails', async () => {
    const user = userEvent.setup();
    vi.spyOn(client, 'requestChildDeletion').mockRejectedValue(
      new Error('401'),
    );

    renderPage();
    await user.type(
      screen.getByLabelText(/Eltern-Passwort/i),
      'wrong',
    );
    await user.type(
      screen.getByLabelText(/Bestaetigung das Wort DELETE/i),
      'DELETE',
    );
    await user.click(screen.getByRole('button', { name: /Loeschung anfordern/i }));

    await waitFor(() => {
      expect(screen.getByRole('alert')).toHaveTextContent(
        /Loeschung konnte nicht gestartet werden/i,
      );
    });
  });

  it('auto-confirms when ?token= is in the URL and shows the deletion record (BR-002)', async () => {
    const record: DeletionRecordResponse = {
      id: '00000000-0000-0000-0000-000000000010',
      childPseudonym: 'Luna',
      completedAt: '2026-04-27T18:00:00Z',
      dataCategories: ['child-profile', 'progress', 'avatar'],
    };
    const spy = vi
      .spyOn(client, 'confirmChildDeletion')
      .mockResolvedValue(record);

    renderPage(`/parents/deletion/${CHILD_ID}?token=tok-xyz`);
    await waitFor(() => {
      expect(spy).toHaveBeenCalledWith(PARENT_ID, 'tok-xyz');
    });
    expect(
      screen.getByRole('heading', { name: /Loeschung abgeschlossen/i }),
    ).toBeInTheDocument();
    expect(screen.getByText(/child-profile/)).toBeInTheDocument();
  });

  it('shows expired-link error when confirmation fails', async () => {
    vi.spyOn(client, 'confirmChildDeletion').mockRejectedValue(
      new Error('410'),
    );
    renderPage(`/parents/deletion/${CHILD_ID}?token=tok-expired`);
    await waitFor(() => {
      expect(screen.getByRole('alert')).toHaveTextContent(
        /abgelaufen oder bereits verwendet/i,
      );
    });
  });

  it('mentions audit logging (NFR-PRIV-002)', () => {
    renderPage();
    expect(
      screen.getByText(/sicher im Audit-Log protokolliert/i),
    ).toBeInTheDocument();
  });
});
