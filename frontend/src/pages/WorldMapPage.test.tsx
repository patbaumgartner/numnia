/**
 * WorldMapPage — UC-005 child enters a world through a portal.
 *
 * Covers:
 *  - Sign-in gate when no childId is in session storage.
 *  - Lists exactly the three Release-1 worlds returned by the backend
 *    (BR-001).
 *  - Each world card surfaces difficulty hints (BR-003).
 *  - Locked R2 portals show "Kommt spaeter" (BR-001) and the page does not
 *    navigate when {@code locked === true}.
 *  - Reduced-motion preference returned by the backend is reflected on the
 *    DOM via a `reduced-motion` class (NFR-A11Y-003).
 *  - No sharp s in any UI copy (NFR-I18N-002, NFR-I18N-004).
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import userEvent from '@testing-library/user-event';
import WorldMapPage from './WorldMapPage';
import * as api from '../api/client';

const CHILD_ID = '33333333-3333-3333-3333-333333333333';

const SAMPLE_WORLDS = [
  {
    id: 'mushroom-jungle',
    displayName: 'Pilzdschungel',
    difficultyLevel: 1,
    requiredLevel: 1,
  },
  {
    id: 'crystal-cave',
    displayName: 'Kristallhoehle',
    difficultyLevel: 2,
    requiredLevel: 2,
  },
  {
    id: 'cloud-island',
    displayName: 'Wolkeninsel',
    difficultyLevel: 3,
    requiredLevel: 3,
  },
];

function renderWithRouter() {
  return render(
    <MemoryRouter initialEntries={['/worlds']}>
      <Routes>
        <Route path="/worlds" element={<WorldMapPage />} />
        <Route path="/training" element={<div data-testid="at-training" />} />
      </Routes>
    </MemoryRouter>,
  );
}

describe('WorldMapPage', () => {
  beforeEach(() => {
    sessionStorage.setItem('numnia_child_id', CHILD_ID);
  });

  afterEach(() => {
    sessionStorage.clear();
    vi.restoreAllMocks();
  });

  it('asks the child to sign in when no childId is in session storage', () => {
    sessionStorage.clear();
    renderWithRouter();
    expect(screen.getByText(/Bitte zuerst anmelden/)).toBeInTheDocument();
  });

  it('uses no sharp s in any copy', async () => {
    vi.spyOn(api, 'listWorlds').mockResolvedValue(SAMPLE_WORLDS);
    const { container } = renderWithRouter();
    await waitFor(() =>
      expect(screen.getByText('Pilzdschungel')).toBeInTheDocument(),
    );
    expect(container.textContent ?? '').not.toContain('ß');
  });

  it('renders exactly the three R1 worlds with difficulty hints (BR-001, BR-003)', async () => {
    vi.spyOn(api, 'listWorlds').mockResolvedValue(SAMPLE_WORLDS);
    renderWithRouter();
    await waitFor(() =>
      expect(screen.getByText('Pilzdschungel')).toBeInTheDocument(),
    );
    expect(screen.getByText('Kristallhoehle')).toBeInTheDocument();
    expect(screen.getByText('Wolkeninsel')).toBeInTheDocument();
    expect(screen.getByTestId('difficulty-mushroom-jungle').textContent).toMatch(
      /Stufe 1/,
    );
    expect(screen.getByTestId('difficulty-crystal-cave').textContent).toMatch(
      /Stufe 2/,
    );
  });

  it('navigates to the practice stage when the training portal opens (BR-002)', async () => {
    vi.spyOn(api, 'listWorlds').mockResolvedValue(SAMPLE_WORLDS);
    vi.spyOn(api, 'enterPortal').mockResolvedValue({
      worldId: 'mushroom-jungle',
      portalType: 'TRAINING',
      locked: false,
      target: 'PRACTICE_STAGE',
      messageCode: null,
      reducedMotion: false,
    });
    renderWithRouter();
    await waitFor(() =>
      expect(screen.getByTestId('enter-training-mushroom-jungle')).toBeInTheDocument(),
    );

    await userEvent.click(screen.getByTestId('enter-training-mushroom-jungle'));

    await waitFor(() =>
      expect(screen.getByTestId('at-training')).toBeInTheDocument(),
    );
  });

  it('shows "Kommt spaeter" and stays on the page when a Duel portal is tapped (BR-001)', async () => {
    vi.spyOn(api, 'listWorlds').mockResolvedValue(SAMPLE_WORLDS);
    vi.spyOn(api, 'enterPortal').mockResolvedValue({
      worldId: 'mushroom-jungle',
      portalType: 'DUEL',
      locked: true,
      target: null,
      messageCode: 'WORLD_PORTAL_COMING_LATER',
      reducedMotion: false,
    });
    renderWithRouter();
    await waitFor(() =>
      expect(screen.getByTestId('enter-DUEL-mushroom-jungle')).toBeInTheDocument(),
    );

    await userEvent.click(screen.getByTestId('enter-DUEL-mushroom-jungle'));

    await waitFor(() =>
      expect(screen.getByTestId('portal-message').textContent).toMatch(
        /Kommt spaeter/,
      ),
    );
    expect(screen.queryByTestId('at-training')).toBeNull();
  });

  it('applies reduced-motion class when the backend reports the preference (NFR-A11Y-003)', async () => {
    vi.spyOn(api, 'listWorlds').mockResolvedValue(SAMPLE_WORLDS);
    vi.spyOn(api, 'enterPortal').mockResolvedValue({
      worldId: 'mushroom-jungle',
      portalType: 'DUEL',
      locked: true,
      target: null,
      messageCode: 'WORLD_PORTAL_COMING_LATER',
      reducedMotion: true,
    });
    renderWithRouter();
    await waitFor(() =>
      expect(screen.getByTestId('enter-DUEL-mushroom-jungle')).toBeInTheDocument(),
    );

    await userEvent.click(screen.getByTestId('enter-DUEL-mushroom-jungle'));

    await waitFor(() =>
      expect(screen.getByTestId('world-map').className).toContain(
        'reduced-motion',
      ),
    );
  });
});
