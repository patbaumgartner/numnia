/**
 * GalleryPage — UC-006 child unlocks creature and picks companion.
 *
 * Covers:
 *  - Sign-in gate when no childId is in session storage.
 *  - Auto-process unlocks on mount; new creatures show in an unlock banner.
 *  - Locked vs unlocked rendering; locked entries cannot be picked.
 *  - Picking an unlocked creature as companion calls the backend and
 *    refreshes the gallery (BR-003, swap any time).
 *  - Consolation banner appears when backend reports {@code consolationAwarded}.
 *  - No sharp s in any UI copy (NFR-I18N-002, NFR-I18N-004).
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import GalleryPage from './GalleryPage';
import * as api from '../api/client';

const CHILD_ID = '44444444-4444-4444-4444-444444444444';

const EMPTY_UNLOCK = {
  newlyUnlocked: [],
  consolationAwarded: false,
  starPointsAwarded: 0,
};

const PILZAR = {
  id: 'pilzar',
  displayName: 'Pilzar',
  operation: 'ADDITION' as const,
  sourceWorldId: 'mushroom-jungle',
};

const WELLENO = {
  id: 'welleno',
  displayName: 'Welleno',
  operation: 'MULTIPLICATION' as const,
  sourceWorldId: 'cloud-island',
};

const ZACKA = {
  id: 'zacka',
  displayName: 'Zacka',
  operation: 'SUBTRACTION' as const,
  sourceWorldId: 'crystal-cave',
};

describe('GalleryPage', () => {
  beforeEach(() => {
    sessionStorage.setItem('numnia_child_id', CHILD_ID);
  });

  afterEach(() => {
    sessionStorage.clear();
    vi.restoreAllMocks();
  });

  it('shows sign-in gate when no childId is stored', () => {
    sessionStorage.clear();
    render(<GalleryPage />);
    expect(screen.getByText(/Bitte zuerst anmelden/i)).toBeInTheDocument();
  });

  it('uses no sharp s in any UI copy (NFR-I18N-004)', async () => {
    vi.spyOn(api, 'processCreatureUnlocks').mockResolvedValue(EMPTY_UNLOCK);
    vi.spyOn(api, 'getCreatureGallery').mockResolvedValue({
      entries: [
        { ...PILZAR, unlocked: false, isCompanion: false },
        { ...WELLENO, unlocked: false, isCompanion: false },
        { ...ZACKA, unlocked: false, isCompanion: false },
      ],
      companion: null,
    });
    const { container } = render(<GalleryPage />);
    await waitFor(() => screen.getByTestId('gallery-list'));
    expect(container.textContent).not.toContain('ß');
  });

  it('renders locked + unlocked creatures from backend', async () => {
    vi.spyOn(api, 'processCreatureUnlocks').mockResolvedValue(EMPTY_UNLOCK);
    vi.spyOn(api, 'getCreatureGallery').mockResolvedValue({
      entries: [
        { ...PILZAR, unlocked: true, isCompanion: false },
        { ...WELLENO, unlocked: false, isCompanion: false },
        { ...ZACKA, unlocked: false, isCompanion: false },
      ],
      companion: null,
    });

    render(<GalleryPage />);
    await waitFor(() => screen.getByTestId('gallery-list'));

    expect(screen.getByTestId('status-pilzar').textContent)
      .toMatch(/Freigeschaltet/);
    expect(screen.getByTestId('status-welleno').textContent)
      .toMatch(/Noch nicht freigeschaltet/);
    expect(screen.getByTestId('pick-welleno')).toBeDisabled();
    expect(screen.getByTestId('pick-pilzar')).not.toBeDisabled();
  });

  it('shows the unlock banner with newly unlocked creature names', async () => {
    vi.spyOn(api, 'processCreatureUnlocks').mockResolvedValue({
      newlyUnlocked: [PILZAR],
      consolationAwarded: false,
      starPointsAwarded: 0,
    });
    vi.spyOn(api, 'getCreatureGallery').mockResolvedValue({
      entries: [
        { ...PILZAR, unlocked: true, isCompanion: false },
        { ...WELLENO, unlocked: false, isCompanion: false },
        { ...ZACKA, unlocked: false, isCompanion: false },
      ],
      companion: null,
    });

    render(<GalleryPage />);
    const banner = await screen.findByTestId('unlock-banner');
    expect(banner.textContent).toMatch(/Pilzar/);
  });

  it('picks an unlocked creature as companion and refreshes gallery (BR-003)', async () => {
    const user = userEvent.setup();
    vi.spyOn(api, 'processCreatureUnlocks').mockResolvedValue(EMPTY_UNLOCK);
    const pickSpy = vi.spyOn(api, 'pickCompanion').mockResolvedValue({
      companion: 'pilzar',
    });
    const gallerySpy = vi
      .spyOn(api, 'getCreatureGallery')
      .mockResolvedValueOnce({
        entries: [
          { ...PILZAR, unlocked: true, isCompanion: false },
          { ...WELLENO, unlocked: false, isCompanion: false },
          { ...ZACKA, unlocked: false, isCompanion: false },
        ],
        companion: null,
      })
      .mockResolvedValueOnce({
        entries: [
          { ...PILZAR, unlocked: true, isCompanion: true },
          { ...WELLENO, unlocked: false, isCompanion: false },
          { ...ZACKA, unlocked: false, isCompanion: false },
        ],
        companion: 'pilzar',
      });

    render(<GalleryPage />);
    await waitFor(() => screen.getByTestId('gallery-list'));

    await user.click(screen.getByTestId('pick-pilzar'));

    await waitFor(() =>
      expect(screen.getByTestId('companion-badge-pilzar')).toBeInTheDocument(),
    );
    expect(pickSpy).toHaveBeenCalledWith(CHILD_ID, 'pilzar');
    expect(gallerySpy).toHaveBeenCalledTimes(2);
  });

  it('shows the consolation banner when backend reports it (alt 1a)', async () => {
    vi.spyOn(api, 'processCreatureUnlocks').mockResolvedValue({
      newlyUnlocked: [],
      consolationAwarded: true,
      starPointsAwarded: 50,
    });
    vi.spyOn(api, 'getCreatureGallery').mockResolvedValue({
      entries: [
        { ...PILZAR, unlocked: true, isCompanion: true },
        { ...WELLENO, unlocked: true, isCompanion: false },
        { ...ZACKA, unlocked: true, isCompanion: false },
      ],
      companion: 'pilzar',
    });

    render(<GalleryPage />);
    const banner = await screen.findByTestId('consolation-banner');
    expect(banner.textContent).toMatch(/50 Sternenpunkte/);
  });
});
