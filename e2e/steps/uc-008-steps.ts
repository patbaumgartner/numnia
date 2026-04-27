/**
 * Step definitions for UC-008 — Child views own learning progress.
 *
 * Backend-driven; the shared "Given an active child session" step is
 * reused from UC-002/UC-003 step files. Scenarios are dry-run only until
 * the supporting `/api/test/star-points` and equivalent seed helpers are
 * implemented (same status as UC-005/UC-007).
 *
 * Refs: FR-GAME-005, FR-LEARN-009, NFR-A11Y-002/003, NFR-UX-001,
 *       NFR-I18N-002, BR-001/002/003.
 */
import { Given, When, Then } from '@cucumber/cucumber';
import { expect } from '@playwright/test';
import { NumniaWorld, API_URL } from '../support/world';

interface OperationProgressDto {
  operation: string;
  totalSessions: number;
  totalTasks: number;
  correctTasks: number;
  accuracy: number;
  masteryStatus: string;
  currentDifficulty: number;
}

interface ProgressOverviewDto {
  childId: string;
  palette: string;
  empty: boolean;
  entries: OperationProgressDto[];
}

async function fetchProgress(childId: string): Promise<ProgressOverviewDto> {
  const res = await fetch(`${API_URL}/api/progress`, {
    headers: { 'X-Child-Id': childId },
  });
  if (!res.ok) {
    throw new Error(`GET /api/progress failed: ${res.status} ${await res.text()}`);
  }
  return (await res.json()) as ProgressOverviewDto;
}

Given('at least three completed training sessions', async function (this: NumniaWorld) {
  // Bootstrapped via /api/test/training-history helper (deferred to UC-008
  // operational follow-up). Scenario is dry-run only until then.
  this.scenarioData['minimumSessions'] = '3';
});

When('the child opens {string}', async function (this: NumniaWorld, label: string) {
  expect(label).toMatch(/My progress/i);
  await this.page.goto('/progress');
});

When('it opens the progress view', async function (this: NumniaWorld) {
  await this.page.goto('/progress');
});

Then(
  'it sees a separate progress bar per operation',
  async function (this: NumniaWorld) {
    const childId = this.scenarioData['childId'] ?? '';
    const overview = await fetchProgress(childId);
    expect(overview.entries.length).toBeGreaterThanOrEqual(4);
    for (const op of [
      'ADDITION',
      'SUBTRACTION',
      'MULTIPLICATION',
      'DIVISION',
    ]) {
      await expect(this.page.getByTestId(`bar-${op}`)).toBeVisible();
    }
  },
);

Then(
  'the mastery status per content domain is marked',
  async function (this: NumniaWorld) {
    for (const op of [
      'ADDITION',
      'SUBTRACTION',
      'MULTIPLICATION',
      'DIVISION',
    ]) {
      await expect(this.page.getByTestId(`mastery-${op}`)).toBeVisible();
    }
  },
);

Given('the progress view is open', async function (this: NumniaWorld) {
  await this.page.goto('/progress');
});

Then(
  'the view contains no leaderboard with other children',
  async function (this: NumniaWorld) {
    await expect(this.page.getByText(/Rangliste/i)).toHaveCount(0);
    await expect(this.page.getByText(/Bestenliste/i)).toHaveCount(0);
    await expect(this.page.getByText(/Vergleich mit/i)).toHaveCount(0);
    const childId = this.scenarioData['childId'] ?? '';
    const overview = await fetchProgress(childId);
    const json = JSON.stringify(overview).toLowerCase();
    expect(json).not.toContain('leaderboard');
    expect(json).not.toContain('rank');
    expect(json).not.toContain('peer');
  },
);

Given(
  'the child has the deuteranopia profile enabled',
  async function (this: NumniaWorld) {
    const childId = this.scenarioData['childId'] ?? '';
    const res = await fetch(`${API_URL}/api/progress/preferences/palette`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        'X-Child-Id': childId,
      },
      body: JSON.stringify({ palette: 'DEUTERANOPIA' }),
    });
    expect(res.ok).toBe(true);
  },
);

Then(
  'the visualization uses the corresponding color palette',
  async function (this: NumniaWorld) {
    const root = this.page.getByTestId('progress-root');
    await expect(root).toHaveClass(/palette-deuteranopia/);
    const childId = this.scenarioData['childId'] ?? '';
    const overview = await fetchProgress(childId);
    expect(overview.palette).toBe('DEUTERANOPIA');
  },
);
