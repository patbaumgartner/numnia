/**
 * Custom Cucumber World for Numnia E2E tests.
 *
 * Provides a Playwright Browser + Page instance per scenario,
 * plus helper methods for interacting with the Numnia backend test API.
 */
import { setWorldConstructor, World } from '@cucumber/cucumber';
import type { IWorldOptions } from '@cucumber/cucumber';
import { chromium } from '@playwright/test';
import type { Browser, BrowserContext, Page } from '@playwright/test';

const BASE_URL = process.env.NUMNIA_FRONTEND_URL ?? 'http://localhost:5173';
const API_URL = process.env.NUMNIA_BACKEND_URL ?? 'http://localhost:8080';

export class NumniaWorld extends World {
  browser!: Browser;
  context!: BrowserContext;
  page!: Page;

  /** Data shared between steps within a scenario */
  scenarioData: Record<string, string> = {};

  constructor(options: IWorldOptions) {
    super(options);
  }

  async openBrowser() {
    this.browser = await chromium.launch({ headless: true });
    this.context = await this.browser.newContext({
      baseURL: BASE_URL,
      locale: 'de-CH',
    });
    this.page = await this.context.newPage();
  }

  async closeBrowser() {
    await this.context?.close();
    await this.browser?.close();
  }

  /** Navigate to a frontend path */
  async goto(path: string) {
    await this.page.goto(`${BASE_URL}${path}`);
  }

  /**
   * Retrieve a verification/confirmation token from the backend test-only
   * endpoint (active only when numnia.e2e.enabled=true).
   */
  async getToken(email: string, purpose: 'PARENT_EMAIL_VERIFICATION' | 'CHILD_PROFILE_CONFIRMATION'): Promise<string> {
    const url = `${API_URL}/api/test/verification-tokens?email=${encodeURIComponent(email)}&purpose=${purpose}`;
    const response = await fetch(url);
    if (!response.ok) {
      throw new Error(`Failed to get token: ${response.status} ${await response.text()}`);
    }
    const body = await response.json() as { token: string };
    return body.token;
  }
}

setWorldConstructor(NumniaWorld);
export { BASE_URL, API_URL };
