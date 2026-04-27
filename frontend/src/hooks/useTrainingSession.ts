/**
 * useTrainingSession — React hook that orchestrates the UC-003 client flow:
 *
 *   start → nextTask → submitAnswer (×N) → endSession.
 *
 * Encapsulates session/state transitions and surfaces only what the UI needs.
 *
 * FR-LEARN-001..009, BR-002 (errors do not deduct star points), BR-003
 * (adaptive engine reaction is reflected in {@link AnswerResultResponse}).
 */
import { useCallback, useState } from 'react';
import {
  startTrainingSession,
  nextTrainingTask,
  submitTrainingAnswer,
  submitTrainingTimeout,
  endTrainingSession,
} from '../api/client';
import type {
  AnswerResultResponse,
  Operation,
  SessionSummaryResponse,
  TrainingTaskResponse,
} from '../api/types';

export type TrainingPhase = 'idle' | 'loading' | 'answering' | 'feedback' | 'finished';

export interface TrainingState {
  phase: TrainingPhase;
  sessionId: string | null;
  task: TrainingTaskResponse | null;
  lastResult: AnswerResultResponse | null;
  summary: SessionSummaryResponse | null;
  starPointsBalance: number;
  errorMessage: string | null;
}

const INITIAL: TrainingState = {
  phase: 'idle',
  sessionId: null,
  task: null,
  lastResult: null,
  summary: null,
  starPointsBalance: 0,
  errorMessage: null,
};

export function useTrainingSession(childId: string) {
  const [state, setState] = useState<TrainingState>(INITIAL);

  const start = useCallback(
    async (operation: Operation) => {
      setState((s) => ({ ...s, phase: 'loading', errorMessage: null }));
      try {
        const session = await startTrainingSession(childId, { operation });
        const task = await nextTrainingTask(session.sessionId);
        setState((s) => ({
          ...s,
          phase: 'answering',
          sessionId: session.sessionId,
          task,
        }));
      } catch (e) {
        setState((s) => ({
          ...s,
          phase: 'idle',
          errorMessage: messageFor(e),
        }));
      }
    },
    [childId],
  );

  const submit = useCallback(
    async (answer: number, responseTimeMs: number) => {
      if (!state.sessionId) return;
      setState((s) => ({ ...s, phase: 'loading' }));
      try {
        const result = await submitTrainingAnswer(state.sessionId, answer, responseTimeMs);
        setState((s) => ({
          ...s,
          phase: 'feedback',
          lastResult: result,
          starPointsBalance: result.starPointsBalance,
        }));
      } catch (e) {
        setState((s) => ({ ...s, phase: 'answering', errorMessage: messageFor(e) }));
      }
    },
    [state.sessionId],
  );

  const timeout = useCallback(async () => {
    if (!state.sessionId) return;
    try {
      const result = await submitTrainingTimeout(state.sessionId);
      setState((s) => ({
        ...s,
        phase: 'feedback',
        lastResult: result,
        starPointsBalance: result.starPointsBalance,
      }));
    } catch (e) {
      setState((s) => ({ ...s, errorMessage: messageFor(e) }));
    }
  }, [state.sessionId]);

  const advance = useCallback(async () => {
    if (!state.sessionId) return;
    setState((s) => ({ ...s, phase: 'loading' }));
    try {
      const task = await nextTrainingTask(state.sessionId);
      setState((s) => ({ ...s, phase: 'answering', task, lastResult: null }));
    } catch (e) {
      setState((s) => ({ ...s, phase: 'feedback', errorMessage: messageFor(e) }));
    }
  }, [state.sessionId]);

  const finish = useCallback(async () => {
    if (!state.sessionId) return;
    try {
      const summary = await endTrainingSession(state.sessionId);
      setState((s) => ({
        ...s,
        phase: 'finished',
        summary,
        starPointsBalance: summary.starPointsBalance,
      }));
    } catch (e) {
      setState((s) => ({ ...s, errorMessage: messageFor(e) }));
    }
  }, [state.sessionId]);

  return { state, start, submit, timeout, advance, finish };
}

function messageFor(error: unknown): string {
  if (error && typeof error === 'object' && 'message' in error) {
    return String((error as { message: string }).message);
  }
  return 'Etwas ist schiefgelaufen. Bitte erneut versuchen.';
}
