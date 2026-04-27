import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import ProgressPage from './ProgressPage';
import * as client from '../api/client';
import type { ProgressOverviewResponse } from '../api/types';

function overview(
  partial: Partial<ProgressOverviewResponse> = {},
): ProgressOverviewResponse {
  return {
    childId: 'c-1',
    palette: 'DEFAULT',
    empty: false,
    entries: [
      {
        operation: 'ADDITION',
        totalSessions: 3,
        totalTasks: 15,
        correctTasks: 12,
        accuracy: 0.8,
        masteryStatus: 'IN_CONSOLIDATION',
        currentDifficulty: 1,
      },
    ],
    ...partial,
  };
}

describe('ProgressPage (UC-008)', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    window.localStorage.clear();
  });

  function renderPage() {
    return render(
      <MemoryRouter>
        <ProgressPage />
      </MemoryRouter>,
    );
  }

  it('shows sign-in gate when no childId is set', () => {
    renderPage();
    expect(screen.getByText(/Bitte zuerst anmelden/)).toBeInTheDocument();
  });

  it('uses Swiss High German without sharp s', async () => {
    window.localStorage.setItem('childId', 'c-1');
    vi.spyOn(client, 'getProgress').mockResolvedValue(overview());
    const { container } = renderPage();
    await waitFor(() => screen.getByTestId('progress-list'));
    expect(container.textContent ?? '').not.toContain('ß');
    expect(container.textContent).toMatch(/Mein Fortschritt/);
  });

  it('renders one progress bar per operation with mastery marker (main 2)', async () => {
    window.localStorage.setItem('childId', 'c-1');
    vi.spyOn(client, 'getProgress').mockResolvedValue(
      overview({
        entries: [
          {
            operation: 'ADDITION',
            totalSessions: 3,
            totalTasks: 15,
            correctTasks: 12,
            accuracy: 0.8,
            masteryStatus: 'IN_CONSOLIDATION',
            currentDifficulty: 1,
          },
          {
            operation: 'MULTIPLICATION',
            totalSessions: 4,
            totalTasks: 20,
            correctTasks: 18,
            accuracy: 0.9,
            masteryStatus: 'MASTERED',
            currentDifficulty: 2,
          },
        ],
      }),
    );
    renderPage();
    await waitFor(() => screen.getByTestId('progress-list'));
    expect(screen.getByTestId('bar-ADDITION')).toBeInTheDocument();
    expect(screen.getByTestId('bar-MULTIPLICATION')).toBeInTheDocument();
    expect(screen.getByTestId('mastery-ADDITION')).toHaveTextContent(/Im Aufbau/);
    expect(screen.getByTestId('mastery-MULTIPLICATION')).toHaveTextContent(
      /Sicher beherrscht/,
    );
  });

  it('does not render any leaderboard or peer-comparison element (BR-002)', async () => {
    window.localStorage.setItem('childId', 'c-1');
    vi.spyOn(client, 'getProgress').mockResolvedValue(overview());
    renderPage();
    await waitFor(() => screen.getByTestId('progress-list'));
    expect(screen.queryByText(/Rangliste/i)).toBeNull();
    expect(screen.queryByText(/Bestenliste/i)).toBeNull();
    expect(screen.queryByText(/Vergleich mit/i)).toBeNull();
    expect(screen.queryByRole('table')).toBeNull();
  });

  it('shows empty hint when no data exists yet (alt 1a)', async () => {
    window.localStorage.setItem('childId', 'c-1');
    vi.spyOn(client, 'getProgress').mockResolvedValue(
      overview({ empty: true, entries: [] }),
    );
    renderPage();
    await waitFor(() => screen.getByTestId('empty-banner'));
    expect(screen.getByTestId('empty-banner')).toHaveTextContent(
      /Leg los und sammle deine ersten Sterne/,
    );
  });

  it('applies the deuteranopia palette class when backend reports it (alt 3a)', async () => {
    window.localStorage.setItem('childId', 'c-1');
    vi.spyOn(client, 'getProgress').mockResolvedValue(
      overview({ palette: 'DEUTERANOPIA' }),
    );
    renderPage();
    await waitFor(() => screen.getByTestId('progress-root'));
    expect(screen.getByTestId('progress-root')).toHaveClass('palette-deuteranopia');
  });

  it('changes palette when the child selects a color-blind profile', async () => {
    window.localStorage.setItem('childId', 'c-1');
    const getSpy = vi
      .spyOn(client, 'getProgress')
      .mockResolvedValueOnce(overview({ palette: 'DEFAULT' }))
      .mockResolvedValueOnce(overview({ palette: 'TRITANOPIA' }));
    const setSpy = vi
      .spyOn(client, 'setProgressPalette')
      .mockResolvedValue({ palette: 'TRITANOPIA' });
    renderPage();
    await waitFor(() => screen.getByTestId('palette-select'));
    fireEvent.change(screen.getByTestId('palette-select'), {
      target: { value: 'TRITANOPIA' },
    });
    await waitFor(() => expect(setSpy).toHaveBeenCalledWith('c-1', 'TRITANOPIA'));
    await waitFor(() =>
      expect(screen.getByTestId('progress-root')).toHaveClass('palette-tritanopia'),
    );
    expect(getSpy).toHaveBeenCalledTimes(2);
  });

  it('shows offline notice when backend is unreachable (exception 1x)', async () => {
    window.localStorage.setItem('childId', 'c-1');
    vi.spyOn(client, 'getProgress').mockRejectedValue(new Error('network'));
    renderPage();
    await waitFor(() => screen.getByTestId('error-banner'));
    expect(screen.getByTestId('error-banner')).toHaveTextContent(
      /Daten sind nicht aktuell/,
    );
  });
});
