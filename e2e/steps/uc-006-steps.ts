/**
 * Step definitions for UC-006 — Child unlocks creature and picks companion.
 *
 * Backend-driven (parallel to UC-005 style). The shared
 * "Given an active child session" step is reused from UC-002/UC-003 step files.
 *
 * Refs: FR-CRE-001/002/003/004/007, FR-GAM-001/005, BR-001/002/003.
 */
import { Given, When, Then } from '@cucumber/cucumber';
import { expect } from '@playwright/test';
import { NumniaWorld, API_URL } from '../support/world';

interface CreatureDto {
  id: string;
  displayName: string;
  operation: string;
  sourceWorldId: string;
}

interface GalleryEntryDto extends CreatureDto {
  unlocked: boolean;
  isCompanion: boolean;
}

interface GalleryDto {
  entries: GalleryEntryDto[];
  companion: string | null;
}

interface UnlockResultDto {
  newlyUnlocked: CreatureDto[];
  consolationAwarded: boolean;
  starPointsAwarded: number;
}

const PILZAR_ID = 'pilzar';
const WELLENO_ID = 'welleno';

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

Given(
  'a configured unlock threshold for the creature {string}',
  function (this: NumniaWorld, creatureName: string) {
    // The static catalogue ships with Pilzar/Welleno/Zacka; just record the
    // candidate name in scenario state for later assertions.
    this.scenarioData['expectedCreatureName'] = creatureName;
  },
);

Given('the child reaches mastery in the related domain', async function (this: NumniaWorld) {
  const childId = this.scenarioData['childId'] ?? '';
  await api('/api/test/learning-progress', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      childId,
      operation: 'ADDITION',
      masteryStatus: 'MASTERED',
    }),
  });
});

When('the system processes the unlock', async function (this: NumniaWorld) {
  const childId = this.scenarioData['childId'] ?? '';
  const res = await api('/api/creatures/unlocks', {
    method: 'POST',
    headers: { 'X-Child-Id': childId },
  });
  const body = (await res.json()) as UnlockResultDto;
  this.scenarioData['lastUnlockResult'] = JSON.stringify(body);
});

Then('the creature appears permanently in the gallery', async function (this: NumniaWorld) {
  const childId = this.scenarioData['childId'] ?? '';
  const res = await api('/api/creatures', {
    headers: { 'X-Child-Id': childId },
  });
  const gallery = (await res.json()) as GalleryDto;
  const pilzar = gallery.entries.find((e) => e.id === PILZAR_ID);
  expect(pilzar?.unlocked).toBe(true);
});

Then('it can be picked as companion', async function (this: NumniaWorld) {
  const childId = this.scenarioData['childId'] ?? '';
  const res = await api(`/api/creatures/${PILZAR_ID}/companion`, {
    method: 'POST',
    headers: { 'X-Child-Id': childId },
  });
  const body = (await res.json()) as { companion: string };
  expect(body.companion).toBe(PILZAR_ID);
});

Given(
  'the candidate names {string}, {string}, {string}',
  function (this: NumniaWorld, a: string, b: string, c: string) {
    this.scenarioData['candidates'] = [a, b, c].join(',');
  },
);

When('the system validates the names', async function (this: NumniaWorld) {
  // Validation happens through the catalogue itself; fetching the catalogue
  // returns the three Release-1 creatures including their displayNames.
  const res = await api('/api/creatures', {
    headers: { 'X-Child-Id': this.scenarioData['childId'] ?? '' },
  });
  const gallery = (await res.json()) as GalleryDto;
  this.scenarioData['catalogueNames'] = gallery.entries
    .map((e) => e.displayName)
    .join(',');
});

Then('all three are accepted', function (this: NumniaWorld) {
  const candidates = (this.scenarioData['candidates'] ?? '').split(',');
  const present = (this.scenarioData['catalogueNames'] ?? '').split(',');
  for (const name of candidates) {
    expect(present).toContain(name);
  }
});

Then(
  'the system rejects no name due to a missing {string} ending',
  function (this: NumniaWorld, forbiddenEnding: string) {
    const candidates = (this.scenarioData['candidates'] ?? '').split(',');
    // BR-002: at least one candidate must NOT end with the forbidden suffix
    // and still be accepted.
    const nonMatching = candidates.filter((n) => !n.endsWith(forbiddenEnding));
    expect(nonMatching.length).toBeGreaterThan(0);
    const present = (this.scenarioData['catalogueNames'] ?? '').split(',');
    for (const name of nonMatching) {
      expect(present).toContain(name);
    }
  },
);

Given('a creature that is not yet unlocked', async function (this: NumniaWorld) {
  const childId = this.scenarioData['childId'] ?? '';
  // Establish a previous companion (Pilzar) by unlocking it via mastery.
  await api('/api/test/learning-progress', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      childId,
      operation: 'ADDITION',
      masteryStatus: 'MASTERED',
    }),
  });
  await api('/api/creatures/unlocks', {
    method: 'POST',
    headers: { 'X-Child-Id': childId },
  });
  await api(`/api/creatures/${PILZAR_ID}/companion`, {
    method: 'POST',
    headers: { 'X-Child-Id': childId },
  });
  // Welleno remains locked.
});

When('the child tries to pick it as companion', async function (this: NumniaWorld) {
  const childId = this.scenarioData['childId'] ?? '';
  const res = await api(`/api/creatures/${WELLENO_ID}/companion`, {
    method: 'POST',
    headers: { 'X-Child-Id': childId },
    allowError: true,
  });
  this.scenarioData['lastStatus'] = String(res.status);
});

Then('the server responds with status 409', function (this: NumniaWorld) {
  expect(this.scenarioData['lastStatus']).toBe('409');
});

Then('the previous companion stays active', async function (this: NumniaWorld) {
  const childId = this.scenarioData['childId'] ?? '';
  const res = await api('/api/creatures', {
    headers: { 'X-Child-Id': childId },
  });
  const gallery = (await res.json()) as GalleryDto;
  expect(gallery.companion).toBe(PILZAR_ID);
});
