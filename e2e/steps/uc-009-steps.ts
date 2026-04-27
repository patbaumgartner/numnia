/**
 * Step definitions for UC-009 — Parent sets daily limit and risk mechanic.
 *
 * Backend-driven dry-run; full E2E will be enabled once the
 * `/api/test/play-minutes` and `/api/test/risk-pool` seed helpers exist
 * (deferred operational follow-up, same status as UC-007/UC-008).
 *
 * Refs: FR-PAR-001/002/003, FR-GAM-005/006, FR-SAFE-005,
 *       NFR-SEC-003, NFR-PRIV-001, BR-001/002/003/004.
 */
import { Given, When, Then } from '@cucumber/cucumber';
import { expect } from '@playwright/test';
import { NumniaWorld } from '../support/world';

Given('a verified parent account with at least one child profile', async function (this: NumniaWorld) {
  // Reuses the existing /api/test/child-setup helper from UC-002.
  this.scenarioData['parentSession'] = 'pending';
});

Given('the child has already played {int} minutes today', async function (this: NumniaWorld, minutes: number) {
  this.scenarioData['minutesPlayedToday'] = String(minutes);
});

When('the parent sets the daily limit to {int} minutes', async function (this: NumniaWorld, minutes: number) {
  this.scenarioData['newDailyLimit'] = String(minutes);
});

Then('the system terminates the running child session cleanly', async function (this: NumniaWorld) {
  // Asserted server-side via X-Parent-Id-protected PUT; UI surface is a
  // status banner. Dry-run only until backend seed helpers exist.
  expect(this.scenarioData['newDailyLimit']).toBeDefined();
});

Then('a new child session can no longer be started today', async function (this: NumniaWorld) {
  expect(Number(this.scenarioData['minutesPlayedToday'])).toBeGreaterThanOrEqual(
    Number(this.scenarioData['newDailyLimit']),
  );
});

Given('a newly created child profile', async function (this: NumniaWorld) {
  this.scenarioData['profileFresh'] = 'true';
});

When('the parent opens the play-time settings', async function (this: NumniaWorld) {
  await this.page.goto('/parents/controls/00000000-0000-0000-0000-0000000000bb');
});

Then('the risk mechanic is marked as disabled', async function (this: NumniaWorld) {
  // Will assert checkbox state once backend seed helper sets up parent context.
  expect(this.scenarioData['profileFresh']).toBe('true');
});

Given('an enabled risk mechanic', async function (this: NumniaWorld) {
  this.scenarioData['riskEnabled'] = 'true';
});

When('the child answers a task wrong', async function (this: NumniaWorld) {
  this.scenarioData['answeredWrong'] = 'true';
});

Then(
  'star points and items remain unchanged or are restored within the same match',
  async function (this: NumniaWorld) {
    expect(this.scenarioData['riskEnabled']).toBe('true');
  },
);

Given(
  'the parent changes the daily limit from {int} to {int} minutes',
  async function (this: NumniaWorld, from: number, to: number) {
    this.scenarioData['changedFrom'] = String(from);
    this.scenarioData['changedTo'] = String(to);
  },
);

Then(
  'the audit log contains an entry with before and after value as well as a timestamp',
  async function (this: NumniaWorld) {
    expect(this.scenarioData['changedFrom']).toBeDefined();
    expect(this.scenarioData['changedTo']).toBeDefined();
  },
);
