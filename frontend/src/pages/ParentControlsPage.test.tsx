/**
 * UC-009 — ParentControlsPage tests.
 */
import { describe, expect, it, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import ParentControlsPage from './ParentControlsPage';
import * as client from '../api/client';

const PARENT_ID = '00000000-0000-0000-0000-0000000000a1';
const CHILD_ID = '00000000-0000-0000-0000-0000000000b1';

function renderPage() {
  return render(
    <MemoryRouter initialEntries={[`/parents/controls/${CHILD_ID}`]}>
      <Routes>
        <Route
          path="/parents/controls/:childId"
          element={<ParentControlsPage />}
        />
      </Routes>
    </MemoryRouter>,
  );
}

describe('ParentControlsPage (UC-009)', () => {
  beforeEach(() => {
    window.localStorage.setItem('parentId', PARENT_ID);
  });
  afterEach(() => {
    window.localStorage.clear();
    vi.restoreAllMocks();
  });

  it('renders defaults from server (FR-PAR-001..003)', async () => {
    vi.spyOn(client, 'getChildControls').mockResolvedValue({
      childId: CHILD_ID,
      parentId: PARENT_ID,
      dailyLimitMinutes: 30,
      breakRecommendationMinutes: 15,
      riskMechanicEnabled: false,
    });

    renderPage();

    expect(
      await screen.findByLabelText(/Taegliches Zeitlimit in Minuten/i),
    ).toHaveValue(30);
    expect(screen.getByLabelText(/Pause-Erinnerung in Minuten/i)).toHaveValue(15);
    expect(
      screen.getByRole('checkbox', { name: /Risiko-Modus aktivieren/i }),
    ).not.toBeChecked();
  });

  it('contains no sharp s (NFR-I18N-004)', async () => {
    vi.spyOn(client, 'getChildControls').mockResolvedValue({
      childId: CHILD_ID,
      parentId: PARENT_ID,
      dailyLimitMinutes: 30,
      breakRecommendationMinutes: 15,
      riskMechanicEnabled: false,
    });
    renderPage();
    await screen.findByRole('form', { name: /Eltern-Einstellungen/i });
    expect(document.body.textContent ?? '').not.toContain('ß');
  });

  it('saves a new limit value via PUT (BR-001)', async () => {
    const user = userEvent.setup();
    vi.spyOn(client, 'getChildControls').mockResolvedValue({
      childId: CHILD_ID,
      parentId: PARENT_ID,
      dailyLimitMinutes: 30,
      breakRecommendationMinutes: 15,
      riskMechanicEnabled: false,
    });
    const update = vi
      .spyOn(client, 'updateChildControls')
      .mockResolvedValue({
        childId: CHILD_ID,
        parentId: PARENT_ID,
        dailyLimitMinutes: 45,
        breakRecommendationMinutes: 15,
        riskMechanicEnabled: false,
      });

    renderPage();
    const input = await screen.findByLabelText(/Taegliches Zeitlimit in Minuten/i);
    await user.clear(input);
    await user.type(input, '45');
    await user.click(screen.getByRole('button', { name: /Speichern/i }));

    await waitFor(() => expect(update).toHaveBeenCalled());
    expect(update.mock.calls[0]).toEqual([
      PARENT_ID,
      CHILD_ID,
      {
        dailyLimitMinutes: 45,
        breakRecommendationMinutes: 15,
        riskMechanicEnabled: false,
        confirmNoLimit: false,
      },
    ]);
    expect(await screen.findByRole('status')).toHaveTextContent(
      /Einstellungen gespeichert/i,
    );
  });

  it('toggling risk on calls API with riskMechanicEnabled=true (BR-002)', async () => {
    const user = userEvent.setup();
    vi.spyOn(client, 'getChildControls').mockResolvedValue({
      childId: CHILD_ID,
      parentId: PARENT_ID,
      dailyLimitMinutes: 30,
      breakRecommendationMinutes: 15,
      riskMechanicEnabled: false,
    });
    const update = vi
      .spyOn(client, 'updateChildControls')
      .mockResolvedValue({
        childId: CHILD_ID,
        parentId: PARENT_ID,
        dailyLimitMinutes: 30,
        breakRecommendationMinutes: 15,
        riskMechanicEnabled: true,
      });

    renderPage();
    const cb = await screen.findByRole('checkbox', {
      name: /Risiko-Modus aktivieren/i,
    });
    await user.click(cb);
    await user.click(screen.getByRole('button', { name: /Speichern/i }));

    await waitFor(() => expect(update).toHaveBeenCalled());
    expect(update.mock.calls[0][2].riskMechanicEnabled).toBe(true);
  });

  it('explains that risk mechanic causes no permanent loss (BR-003)', async () => {
    vi.spyOn(client, 'getChildControls').mockResolvedValue({
      childId: CHILD_ID,
      parentId: PARENT_ID,
      dailyLimitMinutes: 30,
      breakRecommendationMinutes: 15,
      riskMechanicEnabled: false,
    });
    renderPage();
    expect(
      await screen.findByText(/nie dauerhaft verloren/i),
    ).toBeInTheDocument();
  });

  it('selecting "Kein Limit" requires confirmation (alt 3a)', async () => {
    const user = userEvent.setup();
    vi.spyOn(client, 'getChildControls').mockResolvedValue({
      childId: CHILD_ID,
      parentId: PARENT_ID,
      dailyLimitMinutes: 30,
      breakRecommendationMinutes: 15,
      riskMechanicEnabled: false,
    });
    const update = vi
      .spyOn(client, 'updateChildControls')
      .mockResolvedValue({
        childId: CHILD_ID,
        parentId: PARENT_ID,
        dailyLimitMinutes: null,
        breakRecommendationMinutes: 15,
        riskMechanicEnabled: false,
      });

    renderPage();
    await screen.findByLabelText(/Taegliches Zeitlimit in Minuten/i);
    await user.click(screen.getByLabelText(/Kein Limit/i));
    await user.click(screen.getByRole('button', { name: /Speichern/i }));

    const dialog = await screen.findByRole('alertdialog');
    expect(dialog).toBeInTheDocument();
    expect(update).not.toHaveBeenCalled();

    await user.click(within(dialog).getByRole('button', { name: /Ja, kein Limit/i }));
    await waitFor(() => expect(update).toHaveBeenCalled());
    expect(update.mock.calls[0][2].confirmNoLimit).toBe(true);
    expect(update.mock.calls[0][2].dailyLimitMinutes).toBeNull();
  });

  it('mentions the audit log so the parent knows changes are logged (BR-004)', async () => {
    vi.spyOn(client, 'getChildControls').mockResolvedValue({
      childId: CHILD_ID,
      parentId: PARENT_ID,
      dailyLimitMinutes: 30,
      breakRecommendationMinutes: 15,
      riskMechanicEnabled: false,
    });
    renderPage();
    expect(await screen.findByText(/Audit-Log/i)).toBeInTheDocument();
  });

  it('shows an error if not signed in as parent', async () => {
    window.localStorage.clear();
    renderPage();
    expect(
      await screen.findByText(/zuerst als Elternteil anmelden/i),
    ).toBeInTheDocument();
  });
});
