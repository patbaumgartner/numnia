/**
 * AvatarPage tests — UC-007 child customizes avatar.
 *
 * Covers:
 *  - Sign-in gate when no childId is in session storage.
 *  - Loads avatar + inventory on mount; renders base model and inventory.
 *  - Empty inventory shows a Swiss German hint.
 *  - Changing the base model calls the backend and updates state.
 *  - Equipping an inventory item updates the equipped slots.
 *  - No sharp s in any UI copy (NFR-I18N-002, NFR-I18N-004).
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import AvatarPage from './AvatarPage';
import * as api from '../api/client';

const CHILD_ID = '66666666-6666-6666-6666-666666666666';

const BASE_AVATAR = {
  childId: CHILD_ID,
  baseModel: 'avatar-fox',
  equipped: {} as Record<string, string>,
  starPointsBalance: 100,
};

function renderPage() {
  return render(
    <MemoryRouter>
      <AvatarPage />
    </MemoryRouter>,
  );
}

describe('AvatarPage', () => {
  beforeEach(() => {
    sessionStorage.setItem('numnia_child_id', CHILD_ID);
  });

  afterEach(() => {
    sessionStorage.clear();
    vi.restoreAllMocks();
  });

  it('shows sign-in gate when no childId is stored', () => {
    sessionStorage.clear();
    renderPage();
    expect(screen.getByText(/Bitte zuerst anmelden/i)).toBeInTheDocument();
  });

  it('uses no sharp s in any UI copy (NFR-I18N-004)', async () => {
    vi.spyOn(api, 'getAvatar').mockResolvedValue(BASE_AVATAR);
    vi.spyOn(api, 'getInventory').mockResolvedValue({ items: [] });
    const { container } = renderPage();
    await waitFor(() => screen.getByTestId('base-model'));
    expect(container.textContent).not.toContain('ß');
  });

  it('renders the current base model', async () => {
    vi.spyOn(api, 'getAvatar').mockResolvedValue(BASE_AVATAR);
    vi.spyOn(api, 'getInventory').mockResolvedValue({ items: [] });
    renderPage();
    await waitFor(() =>
      expect(screen.getByTestId('base-model').textContent).toMatch(/avatar-fox/),
    );
  });

  it('shows empty-inventory hint when nothing is owned', async () => {
    vi.spyOn(api, 'getAvatar').mockResolvedValue(BASE_AVATAR);
    vi.spyOn(api, 'getInventory').mockResolvedValue({ items: [] });
    renderPage();
    const hint = await screen.findByTestId('inventory-empty');
    expect(hint.textContent).toMatch(/Shop/);
  });

  it('changes the base model when a different avatar is chosen', async () => {
    const user = userEvent.setup();
    vi.spyOn(api, 'getAvatar').mockResolvedValue(BASE_AVATAR);
    vi.spyOn(api, 'getInventory').mockResolvedValue({ items: [] });
    const setSpy = vi.spyOn(api, 'setAvatarBaseModel').mockResolvedValue({
      baseModel: 'avatar-owl',
      equipped: {},
    });

    renderPage();
    await waitFor(() => screen.getByTestId('base-model'));
    await user.click(screen.getByTestId('base-avatar-owl'));

    expect(setSpy).toHaveBeenCalledWith(CHILD_ID, 'avatar-owl');
    await waitFor(() =>
      expect(screen.getByTestId('base-model').textContent).toMatch(/avatar-owl/),
    );
  });

  it('lists inventory items and equips them on click', async () => {
    const user = userEvent.setup();
    vi.spyOn(api, 'getAvatar').mockResolvedValue(BASE_AVATAR);
    vi.spyOn(api, 'getInventory').mockResolvedValue({
      items: [{ itemId: 'star-cap', purchasedAt: '2026-04-27T00:00:00Z' }],
    });
    const equipSpy = vi.spyOn(api, 'equipAvatarItem').mockResolvedValue({
      baseModel: 'avatar-fox',
      equipped: { HEAD: 'star-cap' },
    });

    renderPage();
    await waitFor(() => screen.getByTestId('inventory-list'));
    await user.click(screen.getByTestId('equip-star-cap'));

    expect(equipSpy).toHaveBeenCalledWith(CHILD_ID, 'star-cap');
  });
});
