/**
 * Typed API client for the Numnia backend.
 *
 * Uses the browser's built-in `fetch` API (no axios per AGENTS.md §4).
 * All endpoints match the backend REST contract (ch.numnia.iam.api.*).
 *
 * Base URL defaults to the same origin in production; in development Vite
 * proxies `/api` to `http://localhost:8080` via vite.config.ts.
 */
import type {
  RegisterParentRequest,
  RegisterParentResponse,
  VerifyEmailRequest,
  VerifyEmailResponse,
  CreateChildProfileRequest,
  CreateChildProfileResponse,
  ConfirmChildProfileRequest,
  SignInChildRequest,
  SignInChildResponse,
  ErrorResponse,
  StartTrainingSessionRequest,
  StartTrainingSessionResponse,
  TrainingTaskResponse,
  AnswerResultResponse,
  SessionSummaryResponse,
  ExplanationStepsResponse,
} from './types';

const API_BASE = '/api';

class ApiError extends Error {
  constructor(
    public readonly status: number,
    public readonly code: string,
    message: string,
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

async function request<T>(
  method: string,
  path: string,
  body?: unknown,
  extraHeaders?: Record<string, string>,
): Promise<T> {
  const response = await fetch(`${API_BASE}${path}`, {
    method,
    headers: { 'Content-Type': 'application/json', ...extraHeaders },
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });

  if (!response.ok) {
    let err: ErrorResponse;
    try {
      err = (await response.json()) as ErrorResponse;
    } catch {
      err = { error: 'UNKNOWN', message: response.statusText };
    }
    throw new ApiError(response.status, err.error, err.message);
  }

  // 204 No Content
  if (response.status === 204) {
    return undefined as unknown as T;
  }

  return response.json() as Promise<T>;
}

/** Register a new parent account. */
export async function registerParent(
  req: RegisterParentRequest,
): Promise<RegisterParentResponse> {
  return request<RegisterParentResponse>('POST', '/parents', req);
}

/** Confirm the parent's primary email address. */
export async function verifyEmail(
  req: VerifyEmailRequest,
): Promise<VerifyEmailResponse> {
  return request<VerifyEmailResponse>('POST', '/parents/verify', req);
}

/** Create a child profile for a verified parent. */
export async function createChildProfile(
  parentId: string,
  req: CreateChildProfileRequest,
): Promise<CreateChildProfileResponse> {
  return request<CreateChildProfileResponse>(
    'POST',
    `/parents/${parentId}/child-profiles`,
    req,
  );
}

/** Confirm the secondary consent for a child profile. */
export async function confirmChildProfile(
  parentId: string,
  childId: string,
  req: ConfirmChildProfileRequest,
): Promise<void> {
  return request<void>(
    'POST',
    `/parents/${parentId}/child-profiles/${childId}/confirm`,
    req,
  );
}

export { ApiError };

// ── UC-002: Child sign-in ────────────────────────────────────────────────────

/** Sign in a child with their ID and PIN. Returns a server-side child session. */
export async function signInChild(
  req: SignInChildRequest,
): Promise<SignInChildResponse> {
  return request<SignInChildResponse>('POST', '/child-sessions', req);
}

/** Sign out the currently active child session. */
export async function signOutChild(sessionToken: string): Promise<void> {
  return request<void>('DELETE', '/child-sessions/current', undefined, {
    'X-Numnia-Session': sessionToken,
  });
}

/** Set the PIN for a child profile (parent action). */
export async function setChildPin(
  parentId: string,
  childId: string,
  pin: string,
): Promise<void> {
  return request<void>(
    'POST',
    `/parents/${parentId}/child-profiles/${childId}/pin`,
    { pin },
  );
}

/** Release the lockout on a child profile (parent action). */
export async function releaseChildLock(
  parentId: string,
  childId: string,
): Promise<void> {
  return request<void>(
    'POST',
    `/parents/${parentId}/child-profiles/${childId}/release-lock`,
  );
}

// ── UC-003: Training mode ────────────────────────────────────────────────────

/** Start a new training session for the given child + operation. */
export async function startTrainingSession(
  childId: string,
  req: StartTrainingSessionRequest,
): Promise<StartTrainingSessionResponse> {
  return request<StartTrainingSessionResponse>('POST', '/training/sessions', req, {
    'X-Child-Id': childId,
  });
}

/** Generate the next task for an active session. */
export async function nextTrainingTask(
  sessionId: string,
): Promise<TrainingTaskResponse> {
  return request<TrainingTaskResponse>(
    'POST',
    `/training/sessions/${sessionId}/tasks`,
  );
}

/** Submit a child's answer to the current task. */
export async function submitTrainingAnswer(
  sessionId: string,
  answer: number,
  responseTimeMs: number,
): Promise<AnswerResultResponse> {
  return request<AnswerResultResponse>(
    'POST',
    `/training/sessions/${sessionId}/answers`,
    { answer, responseTimeMs },
  );
}

/** Notify the backend that the answer timer expired. */
export async function submitTrainingTimeout(
  sessionId: string,
): Promise<AnswerResultResponse> {
  return request<AnswerResultResponse>(
    'POST',
    `/training/sessions/${sessionId}/timeouts`,
  );
}

/** End the session and obtain a summary (UC-003 step 12). */
export async function endTrainingSession(
  sessionId: string,
): Promise<SessionSummaryResponse> {
  return request<SessionSummaryResponse>(
    'POST',
    `/training/sessions/${sessionId}/end`,
  );
}

// ── UC-004: Accuracy mode (G0, no time pressure) ─────────────────────────────

/** Start a new accuracy-mode session (G0, no time pressure). */
export async function startAccuracySession(
  childId: string,
  req: StartTrainingSessionRequest,
): Promise<StartTrainingSessionResponse> {
  return request<StartTrainingSessionResponse>(
    'POST',
    '/training/accuracy-sessions',
    req,
    { 'X-Child-Id': childId },
  );
}

/** Fetch the animated solution steps for the current task of a session. */
export async function getTrainingExplanation(
  sessionId: string,
): Promise<ExplanationStepsResponse> {
  return request<ExplanationStepsResponse>(
    'GET',
    `/training/sessions/${sessionId}/explanation`,
  );
}
