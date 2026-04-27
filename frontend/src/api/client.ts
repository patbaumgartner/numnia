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
  WorldResponse,
  PortalEntryResponse,
  PortalType,
  GalleryResponse,
  CreatureUnlockResultResponse,
  PickCompanionResponse,
  AvatarResponse,
  ShopItemsResponse,
  InventoryResponse,
  PurchaseResultResponse,
  ProgressOverviewResponse,
  ColorPalette,
  ChildControlsResponse,
  ChildControlsRequest,
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

// ── UC-005: World map and portal entry ───────────────────────────────────────

/** List the worlds available in the current release (UC-005 BR-001). */
export async function listWorlds(): Promise<WorldResponse[]> {
  return request<WorldResponse[]>('GET', '/worlds');
}

/** Attempt to enter a portal of a world; backend evaluates the unlock rules. */
export async function enterPortal(
  childId: string,
  worldId: string,
  portalType: PortalType,
): Promise<PortalEntryResponse> {
  return request<PortalEntryResponse>(
    'POST',
    `/worlds/${encodeURIComponent(worldId)}/portals/${portalType}/enter`,
    undefined,
    { 'X-Child-Id': childId },
  );
}

// ── UC-006: Creatures gallery and companion ──────────────────────────────────

/** Fetch the gallery (locked + unlocked) for a child (UC-006 step 4). */
export async function getCreatureGallery(
  childId: string,
): Promise<GalleryResponse> {
  return request<GalleryResponse>('GET', '/creatures', undefined, {
    'X-Child-Id': childId,
  });
}

/** Process unlocks from learning progress (UC-006 main flow steps 1-3). */
export async function processCreatureUnlocks(
  childId: string,
): Promise<CreatureUnlockResultResponse> {
  return request<CreatureUnlockResultResponse>(
    'POST',
    '/creatures/unlocks',
    undefined,
    { 'X-Child-Id': childId },
  );
}

/** Pick a creature as the active companion (UC-006 step 5, BR-003). */
export async function pickCompanion(
  childId: string,
  creatureId: string,
): Promise<PickCompanionResponse> {
  return request<PickCompanionResponse>(
    'POST',
    `/creatures/${encodeURIComponent(creatureId)}/companion`,
    undefined,
    { 'X-Child-Id': childId },
  );
}

// ── UC-007: Avatar customization and shop ────────────────────────────────────

/** Fetch the current avatar configuration plus star-points balance. */
export async function getAvatar(childId: string): Promise<AvatarResponse> {
  return request<AvatarResponse>('GET', '/avatar', undefined, {
    'X-Child-Id': childId,
  });
}

/** Change the avatar base model (vetted catalog only, BR-003 of UC-001). */
export async function setAvatarBaseModel(
  childId: string,
  baseModel: string,
): Promise<{ baseModel: string; equipped: Record<string, string> }> {
  return request('PUT', '/avatar/base-model', { baseModel }, {
    'X-Child-Id': childId,
  });
}

/** Equip an item already present in the inventory. */
export async function equipAvatarItem(
  childId: string,
  itemId: string,
): Promise<{ baseModel: string; equipped: Record<string, string> }> {
  return request('POST', '/avatar/equipped', { itemId }, {
    'X-Child-Id': childId,
  });
}

/** List shop items with prices in star points (UC-007 step 3). */
export async function listShopItems(): Promise<ShopItemsResponse> {
  return request<ShopItemsResponse>('GET', '/shop/items');
}

/** Purchase a shop item; deducts price and adds to inventory permanently. */
export async function purchaseShopItem(
  childId: string,
  itemId: string,
): Promise<PurchaseResultResponse> {
  return request<PurchaseResultResponse>(
    'POST',
    `/shop/items/${encodeURIComponent(itemId)}/purchase`,
    undefined,
    { 'X-Child-Id': childId },
  );
}

/** List the items the child already owns (UC-007 BR-003 permanent inventory). */
export async function getInventory(childId: string): Promise<InventoryResponse> {
  return request<InventoryResponse>('GET', '/avatar/inventory', undefined, {
    'X-Child-Id': childId,
  });
}

/** UC-008: get the child's own learning progress overview. */
export async function getProgress(childId: string): Promise<ProgressOverviewResponse> {
  return request<ProgressOverviewResponse>('GET', '/progress', undefined, {
    'X-Child-Id': childId,
  });
}

/** UC-008 alt-flow 3a: switch the color palette (e.g. for color-blind profile). */
export async function setProgressPalette(
  childId: string,
  palette: ColorPalette,
): Promise<{ palette: ColorPalette }> {
  return request<{ palette: ColorPalette }>(
    'PUT',
    '/progress/preferences/palette',
    { palette },
    { 'X-Child-Id': childId },
  );
}

/** UC-009: get the per-child play-time + risk controls. */
export async function getChildControls(
  parentId: string,
  childId: string,
): Promise<ChildControlsResponse> {
  return request<ChildControlsResponse>(
    'GET',
    `/parents/me/children/${encodeURIComponent(childId)}/controls`,
    undefined,
    { 'X-Parent-Id': parentId },
  );
}

/** UC-009: update the per-child controls (daily limit + risk). */
export async function updateChildControls(
  parentId: string,
  childId: string,
  body: ChildControlsRequest,
): Promise<ChildControlsResponse> {
  return request<ChildControlsResponse>(
    'PUT',
    `/parents/me/children/${encodeURIComponent(childId)}/controls`,
    body,
    { 'X-Parent-Id': parentId },
  );
}
