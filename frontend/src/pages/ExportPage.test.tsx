/**
 * UC-010 — ExportPage tests.
 *
 * Verifies sign-in gate, Swiss High German copy without sharp s,
 * format selection, signed-link rendering, and audit-info hint.
 */
import { describe, expect, it, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import ExportPage from './ExportPage';
import * as client from '../api/client';
import type { ExportSummaryResponse } from '../api/types';

const PARENT_ID = '00000000-0000-0000-0000-0000000000a1';
const CHILD_ID = '00000000-0000-0000-0000-0000000000b1';

function renderPage() {
  return render(
    <MemoryRouter initialEntries={[`/parents/exports/${CHILD_ID}`]}>
      <Routes>
        <Route path="/parents/exports/:childId" element={<ExportPage />} />
      </Routes>
    </MemoryRouter>,
  );
}

describe('ExportPage (UC-010)', () => {
  beforeEach(() => {
    window.localStorage.setItem('parentId', PARENT_ID);
  });
  afterEach(() => {
    window.localStorage.clear();
    vi.restoreAllMocks();
  });

  it('sign-in gate when no parentId is set', () => {
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
      screen.getByRole('heading', { name: /Daten-Export/i }),
    ).toBeInTheDocument();
  });

  it('triggers a JSON export and shows signed download link (BR-002)', async () => {
    const user = userEvent.setup();
    const triggered: ExportSummaryResponse = {
      id: '00000000-0000-0000-0000-000000000010',
      childId: CHILD_ID,
      format: 'JSON',
      token: 'tok-json-1',
      signedUrlPath: '/api/parents/me/exports/tok-json-1',
      createdAt: '2026-04-28T10:00:00Z',
      expiresAt: '2026-05-05T10:00:00Z',
      size: 4096,
    };
    const spy = vi
      .spyOn(client, 'triggerExport')
      .mockResolvedValue([triggered]);

    renderPage();
    await user.click(screen.getByRole('radio', { name: /JSON/i }));
    await user.click(
      screen.getByRole('button', { name: /Export erstellen/i }),
    );

    await waitFor(() =>
      expect(spy).toHaveBeenCalledWith(PARENT_ID, CHILD_ID, 'JSON'),
    );
    const link = await screen.findByRole('link', {
      name: /Herunterladen/i,
    });
    expect(link).toHaveAttribute('href', triggered.signedUrlPath);
    expect(link).toHaveAttribute(
      'href',
      expect.stringContaining('/api/parents/me/exports/'),
    );
  });

  it('shows the deadline hint with 7-day expiry (BR-004)', async () => {
    const user = userEvent.setup();
    vi.spyOn(client, 'triggerExport').mockResolvedValue([
      {
        id: '00000000-0000-0000-0000-000000000011',
        childId: CHILD_ID,
        format: 'PDF',
        token: 'tok-pdf-1',
        signedUrlPath: '/api/parents/me/exports/tok-pdf-1',
        createdAt: '2026-04-28T10:00:00Z',
        expiresAt: '2026-05-05T10:00:00Z',
        size: 2048,
      },
    ]);

    renderPage();
    await user.click(screen.getByRole('radio', { name: /PDF/i }));
    await user.click(
      screen.getByRole('button', { name: /Export erstellen/i }),
    );

    await waitFor(() =>
      expect(screen.getByText(/7 Tage/i)).toBeInTheDocument(),
    );
  });

  it('renders both files when BOTH format is requested', async () => {
    const user = userEvent.setup();
    vi.spyOn(client, 'triggerExport').mockResolvedValue([
      {
        id: 'aa',
        childId: CHILD_ID,
        format: 'JSON',
        token: 'tj',
        signedUrlPath: '/api/parents/me/exports/tj',
        createdAt: '2026-04-28T10:00:00Z',
        expiresAt: '2026-05-05T10:00:00Z',
        size: 1024,
      },
      {
        id: 'bb',
        childId: CHILD_ID,
        format: 'PDF',
        token: 'tp',
        signedUrlPath: '/api/parents/me/exports/tp',
        createdAt: '2026-04-28T10:00:00Z',
        expiresAt: '2026-05-05T10:00:00Z',
        size: 2048,
      },
    ]);

    renderPage();
    await user.click(screen.getByRole('radio', { name: /Beide/i }));
    await user.click(
      screen.getByRole('button', { name: /Export erstellen/i }),
    );

    const links = await screen.findAllByRole('link', { name: /Herunterladen/i });
    expect(links).toHaveLength(2);
  });

  it('shows an error when the request fails', async () => {
    const user = userEvent.setup();
    vi.spyOn(client, 'triggerExport').mockRejectedValue(
      new Error('network down'),
    );

    renderPage();
    await user.click(screen.getByRole('radio', { name: /JSON/i }));
    await user.click(
      screen.getByRole('button', { name: /Export erstellen/i }),
    );

    expect(
      await screen.findByText(/Export konnte nicht erstellt werden/i),
    ).toBeInTheDocument();
  });

  it('mentions audit logging (BR-003)', () => {
    renderPage();
    expect(
      screen.getByText(/Audit-Log|protokolliert/i),
    ).toBeInTheDocument();
  });
});
