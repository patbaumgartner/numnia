/**
 * Step definitions for UC-005 — Child enters a world through a portal.
 *
 * Backend-driven, mirroring the UC-004 step style. The "Background" step
 * {@code Given an active child session} is reused from the UC-002 / UC-003
 * step files.
 *
 * Refs: FR-WORLD-001..005, NFR-PERF-002, NFR-A11Y-002, NFR-A11Y-003.
 */
import { Given, When, Then } from '@cucumber/cucumber';
import { expect } from '@playwright/test';
import { NumniaWorld, API_URL } from '../support/world';

interface WorldDto {
  id: string;
  displayName: string;
  difficultyLevel: number;
  requiredLevel: number;
}

interface PortalEntryDto {
  worldId: string;
  portalType: string;
  locked: boolean;
  target: string | null;
  messageCode: string | null;
  reducedMotion: boolean;
}

const MUSHROOM_JUNGLE = 'mushroom-jungle';

async function api(path: string, init?: RequestInit): Promise<Response> {
  const response = await fetch(`${API_URL}${path}`, init);
  if (!response.ok) {
    throw new Error(
      `${init?.method ?? 'GET'} ${path} failed: ${response.status} ${await response.text()}`,
    );
  }
  return response;
}

Given('three worlds are unlocked in Release 1', async function (this: NumniaWorld) {
  const res = await api('/api/worlds');
  const worlds = (await res.json()) as WorldDto[];
  expect(worlds).toHaveLength(3);
  this.scenarioData['worldsCount'] = String(worlds.length);
});

Given('the child has reached level S2', async function (this: NumniaWorld) {
  const childId =
    this.scenarioData['childId'] ??
    (await (async () => {
      const r = await api('/api/test/child-setup', { method: 'POST' });
      const body = (await r.json()) as { childId: string };
      this.scenarioData['childId'] = body.childId;
      return body.childId;
    })());
  await api(`/api/test/learning-progress?childId=${childId}&operation=ADDITION&difficulty=2&speed=1`, {
    method: 'POST',
  });
});

Given('the task pool of the world {string} is configured', async function (
  this: NumniaWorld,
  _worldName: string,
) {
  // Backed by InMemoryTaskPoolRepository which seeds the three R1 slugs by default.
  // No-op: the assertion happens implicitly when entering the portal.
});

When('the child enters the training portal of Mushroom Jungle', async function (
  this: NumniaWorld,
) {
  const childId = this.scenarioData['childId'];
  const res = await api(
    `/api/worlds/${MUSHROOM_JUNGLE}/portals/TRAINING/enter`,
    { method: 'POST', headers: { 'X-Child-Id': childId } },
  );
  this.scenarioData['lastPortal'] = await res.text();
});

Then('the system switches to the practice stage of the world', async function (
  this: NumniaWorld,
) {
  const entry = JSON.parse(this.scenarioData['lastPortal']) as PortalEntryDto;
  expect(entry.locked).toBe(false);
  expect(entry.target).toBe('PRACTICE_STAGE');
});

Given('the child has reduced-motion enabled', async function (this: NumniaWorld) {
  const childId =
    this.scenarioData['childId'] ??
    (await (async () => {
      const r = await api('/api/test/child-setup', { method: 'POST' });
      const body = (await r.json()) as { childId: string };
      this.scenarioData['childId'] = body.childId;
      return body.childId;
    })());
  await api(`/api/test/reduced-motion?childId=${childId}&enabled=true`, {
    method: 'POST',
  });
});

When('it enters a world', async function (this: NumniaWorld) {
  const childId = this.scenarioData['childId'];
  await api(`/api/test/learning-progress?childId=${childId}&operation=ADDITION&difficulty=1&speed=1`, {
    method: 'POST',
  });
  const res = await api(
    `/api/worlds/${MUSHROOM_JUNGLE}/portals/TRAINING/enter`,
    { method: 'POST', headers: { 'X-Child-Id': childId } },
  );
  this.scenarioData['lastPortal'] = await res.text();
});

Then(
  'particle effects and intense animations are reduced',
  async function (this: NumniaWorld) {
    const entry = JSON.parse(this.scenarioData['lastPortal']) as PortalEntryDto;
    expect(entry.reducedMotion).toBe(true);
  },
);

Given('a portal of type {string}', function (this: NumniaWorld, type: string) {
  this.scenarioData['portalType'] = type.toUpperCase();
});

When('the child taps on it in Release 1', async function (this: NumniaWorld) {
  const childId =
    this.scenarioData['childId'] ??
    (await (async () => {
      const r = await api('/api/test/child-setup', { method: 'POST' });
      const body = (await r.json()) as { childId: string };
      this.scenarioData['childId'] = body.childId;
      return body.childId;
    })());
  const portalType = this.scenarioData['portalType'];
  const res = await api(
    `/api/worlds/${MUSHROOM_JUNGLE}/portals/${portalType}/enter`,
    { method: 'POST', headers: { 'X-Child-Id': childId } },
  );
  this.scenarioData['lastPortal'] = await res.text();
});

Then('the system shows the notice {string}', function (
  this: NumniaWorld,
  notice: string,
) {
  expect(notice).toBe('coming later');
  const entry = JSON.parse(this.scenarioData['lastPortal']) as PortalEntryDto;
  expect(entry.messageCode).toBe('WORLD_PORTAL_COMING_LATER');
});

Then('the portal stays closed', function (this: NumniaWorld) {
  const entry = JSON.parse(this.scenarioData['lastPortal']) as PortalEntryDto;
  expect(entry.locked).toBe(true);
  expect(entry.target).toBeNull();
});
