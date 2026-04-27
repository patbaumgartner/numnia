/**
 * Step definitions for UC-004 — Child practices in accuracy mode.
 *
 * Backend-driven, mirroring the UC-003 step style. The "Background" step
 * "an active child session" is reused from the UC-003 steps file.
 *
 * Refs: FR-GAME-001/002, FR-LEARN-004/006/008, FR-GAM-005,
 *       NFR-A11Y-001/003, NFR-I18N-002, BR-001..BR-003 of UC-004.
 */
import { Given, When, Then } from '@cucumber/cucumber';
import { expect } from '@playwright/test';
import { NumniaWorld, API_URL } from '../support/world';

interface SessionDto {
  sessionId: string;
  operation: string;
  difficulty: number;
  speed: number;
}

interface TaskDto {
  taskId: string;
  operation: string;
  operandA: number;
  operandB: number;
  difficulty: number;
  speed: number;
  timed: boolean;
}

interface AnswerResultDto {
  outcome: 'CORRECT' | 'WRONG' | 'TIMEOUT';
  currentSpeed: number;
  modeSuggestion: 'NONE' | 'ACCURACY' | 'EXPLANATION';
  starPointsBalance: number;
}

interface ExplanationDto {
  taskId: string;
  operation: string;
  steps: string[];
}

async function api(path: string, init?: RequestInit): Promise<Response> {
  const response = await fetch(`${API_URL}${path}`, init);
  if (!response.ok) {
    throw new Error(
      `${init?.method ?? 'GET'} ${path} failed: ${response.status} ${await response.text()}`,
    );
  }
  return response;
}

async function startAccuracy(
  childId: string,
  operation: string,
): Promise<SessionDto> {
  const res = await api('/api/training/accuracy-sessions', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'X-Child-Id': childId },
    body: JSON.stringify({ operation }),
  });
  return (await res.json()) as SessionDto;
}

async function nextTask(sessionId: string): Promise<TaskDto> {
  const res = await api(`/api/training/sessions/${sessionId}/tasks`, {
    method: 'POST',
  });
  return (await res.json()) as TaskDto;
}

async function submit(
  sessionId: string,
  answer: number,
): Promise<AnswerResultDto> {
  const res = await api(`/api/training/sessions/${sessionId}/answers`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ answer, responseTimeMs: 0 }),
  });
  return (await res.json()) as AnswerResultDto;
}

// ── Scenario 1: Accuracy mode runs without a timer ───────────────────────────

Given(
  'the child starts accuracy mode for subtraction',
  async function (this: NumniaWorld) {
    const session = await startAccuracy(
      this.scenarioData['childId'],
      'SUBTRACTION',
    );
    expect(session.speed).toBe(0);
    this.scenarioData['sessionId'] = session.sessionId;
  },
);

When('a task is shown', async function (this: NumniaWorld) {
  const task = await nextTask(this.scenarioData['sessionId']);
  this.scenarioData['taskId'] = task.taskId;
  this.scenarioData['timed'] = String(task.timed);
  this.scenarioData['speed'] = String(task.speed);
  this.scenarioData['operandA'] = String(task.operandA);
  this.scenarioData['operandB'] = String(task.operandB);
});

Then('no time limit is active', function (this: NumniaWorld) {
  expect(this.scenarioData['timed']).toBe('false');
  expect(Number(this.scenarioData['speed'])).toBe(0);
});

Then('no timer is visible in the UI', function (this: NumniaWorld) {
  // The backend asserts the absence of timing data; the React AccuracyPage
  // suppresses the timer when the task carries `timed === false`. The
  // corresponding UI assertion lives in AccuracyPage.test.tsx
  // ("starts an accuracy session and shows the task without any timer").
  expect(this.scenarioData['timed']).toBe('false');
});

// ── Scenario 2: Explanation mode is reachable from accuracy mode ────────────

Given(
  'a task is shown in accuracy mode',
  async function (this: NumniaWorld) {
    const session = await startAccuracy(
      this.scenarioData['childId'],
      'ADDITION',
    );
    this.scenarioData['sessionId'] = session.sessionId;
    const task = await nextTask(this.scenarioData['sessionId']);
    this.scenarioData['taskId'] = task.taskId;
    this.scenarioData['operandA'] = String(task.operandA);
    this.scenarioData['operandB'] = String(task.operandB);
  },
);

When(
  'the child selects {string}',
  async function (this: NumniaWorld, action: string) {
    if (action !== 'Show explanation') {
      throw new Error(`Unknown action in UC-004: ${action}`);
    }
    const res = await api(
      `/api/training/sessions/${this.scenarioData['sessionId']}/explanation`,
    );
    const exp = (await res.json()) as ExplanationDto;
    this.scenarioData['explanationStepCount'] = String(exp.steps.length);
    this.scenarioData['explanationContainsSharpS'] = String(
      exp.steps.some((s) => s.includes('ß')),
    );
  },
);

Then('animated solution steps are played', function (this: NumniaWorld) {
  expect(Number(this.scenarioData['explanationStepCount'])).toBeGreaterThanOrEqual(
    2,
  );
  expect(this.scenarioData['explanationContainsSharpS']).toBe('false');
});

Then('the task remains workable', async function (this: NumniaWorld) {
  // Re-fetching the explanation does not advance the session — the same
  // task is still answerable. Submit the correct answer to verify.
  const a = Number(this.scenarioData['operandA']);
  const b = Number(this.scenarioData['operandB']);
  const result = await submit(this.scenarioData['sessionId'], a + b);
  expect(result.outcome).toBe('CORRECT');
});

// ── Scenario 3: No star point loss on error ─────────────────────────────────

Given(
  'a child with {int} star points',
  async function (this: NumniaWorld, balance: number) {
    await api(
      `/api/test/star-points/${this.scenarioData['childId']}?balance=${balance}`,
      { method: 'POST' },
    );
    this.scenarioData['expectedStarBalance'] = String(balance);
  },
);

When(
  'it answers a task wrong in accuracy mode',
  async function (this: NumniaWorld) {
    const session = await startAccuracy(
      this.scenarioData['childId'],
      'ADDITION',
    );
    this.scenarioData['sessionId'] = session.sessionId;
    const task = await nextTask(this.scenarioData['sessionId']);
    const wrong = task.operandA + task.operandB + 1;
    const result = await submit(this.scenarioData['sessionId'], wrong);
    this.scenarioData['outcome'] = result.outcome;
    this.scenarioData['starsAfter'] = String(result.starPointsBalance);
  },
);

Then(
  'the star points balance stays at {int}',
  function (this: NumniaWorld, expected: number) {
    // Shared between UC-003 and UC-004 scenarios. Cucumber's step
    // registry is global across `steps/**` files; we keep this single
    // parameterised definition here (UC-004 was the first author of the
    // {int} form). UC-003's "stays at 12" reuses it.
    expect(Number(this.scenarioData['starsAfter'])).toBe(expected);
  },
);
