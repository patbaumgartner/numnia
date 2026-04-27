import { defineConfig, devices } from '@playwright/test';

/**
 * Playwright configuration for Numnia E2E tests.
 * Locale: de-CH (Swiss High German) — NFR-I18N-002.
 * baseURL: Vite dev server on port 5173.
 */
export default defineConfig({
  testDir: './tests',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : undefined,
  reporter: 'html',
  use: {
    baseURL: 'http://localhost:5173',
    locale: 'de-CH',
    trace: 'on-first-retry',
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
  webServer: {
    command: 'pnpm --filter numnia-frontend dev',
    url: 'http://localhost:5173',
    reuseExistingServer: !process.env.CI,
  },
});
