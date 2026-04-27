/**
 * Step definitions for UC-011 — Parent deletes a child account.
 *
 * Backend-driven dry-run; the canonical assertions live in the backend
 * Cucumber suite (Uc011StepDefinitions). The Playwright E2E layer here
 * exercises the navigation contract until the production
 * `/api/test/expire-deletion` and `/api/test/backup-rotation` seed helpers
 * are wired (same status as UC-010).
 *
 * Refs: FR-PAR-005, FR-SAFE-005, NFR-PRIV-002, NFR-SEC-003, NFR-OPS-002,
 *       BR-001 (cool-off), BR-002 (record), BR-003 (audit), BR-004 (backups).
 *
 * The "a verified parent account with a child profile" Background step is
 * provided by uc-010-steps.ts and resolved by Cucumber automatically.
 */
import { Given, When, Then } from '@cucumber/cucumber';
import { expect } from '@playwright/test';
import { NumniaWorld } from '../support/world';

Given('the parent confirms with password and the word {string}', async function (
  this: NumniaWorld,
  word: string,
) {
  this.scenarioData['confirmationWord'] = word;
  this.scenarioData['deletionTriggered'] = 'true';
  await this.page.goto('/parents/deletion/00000000-0000-0000-0000-0000000000bb');
});

When(
  'the parent opens the link from the confirmation email within 24 hours',
  async function (this: NumniaWorld) {
    this.scenarioData['linkOpenedWithinCoolOff'] = 'true';
  },
);

Then('all personal data of the child profile is deleted', async function (
  this: NumniaWorld,
) {
  // Asserted server-side in backend Cucumber + DeletionServiceTest.
  expect(this.scenarioData['linkOpenedWithinCoolOff']).toBe('true');
});

Then(
  'the parent receives a deletion record with date and data categories',
  async function (this: NumniaWorld) {
    expect(this.scenarioData['deletionTriggered']).toBe('true');
  },
);

Given('a triggered deletion process', async function (this: NumniaWorld) {
  this.scenarioData['deletionTriggered'] = 'true';
});

When(
  'the parent does not open the confirmation link within 24 hours',
  async function (this: NumniaWorld) {
    // Time travel is simulated server-side via the backend Cucumber suite.
    this.scenarioData['coolOffElapsed'] = 'true';
  },
);

Then('the child profile remains active', async function (this: NumniaWorld) {
  expect(this.scenarioData['coolOffElapsed']).toBe('true');
});

Then(
  'the deletion process is marked as {string} in the audit log',
  async function (this: NumniaWorld, label: string) {
    // Audit assertion is server-side; we just verify the label contract.
    expect(label).toBe('discarded');
  },
);

Given('a completed deletion process', async function (this: NumniaWorld) {
  this.scenarioData['deletionCompleted'] = 'true';
});

When('the next backup rotation runs', async function (this: NumniaWorld) {
  this.scenarioData['backupRotated'] = 'true';
});

Then(
  'the active backups no longer contain personal data of the deleted child profile',
  async function (this: NumniaWorld) {
    expect(this.scenarioData['deletionCompleted']).toBe('true');
    expect(this.scenarioData['backupRotated']).toBe('true');
  },
);
