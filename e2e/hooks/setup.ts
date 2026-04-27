/**
 * Cucumber hooks for Numnia E2E tests.
 *
 * Before each scenario: open a fresh Playwright browser.
 * After each scenario: close the browser.
 *
 * Pre-condition: the backend (port 8080 with numnia.e2e.enabled=true) and
 * the frontend (Vite dev server, port 5173 proxying /api to port 8080)
 * must already be running. Start them with:
 *
 *   Backend:  cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=e2e
 *   Frontend: cd frontend && pnpm dev
 */
import { Before, After, BeforeAll, AfterAll } from '@cucumber/cucumber';
import { NumniaWorld, API_URL } from '../support/world';

/** Verify connectivity to backend and frontend before the suite runs. */
BeforeAll(async function () {
  const backendHealth = `${API_URL}/actuator/health`;
  let backendOk = false;
  for (let i = 0; i < 30; i++) {
    try {
      const response = await fetch(backendHealth);
      if (response.ok) {
        backendOk = true;
        break;
      }
    } catch {
      // not yet ready
    }
    await new Promise(r => setTimeout(r, 1000));
  }
  if (!backendOk) {
    console.warn(
      `[E2E] Backend health check failed at ${backendHealth} — ` +
      'start the backend with profile "e2e" before running E2E tests.',
    );
  }
});

Before(async function (this: NumniaWorld) {
  await this.openBrowser();
});

After(async function (this: NumniaWorld) {
  await this.closeBrowser();
});

AfterAll(async function () {
  // No-op: servers are managed externally.
});
