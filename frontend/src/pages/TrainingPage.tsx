/**
 * TrainingPage — UC-003 child training mode entry point.
 *
 * Flow:
 *   1. Pick operation (FR-LEARN-002).
 *   2. Solve generated tasks (FR-LEARN-003..006).
 *   3. Show feedback after each answer (FR-LEARN-007..009, BR-002 BR-003).
 *   4. End session and display a small summary (FR-LEARN-012).
 *
 * UI copy is Swiss High German with umlauts, without sharp s
 * (NFR-I18N-002, NFR-I18N-004).
 */
import { useState } from 'react';
import OperationPicker from '../components/OperationPicker';
import { useTrainingSession } from '../hooks/useTrainingSession';
import type { Operation } from '../api/types';

const OPERATION_SYMBOL: Record<Operation, string> = {
  ADDITION: '+',
  SUBTRACTION: '−',
  MULTIPLICATION: '×',
  DIVISION: '÷',
};

const SUGGESTION_LABEL: Record<string, string> = {
  ACCURACY: 'Magst du im Genauigkeits-Modus weiterueben?',
  EXPLANATION: 'Wir koennen dir die Aufgabe gerne erklaeren.',
};

export default function TrainingPage() {
  const childId = sessionStorage.getItem('numnia_child_id') ?? '';
  const { state, start, submit, advance, finish } = useTrainingSession(childId);
  const [answer, setAnswer] = useState('');
  const [questionStartedAt, setQuestionStartedAt] = useState<number>(0);

  if (!childId) {
    return (
      <main>
        <h2>Training</h2>
        <p>Bitte zuerst anmelden.</p>
      </main>
    );
  }

  if (state.phase === 'idle' || state.phase === 'loading') {
    return (
      <main>
        <h2>Training waehlen</h2>
        {state.errorMessage && <p role="alert">{state.errorMessage}</p>}
        <OperationPicker
          onPick={(op: Operation) => {
            setQuestionStartedAt(Date.now());
            void start(op);
          }}
          disabled={state.phase === 'loading'}
        />
      </main>
    );
  }

  if (state.phase === 'finished' && state.summary) {
    return (
      <main>
        <h2>Gut gemacht!</h2>
        <p>
          Richtig: {state.summary.correctTasks} von {state.summary.totalTasks}.
        </p>
        <p data-testid="star-balance">Sternenpunkte: {state.summary.starPointsBalance}</p>
        <a href="/child">Zurueck zur Startseite</a>
      </main>
    );
  }

  return (
    <main>
      <h2>Aufgabe loesen</h2>
      <p data-testid="star-balance">Sternenpunkte: {state.starPointsBalance}</p>

      {state.task && state.phase === 'answering' && (
        <form
          onSubmit={(e) => {
            e.preventDefault();
            const value = Number(answer);
            if (!Number.isFinite(value)) return;
            const elapsed = Math.max(0, Date.now() - questionStartedAt);
            void submit(value, elapsed);
            setAnswer('');
          }}
        >
          <p aria-label="Aufgabe">
            {state.task.operandA} {OPERATION_SYMBOL[state.task.operation]}{' '}
            {state.task.operandB} = ?
          </p>
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
        </form>
      )}

      {state.phase === 'feedback' && state.lastResult && (
        <section aria-label="Rueckmeldung">
          {state.lastResult.outcome === 'CORRECT' ? (
            <p>Super, das ist richtig!</p>
          ) : state.lastResult.outcome === 'TIMEOUT' ? (
            <p>Die Zeit ist abgelaufen. Magst du es nochmals probieren?</p>
          ) : (
            <p>Leider noch nicht ganz. Sternenpunkte bleiben gleich.</p>
          )}
          {state.lastResult.modeSuggestion !== 'NONE' && (
            <p data-testid="mode-suggestion">
              {SUGGESTION_LABEL[state.lastResult.modeSuggestion]}
            </p>
          )}
          <button
            type="button"
            onClick={() => {
              setQuestionStartedAt(Date.now());
              void advance();
            }}
          >
            Naechste Aufgabe
          </button>
          <button type="button" onClick={() => void finish()}>
            Training beenden
          </button>
        </section>
      )}
    </main>
  );
}
