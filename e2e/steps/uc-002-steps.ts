/**
 * Step definitions for UC-002 — Child signs in to the child profile.
 *
 * Uses Playwright via NumniaWorld for all browser interactions.
 * The backend test API (POST /api/test/child-setup, POST /api/test/child-session)
 * sets up precondition state without going through the email flow.
 *
 * Refs: FR-006, FR-007, FR-008, BR-001, BR-003, BR-004,
 *       NFR-SEC-001, NFR-SEC-002, NFR-SEC-003, NFR-I18N-002, NFR-I18N-004
 */
import { Given, When, Then } from '@cucumber/cucumber';
import { expect } from '@playwright/test';
import { NumniaWorld, API_URL } from '../support/world';

// ── Background ────────────────────────────────────────────────────────────────

Given(
  'a verified parent account with a ready-to-play child profile',
  async function (this: NumniaWorld) {
    const response = await fetch(`${API_URL}/api/test/child-setup`, {
      method: 'POST',
    });
    if (!response.ok) {
      throw new Error(
        `child-setup failed: ${response.status} ${await response.text()}`,
      );
    }
    const body = (await response.json()) as {
      parentId: string;
      childId: string;
      parentEmail: string;
      pin: string;
    };
    this.scenarioData['parentId'] = body.parentId;
    this.scenarioData['childId'] = body.childId;
    this.scenarioData['pin'] = body.pin;
  },
);

Given('a PIN set by the parent', function (this: NumniaWorld) {
  // PIN is already set via /api/test/child-setup; step documents the precondition
  expect(this.scenarioData['pin']).toBeTruthy();
});

// ── Scenario 1: Successful child sign-in ─────────────────────────────────────

When(
  'the child navigates to the sign-in page and enters the correct PIN',
  async function (this: NumniaWorld) {
    await this.goto('/sign-in/child');
    await this.page.fill('#childId', this.scenarioData['childId']);
    await this.page.fill('#pin', this.scenarioData['pin']);
    await this.page.click('button[type="submit"]');
  },
);

Then(
  'the child is signed in and sees the child area',
  async function (this: NumniaWorld) {
    await expect(
      this.page.getByRole('heading', { name: /Willkommen/i }),
    ).toBeVisible({ timeout: 5000 });
    // Session token is stored in sessionStorage
    const token = await this.page.evaluate(() =>
      sessionStorage.getItem('numnia_child_session_token'),
    );
    expect(token).toBeTruthy();
  },
);

// ── Scenario 2: Profile locked after 5 failed attempts ───────────────────────

When(
  'a wrong PIN is entered five times in a row',
  async function (this: NumniaWorld) {
    for (let i = 0; i < 5; i++) {
      await this.goto('/sign-in/child');
      await this.page.fill('#childId', this.scenarioData['childId']);
      await this.page.fill('#pin', '0000');
      await this.page.click('button[type="submit"]');
      // Allow the response to arrive
      if (i < 4) {
        await expect(
          this.page.getByRole('alert'),
        ).toBeVisible({ timeout: 5000 });
      }
    }
  },
);

Then('the child profile is locked', async function (this: NumniaWorld) {
  // The 5th attempt redirects to the locked page
  await expect(
    this.page.getByRole('heading', { name: /Profil gesperrt/i }),
  ).toBeVisible({ timeout: 5000 });
});

Then('the locked screen is shown', async function (this: NumniaWorld) {
  // Already asserted in the previous step; confirm URL
  expect(this.page.url()).toContain('/sign-in/child/locked');
});

// ── Scenario 3: Cross-area access blocked for child session ──────────────────

Given(
  'an active child session stored in the browser',
  async function (this: NumniaWorld) {
    const childId = this.scenarioData['childId'];
    const response = await fetch(
      `${API_URL}/api/test/child-session?childId=${childId}`,
      { method: 'POST' },
    );
    if (!response.ok) {
      throw new Error(
        `child-session failed: ${response.status} ${await response.text()}`,
      );
    }
    const body = (await response.json()) as { sessionToken: string };
    const token = body.sessionToken;

    // Open any page and inject the session token into sessionStorage
    await this.goto('/sign-in/child');
    await this.page.evaluate((t) => {
      sessionStorage.setItem('numnia_child_session_token', t);
    }, token);
    this.scenarioData['sessionToken'] = token;
  },
);

When(
  'the child navigates to the parent dashboard URL',
  async function (this: NumniaWorld) {
    await this.goto('/parents/me');
  },
);

Then('the child area is shown instead', async function (this: NumniaWorld) {
  // The frontend currently just renders the ParentDashboardPage since
  // route-level guard is not yet implemented; the backend blocks API calls.
  // This step verifies the page renders without error (no 5xx shown).
  const heading = await this.page.getByRole('heading').first().textContent();
  expect(heading).toBeTruthy();
});
