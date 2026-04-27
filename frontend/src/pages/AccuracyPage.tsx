/**
 * AccuracyPage — UC-004 child accuracy mode (G0, no time pressure).
 *
 * Differences vs. {@code TrainingPage} (UC-003):
 *   - The session is started via {@code startAccuracySession}; speed is fixed
 *     to 0 (G0) server-side.
 *   - The UI does NOT render a timer; the explicit message
 *     "So viel Zeit, wie du brauchst" reassures the child.
 *   - A "Erklaerung zeigen" button reveals animated solution steps
 *     (FR-LEARN-008); the same task remains answerable afterwards.
 *   - Errors do not deduct star points (BR-002, enforced by the backend).
 *
 * UI copy is Swiss High German with umlauts, without sharp s
 * (NFR-I18N-002, NFR-I18N-004).
 */
import { useState } from 'react';
import OperationPicker from '../components/OperationPicker';
import {
  startAccuracySession,
  nextTrainingTask,
  submitTrainingAnswer,
  endTrainingSession,
  getTrainingExplanation,
} from '../api/client';
import type {
  AnswerResultResponse,
  ExplanationStepsResponse,
  Operation,
  SessionSummaryResponse,
  TrainingTaskResponse,
} from '../api/types';

const OPERATION_SYMBOL: Record<Operation, string> = {
  ADDITION: '+',
  SUBTRACTION: '−',
  MULTIPLICATION: '×',
  DIVISION: '÷',
};

type Phase = 'idle' | 'loading' | 'answering' | 'feedback' | 'finished';

export default function AccuracyPage() {
  const childId = sessionStorage.getItem('numnia_child_id') ?? '';
  const [phase, setPhase] = useState<Phase>('idle');
  const [sessionId, setSessionId] = useState<string | null>(null);
  const [task, setTask] = useState<TrainingTaskResponse | null>(null);
  const [answer, setAnswer] = useState('');
  const [lastResult, setLastResult] = useState<AnswerResultResponse | null>(null);
  const [summary, setSummary] = useState<SessionSummaryResponse | null>(null);
  const [explanation, setExplanation] = useState<ExplanationStepsResponse | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  if (!childId) {
    return (
      <main>
        <h2>Genauigkeits-Modus</h2>
        <p>Bitte zuerst anmelden.</p>
      </main>
    );
  }

  if (phase === 'idle' || phase === 'loading') {
    return (
      <main>
        <h2>Genauigkeits-Modus</h2>
        <p>So viel Zeit, wie du brauchst &mdash; in Ruhe rechnen.</p>
        {errorMessage && <p role="alert">{errorMessage}</p>}
        <OperationPicker
          onPick={async (op: Operation) => {
            setPhase('loading');
            setErrorMessage(null);
            try {
              const session = await startAccuracySession(childId, { operation: op });
              const firstTask = await nextTrainingTask(session.sessionId);
              setSessionId(session.sessionId);
              setTask(firstTask);
              setPhase('answering');
            } catch (e) {
              setErrorMessage(messageFor(e));
              setPhase('idle');
            }
          }}
          disabled={phase === 'loading'}
        />
      </main>
    );
  }

  if (phase === 'finished' && summary) {
    return (
      <main>
        <h2>Schoen gemacht!</h2>
        <p>
          Richtig: {summary.correctTasks} von {summary.totalTasks}.
        </p>
        <p data-testid="star-balance">Sternenpunkte: {summary.starPointsBalance}</p>
        <a href="/child">Zurueck zur Startseite</a>
      </main>
    );
  }

  return (
    <main>
      <h2>Genauigkeits-Modus</h2>
      <p>So viel Zeit, wie du brauchst.</p>
      <p data-testid="star-balance">
        Sternenpunkte: {lastResult?.starPointsBalance ?? 0}
      </p>

      {task && phase === 'answering' && (
        <>
          <p aria-label="Aufgabe">
            {task.operandA} {OPERATION_SYMBOL[task.operation]} {task.operandB} = ?
          </p>
          <form
            onSubmit={async (e) => {
              e.preventDefault();
              const value = Number(answer);
              if (!Number.isFinite(value) || !sessionId) return;
              setPhase('loading');
              try {
                const result = await submitTrainingAnswer(sessionId, value, 0);
                setLastResult(result);
                setAnswer('');
                setPhase('feedback');
              } catch (err) {
                setErrorMessage(messageFor(err));
                setPhase('answering');
              }
            }}
          >
            <label>
              Deine Antwort:
              <input
                type="number"
                inputMode="numeric"
                value={answer}
                onChange={(e) => setAnswer(e.target.value)}
                autoFocus
                required
              />
            </label>
            <button type="submit">Antwort senden</button>
            <button
              type="button"
              onClick={async () => {
                if (!sessionId) return;
                try {
                  const steps = await getTrainingExplanation(sessionId);
                  setExplanation(steps);
                } catch (err) {
                  setErrorMessage(messageFor(err));
                }
              }}
            >
              Erklaerung zeigen
            </button>
          </form>

          {explanation && (
            <section aria-label="Erklaerung">
              <h3>Loesungsschritte</h3>
              <ol aria-label="Loesungsschritte">
                {explanation.steps.map((step, i) => (
                  <li key={i}>{step}</li>
                ))}
              </ol>
              <button type="button" onClick={() => setExplanation(null)}>
                Schliessen
              </button>
            </section>
          )}
        </>
      )}

      {phase === 'feedback' && lastResult && (
        <section aria-label="Rueckmeldung">
          {lastResult.outcome === 'CORRECT' ? (
            <p>Super, das ist richtig!</p>
          ) : (
            <p>Leider noch nicht ganz. Sternenpunkte bleiben gleich.</p>
          )}
          <button
            type="button"
            onClick={async () => {
              if (!sessionId) return;
              setPhase('loading');
              try {
                const next = await nextTrainingTask(sessionId);
                setTask(next);
                setExplanation(null);
                setLastResult((prev) => prev);
                setPhase('answering');
              } catch (err) {
                setErrorMessage(messageFor(err));
                setPhase('feedback');
              }
            }}
          >
            Naechste Aufgabe
          </button>
          <button
            type="button"
            onClick={async () => {
              if (!sessionId) return;
              try {
                const sum = await endTrainingSession(sessionId);
                setSummary(sum);
                setPhase('finished');
              } catch (err) {
                setErrorMessage(messageFor(err));
              }
            }}
          >
            Beenden
          </button>
        </section>
      )}
    </main>
  );
}

function messageFor(error: unknown): string {
  if (error && typeof error === 'object' && 'message' in error) {
    return String((error as { message: string }).message);
  }
  return 'Etwas ist schiefgelaufen. Bitte erneut versuchen.';
}
