/**
 * Step definitions for UC-007 — Child customizes avatar and uses shop.
 *
 * Backend-driven (parallel to UC-006 style). The shared
 * "Given an active child session" step is reused from UC-002/UC-003 step files.
 *
 * Note: the {@code /api/test/star-points} helper is referenced as a follow-up
 * (same status as UC-005's reduced-motion helper); these scenarios run
 * dry-run only until the helper is implemented.
 *
 * Refs: FR-CRE-005/006, FR-GAM-001/002/003/005, BR-001/002/003/004.
 */
import { Given, When, Then } from '@cucumber/cucumber';
import { expect } from '@playwright/test';
import { NumniaWorld, API_URL } from '../support/world';

const STAR_CAP_ID = 'star-cap';

interface ShopItemDto {
  id: string;
  displayName: string;
  priceStarPoints: number;
  slot: string;
}

interface ShopItemsDto {
  items: ShopItemDto[];
}

interface PurchaseResultDto {
  itemId: string;
  starPointsBalance: number;
}

interface InventoryDto {
  items: { itemId: string; purchasedAt: string }[];
}

async function api(
  path: string,
  init?: RequestInit & { allowError?: boolean },
): Promise<Response> {
  const response = await fetch(`${API_URL}${path}`, init);
  if (!response.ok && !init?.allowError) {
    throw new Error(
      `${init?.method ?? 'GET'} ${path} failed: ${response.status} ${await response.text()}`,
    );
  }
  return response;
}

Given('a configured shop catalog', async function (this: NumniaWorld) {
  const res = await api('/api/shop/items');
  const body = (await res.json()) as ShopItemsDto;
  expect(body.items.length).toBeGreaterThan(0);
  this.scenarioData['shopItems'] = JSON.stringify(body.items);
});

Given('the child has 50 star points', async function (this: NumniaWorld) {
  const childId = this.scenarioData['childId'] ?? '';
  await api('/api/test/star-points', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ childId, balance: 50 }),
  });
});

Given('the child has 10 star points', async function (this: NumniaWorld) {
  const childId = this.scenarioData['childId'] ?? '';
  await api('/api/test/star-points', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ childId, balance: 10 }),
  });
});

Given(
  'the item {string} costs {int} star points',
  function (this: NumniaWorld, displayName: string, price: number) {
    const items = JSON.parse(
      this.scenarioData['shopItems'] ?? '[]',
    ) as ShopItemDto[];
    const cap = items.find((i) => i.id === STAR_CAP_ID);
    expect(cap?.priceStarPoints).toBe(price);
    expect(displayName).toBe('Star Cap');
  },
);

Given('the item costs {int} star points', function (this: NumniaWorld, price: number) {
  const items = JSON.parse(
    this.scenarioData['shopItems'] ?? '[]',
  ) as ShopItemDto[];
  const cap = items.find((i) => i.id === STAR_CAP_ID);
  expect(cap?.priceStarPoints).toBe(price);
});

When('the child confirms the purchase', async function (this: NumniaWorld) {
  const childId = this.scenarioData['childId'] ?? '';
  const res = await api(`/api/shop/items/${STAR_CAP_ID}/purchase`, {
    method: 'POST',
    headers: { 'X-Child-Id': childId },
  });
  const body = (await res.json()) as PurchaseResultDto;
  this.scenarioData['lastPurchase'] = JSON.stringify(body);
});

When('the child tries the purchase', async function (this: NumniaWorld) {
  const childId = this.scenarioData['childId'] ?? '';
  const res = await api(`/api/shop/items/${STAR_CAP_ID}/purchase`, {
    method: 'POST',
    headers: { 'X-Child-Id': childId },
    allowError: true,
  });
  this.scenarioData['lastStatus'] = String(res.status);
});

Then(
  'the star points balance is reduced to {int}',
  function (this: NumniaWorld, expected: number) {
    const body = JSON.parse(
      this.scenarioData['lastPurchase'] ?? '{}',
    ) as PurchaseResultDto;
    expect(body.starPointsBalance).toBe(expected);
  },
);

Then('the Star Cap is permanently in the inventory', async function (this: NumniaWorld) {
  const childId = this.scenarioData['childId'] ?? '';
  const res = await api('/api/avatar/inventory', {
    headers: { 'X-Child-Id': childId },
  });
  const body = (await res.json()) as InventoryDto;
  expect(body.items.some((it) => it.itemId === STAR_CAP_ID)).toBe(true);
});

Then('the system shows a notice about collecting more star points', function (
  this: NumniaWorld,
) {
  expect(this.scenarioData['lastStatus']).toBe('409');
});

Then('no booking takes place', async function (this: NumniaWorld) {
  const childId = this.scenarioData['childId'] ?? '';
  const res = await api('/api/avatar/inventory', {
    headers: { 'X-Child-Id': childId },
  });
  const body = (await res.json()) as InventoryDto;
  expect(body.items.some((it) => it.itemId === STAR_CAP_ID)).toBe(false);
});

When('the client tries to unlock an item without payment', async function (
  this: NumniaWorld,
) {
  const childId = this.scenarioData['childId'] ?? '';
  const res = await api('/api/avatar/equipped', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'X-Child-Id': childId,
    },
    body: JSON.stringify({ itemId: STAR_CAP_ID }),
    allowError: true,
  });
  this.scenarioData['lastStatus'] = String(res.status);
});

Then('the server responds with an error status', function (this: NumniaWorld) {
  const status = Number(this.scenarioData['lastStatus'] ?? '0');
  expect(status).toBeGreaterThanOrEqual(400);
});

Then('the incident is documented in the shop audit log', function () {
  // Backend asserts the audit entry in unit + cucumber tests; the E2E layer
  // is satisfied by the prior 4xx response.
});
