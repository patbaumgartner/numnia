/**
 * API types for UC-001 parent registration and child profile creation.
 * Mirrors the backend DTO layer (ch.numnia.iam.api.dto.*).
 *
 * Swiss High German UI copy lives in page/component files, not here.
 */

export interface RegisterParentRequest {
  firstName: string;
  salutation: string;
  email: string;
  password: string;
  privacyConsented: boolean;
  termsAccepted: boolean;
}

export interface RegisterParentResponse {
  parentId: string;
}

export interface VerifyEmailRequest {
  token: string;
}

export interface VerifyEmailResponse {
  parentId: string;
  status: string;
}

export interface CreateChildProfileRequest {
  pseudonym: string;
  yearOfBirth: number;
  avatarBaseModel: string;
}

export interface CreateChildProfileResponse {
  childProfileId: string;
  pseudonym: string;
}

export interface ConfirmChildProfileRequest {
  token: string;
}

export interface ErrorResponse {
  error: string;
  message: string;
}

// ── UC-002: Child sign-in ────────────────────────────────────────────────────

export interface SignInChildRequest {
  childId: string;
  pin: string;
}

export interface SignInChildResponse {
  sessionToken: string;
  childId: string;
  role: string;
  expiresAt: string;
}

// ── UC-003: Training mode ────────────────────────────────────────────────────

export type Operation = 'ADDITION' | 'SUBTRACTION' | 'MULTIPLICATION' | 'DIVISION';
export type AnswerOutcome = 'CORRECT' | 'WRONG' | 'TIMEOUT';
export type ModeSuggestion = 'NONE' | 'ACCURACY' | 'EXPLANATION';

export interface StartTrainingSessionRequest {
  operation: Operation;
  worldId?: string;
}

export interface StartTrainingSessionResponse {
  sessionId: string;
  operation: Operation;
  difficulty: number;
  speed: number;
}

export interface TrainingTaskResponse {
  taskId: string;
  operation: Operation;
  operandA: number;
  operandB: number;
  difficulty: number;
  speed: number;
  timed?: boolean;
}

export interface AnswerResultResponse {
  outcome: AnswerOutcome;
  currentSpeed: number;
  modeSuggestion: ModeSuggestion;
  starPointsBalance: number;
}

// ── UC-004: Accuracy mode (G0, no time pressure) ─────────────────────────────

export interface ExplanationStepsResponse {
  taskId: string;
  operation: Operation;
  steps: string[];
}

export interface SessionSummaryResponse {
  sessionId: string;
  totalTasks: number;
  correctTasks: number;
  starPointsBalance: number;
  masteryStatus: 'NOT_STARTED' | 'IN_CONSOLIDATION' | 'MASTERED';
}

// ── UC-005: World map and portal entry ───────────────────────────────────────

export type PortalType =
  | 'TRAINING'
  | 'DUEL'
  | 'TEAM'
  | 'EVENT'
  | 'BOSS'
  | 'CLASS'
  | 'SEASON';

export interface WorldResponse {
  id: string;
  displayName: string;
  difficultyLevel: number;
  requiredLevel: number;
}

export interface PortalEntryResponse {
  worldId: string;
  portalType: PortalType;
  locked: boolean;
  target: string | null;
  messageCode: string | null;
  reducedMotion: boolean;
}

// ── UC-006: Creatures gallery and companion ──────────────────────────────────

export interface CreatureResponse {
  id: string;
  displayName: string;
  operation: Operation;
  sourceWorldId: string;
}

export interface GalleryEntryResponse extends CreatureResponse {
  unlocked: boolean;
  isCompanion: boolean;
}

export interface GalleryResponse {
  entries: GalleryEntryResponse[];
  companion: string | null;
}

export interface CreatureUnlockResultResponse {
  newlyUnlocked: CreatureResponse[];
  consolationAwarded: boolean;
  starPointsAwarded: number;
}

export interface PickCompanionResponse {
  companion: string;
}

// ── UC-007: Avatar customization and shop ────────────────────────────────────

export interface AvatarResponse {
  childId: string;
  baseModel: string;
  equipped: Record<string, string>;
  starPointsBalance: number;
}

export interface ShopItemResponse {
  id: string;
  displayName: string;
  priceStarPoints: number;
  slot: string;
}

export interface InventoryItemResponse {
  itemId: string;
  purchasedAt: string;
}

export interface InventoryResponse {
  items: InventoryItemResponse[];
}

export interface ShopItemsResponse {
  items: ShopItemResponse[];
}

export interface PurchaseResultResponse {
  itemId: string;
  starPointsBalance: number;
}

// ── UC-008: progress view ────────────────────────────────────────────────

export type ColorPalette = 'DEFAULT' | 'DEUTERANOPIA' | 'PROTANOPIA' | 'TRITANOPIA';

export type MasteryStatus = 'NOT_STARTED' | 'IN_CONSOLIDATION' | 'MASTERED';

export type ProgressOperation =
  | 'ADDITION'
  | 'SUBTRACTION'
  | 'MULTIPLICATION'
  | 'DIVISION';

export interface OperationProgressResponse {
  operation: ProgressOperation;
  totalSessions: number;
  totalTasks: number;
  correctTasks: number;
  accuracy: number;
  masteryStatus: MasteryStatus;
  currentDifficulty: number;
}

export interface ProgressOverviewResponse {
  childId: string;
  palette: ColorPalette;
  empty: boolean;
  entries: OperationProgressResponse[];
}

// ── UC-009: parent controls (daily limit + risk mechanic) ────────────────

export interface ChildControlsResponse {
  childId: string;
  parentId: string;
  dailyLimitMinutes: number | null;
  breakRecommendationMinutes: number;
  riskMechanicEnabled: boolean;
}

export interface ChildControlsRequest {
  dailyLimitMinutes: number | null;
  breakRecommendationMinutes: number;
  riskMechanicEnabled: boolean;
  confirmNoLimit: boolean;
}

// ── UC-010: parent data export (JSON / PDF) ──────────────────────────────

export type ExportFormat = 'JSON' | 'PDF' | 'BOTH';

export interface ExportSummaryResponse {
  id: string;
  childId: string;
  format: ExportFormat;
  token: string;
  signedUrlPath: string;
  createdAt: string;
  expiresAt: string;
  size: number;
}

export interface TriggerExportRequest {
  format: ExportFormat;
}

// ── UC-011: parent-initiated child-account deletion ──────────────────────

export type DeletionStatus = 'PENDING' | 'CONFIRMED' | 'DISCARDED';

export interface DeletionTriggerRequest {
  password: string;
  confirmationWord: string;
}

export interface DeletionRequestSummary {
  id: string;
  token: string;
  signedUrlPath: string;
  expiresAt: string;
  status: DeletionStatus;
}

export interface DeletionRecordResponse {
  id: string;
  childPseudonym: string;
  completedAt: string;
  dataCategories: string[];
}
