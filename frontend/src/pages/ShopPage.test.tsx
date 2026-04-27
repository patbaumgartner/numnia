/**
 * ShopPage tests — UC-007 child buys items with star points.
 *
 * Covers:
 *  - Sign-in gate when no childId is in session storage.
 *  - Loads catalog + balance on mount; renders price list and balance.
 *  - Successful purchase shows Swiss German confirmation; balance updates.
 *  - Insufficient star points shows the BR-001 notice; no booking takes place.
 *  - Duplicate purchase shows a Swiss German notice (alt: already owned).
 *  - No sharp s in any UI copy (NFR-I18N-002, NFR-I18N-004).
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import ShopPage from './ShopPage';
import * as api from '../api/client';
import { ApiError } from '../api/client';

const CHILD_ID = '55555555-5555-5555-5555-555555555555';

const STAR_CAP = {
  id: 'star-cap',
  displayName: 'Sternenmuetze',
  priceStarPoints: 30,
  slot: 'HEAD',
};

const MOON_CAPE = {
  id: 'moon-cape',
  displayName: 'Mondumhang',
  priceStarPoints: 50,
  slot: 'BODY',
};

const BASE_AVATAR = {
  childId: CHILD_ID,
  baseModel: 'avatar-fox',
  equipped: {} as Record<string, string>,
  starPointsBalance: 50,
};

describe('ShopPage', () => {
  beforeEach(() => {
    sessionStorage.setItem('numnia_child_id', CHILD_ID);
  });

  afterEach(() => {
    sessionStorage.clear();
    vi.restoreAllMocks();
  });

  it('shows sign-in gate when no childId is stored', () => {
    sessionStorage.clear();
    render(<ShopPage />);
    expect(screen.getByText(/Bitte zuerst anmelden/i)).toBeInTheDocument();
  });

  it('uses no sharp s in any UI copy (NFR-I18N-004)', async () => {
    vi.spyOn(api, 'listShopItems').mockResolvedValue({
      items: [STAR_CAP, MOON_CAPE],
    });
    vi.spyOn(api, 'getAvatar').mockResolvedValue(BASE_AVATAR);
    const { container } = render(<ShopPage />);
    await waitFor(() => screen.getByTestId('shop-list'));
    expect(container.textContent).not.toContain('ß');
  });

  it('renders shop catalog with prices and balance', async () => {
    vi.spyOn(api, 'listShopItems').mockResolvedValue({
      items: [STAR_CAP, MOON_CAPE],
    });
    vi.spyOn(api, 'getAvatar').mockResolvedValue(BASE_AVATAR);

    render(<ShopPage />);
    await waitFor(() => screen.getByTestId('shop-list'));

    expect(screen.getByTestId('balance').textContent).toMatch(/50 Sternenpunkte/);
    expect(screen.getByTestId('price-star-cap').textContent).toMatch(
      /30 Sternenpunkte/,
    );
    expect(screen.getByTestId('price-moon-cape').textContent).toMatch(
      /50 Sternenpunkte/,
    );
  });

  it('purchases an item and updates the balance (BR-002)', async () => {
    const user = userEvent.setup();
    vi.spyOn(api, 'listShopItems').mockResolvedValue({ items: [STAR_CAP] });
    vi.spyOn(api, 'getAvatar').mockResolvedValue(BASE_AVATAR);
    const purchaseSpy = vi.spyOn(api, 'purchaseShopItem').mockResolvedValue({
      itemId: 'star-cap',
      starPointsBalance: 20,
    });

    render(<ShopPage />);
    await waitFor(() => screen.getByTestId('shop-list'));

    await user.click(screen.getByTestId('buy-star-cap'));

    await waitFor(() =>
      expect(screen.getByTestId('purchase-success')).toBeInTheDocument(),
    );
    expect(purchaseSpy).toHaveBeenCalledWith(CHILD_ID, 'star-cap');
    expect(screen.getByTestId('balance').textContent).toMatch(/20 Sternenpunkte/);
  });

  it('shows the BR-001 notice on insufficient star points (alt 4a)', async () => {
    const user = userEvent.setup();
    vi.spyOn(api, 'listShopItems').mockResolvedValue({ items: [STAR_CAP] });
    vi.spyOn(api, 'getAvatar').mockResolvedValue({
      ...BASE_AVATAR,
      starPointsBalance: 10,
    });
    vi.spyOn(api, 'purchaseShopItem').mockRejectedValue(
      new ApiError(409, 'INSUFFICIENT_STAR_POINTS', 'not enough'),
    );

    render(<ShopPage />);
    await waitFor(() => screen.getByTestId('shop-list'));
    await user.click(screen.getByTestId('buy-star-cap'));

    const error = await screen.findByTestId('purchase-error');
    expect(error.textContent).toMatch(/Sammle noch mehr Sternenpunkte/);
    // Balance unchanged (no booking takes place)
    expect(screen.getByTestId('balance').textContent).toMatch(/10 Sternenpunkte/);
  });

  it('shows the duplicate notice when item already owned', async () => {
    const user = userEvent.setup();
    vi.spyOn(api, 'listShopItems').mockResolvedValue({ items: [STAR_CAP] });
    vi.spyOn(api, 'getAvatar').mockResolvedValue(BASE_AVATAR);
    vi.spyOn(api, 'purchaseShopItem').mockRejectedValue(
      new ApiError(409, 'ALREADY_IN_INVENTORY', 'dup'),
    );

    render(<ShopPage />);
    await waitFor(() => screen.getByTestId('shop-list'));
    await user.click(screen.getByTestId('buy-star-cap'));

    const error = await screen.findByTestId('purchase-error');
    expect(error.textContent).toMatch(/bereits in deinem Inventar/);
  });
});
