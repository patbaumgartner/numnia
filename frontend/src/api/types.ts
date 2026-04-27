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
}

export interface AnswerResultResponse {
  outcome: AnswerOutcome;
  currentSpeed: number;
  modeSuggestion: ModeSuggestion;
  starPointsBalance: number;
}

export interface SessionSummaryResponse {
  sessionId: string;
  totalTasks: number;
  correctTasks: number;
  starPointsBalance: number;
  masteryStatus: 'NOT_STARTED' | 'IN_CONSOLIDATION' | 'MASTERED';
}
