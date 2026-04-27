/**
 * Tests for TrainingPage — UC-003 frontend slice.
 *
 * Covers:
 *  - Operation picking starts a session and shows the first task (FR-LEARN-002).
 *  - Correct answer feedback (FR-LEARN-007).
 *  - Wrong answer keeps star points (BR-002).
 *  - Mode suggestion is rendered when the adaptive engine triggers (BR-003).
 *  - End session shows the summary (FR-LEARN-012).
 *  - Swiss High German copy without sharp s (NFR-I18N-002/004).
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import TrainingPage from './TrainingPage';
import * as api from '../api/client';

const CHILD_ID = '11111111-1111-1111-1111-111111111111';

describe('TrainingPage', () => {
  beforeEach(() => {
    sessionStorage.setItem('numnia_child_id', CHILD_ID);
  });

  afterEach(() => {
    sessionStorage.clear();
    vi.restoreAllMocks();
  });

  it('asks the child to sign in if no childId is in session storage', () => {
    sessionStorage.clear();
    render(<TrainingPage />);
    expect(screen.getByText(/Bitte zuerst anmelden/)).toBeInTheDocument();
  });

  it('uses no sharp s in any copy', () => {
    const { container } = render(<TrainingPage />);
    expect(container.textContent ?? '').not.toContain('ß');
  });

  it('starts a session and displays the first task when an operation is picked', async () => {
    vi.spyOn(api, 'startTrainingSession').mockResolvedValue({
      sessionId: 'sess-1',
      operation: 'ADDITION',
      difficulty: 1,
      speed: 2,
    });
    vi.spyOn(api, 'nextTrainingTask').mockResolvedValue({
      taskId: 't1',
      operation: 'ADDITION',
      operandA: 3,
      operandB: 4,
      difficulty: 1,
      speed: 2,
    });

    render(<TrainingPage />);
    await userEvent.click(screen.getByRole('button', { name: /Plus rechnen/ }));

    await waitFor(() => {
      expect(screen.getByLabelText('Aufgabe')).toHaveTextContent('3 + 4 = ?');
    });
  });

  it('shows positive feedback when the answer is correct', async () => {
    vi.spyOn(api, 'startTrainingSession').mockResolvedValue({
      sessionId: 'sess-1',
      operation: 'ADDITION',
      difficulty: 1,
      speed: 2,
    });
    vi.spyOn(api, 'nextTrainingTask').mockResolvedValue({
      taskId: 't1',
      operation: 'ADDITION',
      operandA: 3,
      operandB: 4,
      difficulty: 1,
      speed: 2,
    });
    vi.spyOn(api, 'submitTrainingAnswer').mockResolvedValue({
      outcome: 'CORRECT',
      currentSpeed: 2,
      modeSuggestion: 'NONE',
      starPointsBalance: 1,
    });

    render(<TrainingPage />);
    await userEvent.click(screen.getByRole('button', { name: /Plus rechnen/ }));
    await screen.findByLabelText('Aufgabe');
    await userEvent.type(screen.getByLabelText(/Deine Antwort/), '7');
    await userEvent.click(screen.getByRole('button', { name: /Antwort senden/ }));

    expect(await screen.findByText(/Super, das ist richtig/)).toBeInTheDocument();
    expect(screen.getByTestId('star-balance')).toHaveTextContent('1');
  });

  it('keeps star points when the answer is wrong (BR-002) and surfaces the mode suggestion (BR-003)', async () => {
    vi.spyOn(api, 'startTrainingSession').mockResolvedValue({
      sessionId: 'sess-1',
      operation: 'MULTIPLICATION',
      difficulty: 3,
      speed: 3,
    });
    vi.spyOn(api, 'nextTrainingTask').mockResolvedValue({
      taskId: 't1',
      operation: 'MULTIPLICATION',
      operandA: 6,
      operandB: 7,
      difficulty: 3,
      speed: 3,
    });
    vi.spyOn(api, 'submitTrainingAnswer').mockResolvedValue({
      outcome: 'WRONG',
      currentSpeed: 2,
      modeSuggestion: 'ACCURACY',
      starPointsBalance: 12,
    });

    render(<TrainingPage />);
    await userEvent.click(screen.getByRole('button', { name: /Mal rechnen/ }));
    await screen.findByLabelText('Aufgabe');
    await userEvent.type(screen.getByLabelText(/Deine Antwort/), '40');
    await userEvent.click(screen.getByRole('button', { name: /Antwort senden/ }));

    expect(await screen.findByText(/noch nicht ganz/)).toBeInTheDocument();
    expect(screen.getByTestId('star-balance')).toHaveTextContent('12');
    expect(screen.getByTestId('mode-suggestion')).toHaveTextContent(
      /Genauigkeits-Modus/,
    );
  });

  it('displays a summary after the session ends', async () => {
    vi.spyOn(api, 'startTrainingSession').mockResolvedValue({
      sessionId: 'sess-1',
      operation: 'ADDITION',
      difficulty: 1,
      speed: 2,
    });
    vi.spyOn(api, 'nextTrainingTask').mockResolvedValue({
      taskId: 't1',
      operation: 'ADDITION',
      operandA: 1,
      operandB: 2,
      difficulty: 1,
      speed: 2,
    });
    vi.spyOn(api, 'submitTrainingAnswer').mockResolvedValue({
      outcome: 'CORRECT',
      currentSpeed: 2,
      modeSuggestion: 'NONE',
      starPointsBalance: 1,
    });
    vi.spyOn(api, 'endTrainingSession').mockResolvedValue({
      sessionId: 'sess-1',
      totalTasks: 1,
      correctTasks: 1,
      starPointsBalance: 1,
      masteryStatus: 'IN_CONSOLIDATION',
    });

    render(<TrainingPage />);
    await userEvent.click(screen.getByRole('button', { name: /Plus rechnen/ }));
    await screen.findByLabelText('Aufgabe');
    await userEvent.type(screen.getByLabelText(/Deine Antwort/), '3');
    await userEvent.click(screen.getByRole('button', { name: /Antwort senden/ }));
    await screen.findByText(/Super, das ist richtig/);
    await userEvent.click(screen.getByRole('button', { name: /Training beenden/ }));

    expect(await screen.findByText(/Gut gemacht/)).toBeInTheDocument();
    expect(screen.getByText(/Richtig: 1 von 1/)).toBeInTheDocument();
  });
});
