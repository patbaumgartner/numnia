/**
 * AccuracyPage — UC-004 child accuracy mode (G0, no time pressure).
 *
 * Covers:
 *  - Sign-in gate when no childId is in session storage.
 *  - Picking an operation starts an *accuracy* session via the backend
 *    (FR-GAME-001/002, BR-001 = G0, no timer).
 *  - The UI does NOT render a timer when the task carries `timed=false`.
 *  - "Show explanation" reveals animated solution steps (FR-LEARN-008).
 *  - Wrong answer does not reduce star points (BR-002).
 *  - Swiss High German copy without sharp s (NFR-I18N-002/004).
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import AccuracyPage from './AccuracyPage';
import * as api from '../api/client';

const CHILD_ID = '22222222-2222-2222-2222-222222222222';

describe('AccuracyPage', () => {
  beforeEach(() => {
    sessionStorage.setItem('numnia_child_id', CHILD_ID);
  });

  afterEach(() => {
    sessionStorage.clear();
    vi.restoreAllMocks();
  });

  it('asks the child to sign in when no childId is in session storage', () => {
    sessionStorage.clear();
    render(<AccuracyPage />);
    expect(screen.getByText(/Bitte zuerst anmelden/)).toBeInTheDocument();
  });

  it('uses no sharp s in any copy', () => {
    const { container } = render(<AccuracyPage />);
    expect(container.textContent ?? '').not.toContain('ß');
  });

  it('explicitly tells the child that there is no time pressure (BR-001)', () => {
    render(<AccuracyPage />);
    expect(
      screen.getByText(/So viel Zeit, wie du brauchst/i),
    ).toBeInTheDocument();
  });

  it('starts an accuracy session and shows the task without any timer', async () => {
    vi.spyOn(api, 'startAccuracySession').mockResolvedValue({
      sessionId: 'sess-acc-1',
      operation: 'SUBTRACTION',
      difficulty: 1,
      speed: 0,
    });
    vi.spyOn(api, 'nextTrainingTask').mockResolvedValue({
      taskId: 't1',
      operation: 'SUBTRACTION',
      operandA: 9,
      operandB: 4,
      difficulty: 1,
      speed: 0,
      timed: false,
    });

    render(<AccuracyPage />);
    await userEvent.click(screen.getByRole('button', { name: /Minus rechnen/ }));

    await waitFor(() => {
      expect(screen.getByLabelText('Aufgabe')).toHaveTextContent('9 − 4 = ?');
    });
    expect(screen.queryByTestId('countdown-timer')).not.toBeInTheDocument();
    expect(screen.queryByRole('timer')).not.toBeInTheDocument();
  });

  it('shows animated solution steps when "Erklaerung zeigen" is selected', async () => {
    vi.spyOn(api, 'startAccuracySession').mockResolvedValue({
      sessionId: 'sess-acc-2',
      operation: 'ADDITION',
      difficulty: 1,
      speed: 0,
    });
    vi.spyOn(api, 'nextTrainingTask').mockResolvedValue({
      taskId: 't1',
      operation: 'ADDITION',
      operandA: 3,
      operandB: 5,
      difficulty: 1,
      speed: 0,
      timed: false,
    });
    vi.spyOn(api, 'getTrainingExplanation').mockResolvedValue({
      taskId: 't1',
      operation: 'ADDITION',
      steps: [
        'Schau die Aufgabe an: 3 + 5.',
        'Stelle die Zahlen nebeneinander.',
        'Loesung: 3 + 5 = 8.',
      ],
    });

    render(<AccuracyPage />);
    await userEvent.click(screen.getByRole('button', { name: /Plus rechnen/ }));
    await screen.findByLabelText('Aufgabe');
    await userEvent.click(
      screen.getByRole('button', { name: /Erklaerung zeigen/i }),
    );

    const explanation = await screen.findByRole('list', {
      name: /Loesungsschritte/i,
    });
    expect(explanation).toBeInTheDocument();
    expect(explanation.querySelectorAll('li').length).toBeGreaterThanOrEqual(2);

    // After the explanation closes, the same task is still answerable.
    expect(screen.getByLabelText('Aufgabe')).toHaveTextContent('3 + 5 = ?');
    expect(screen.getByLabelText(/Deine Antwort/)).toBeEnabled();
  });

  it('keeps star points when the answer is wrong in accuracy mode (BR-002)', async () => {
    vi.spyOn(api, 'startAccuracySession').mockResolvedValue({
      sessionId: 'sess-acc-3',
      operation: 'ADDITION',
      difficulty: 1,
      speed: 0,
    });
    vi.spyOn(api, 'nextTrainingTask').mockResolvedValue({
      taskId: 't1',
      operation: 'ADDITION',
      operandA: 2,
      operandB: 3,
      difficulty: 1,
      speed: 0,
      timed: false,
    });
    vi.spyOn(api, 'submitTrainingAnswer').mockResolvedValue({
      outcome: 'WRONG',
      currentSpeed: 0,
      modeSuggestion: 'NONE',
      starPointsBalance: 8,
    });

    render(<AccuracyPage />);
    await userEvent.click(screen.getByRole('button', { name: /Plus rechnen/ }));
    await screen.findByLabelText('Aufgabe');
    await userEvent.type(screen.getByLabelText(/Deine Antwort/), '99');
    await userEvent.click(
      screen.getByRole('button', { name: /Antwort senden/ }),
    );

    expect(await screen.findByText(/noch nicht ganz/)).toBeInTheDocument();
    expect(screen.getByTestId('star-balance')).toHaveTextContent('8');
  });
});
