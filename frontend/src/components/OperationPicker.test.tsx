/**
 * Tests for OperationPicker.
 *
 * Covers FR-LEARN-002 (operation choice) and NFR-I18N-002/004 (Swiss High
 * German copy, no sharp s).
 */
import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import OperationPicker from './OperationPicker';

describe('OperationPicker', () => {
  it('lists all four operations with Swiss German labels', () => {
    render(<OperationPicker onPick={() => {}} />);
    expect(screen.getByRole('button', { name: /Plus rechnen/ })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Minus rechnen/ })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Mal rechnen/ })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Geteilt rechnen/ })).toBeInTheDocument();
  });

  it('uses no sharp s in any copy', () => {
    const { container } = render(<OperationPicker onPick={() => {}} />);
    expect(container.textContent ?? '').not.toContain('ß');
  });

  it('invokes onPick with the operation enum when a button is clicked', async () => {
    const onPick = vi.fn();
    render(<OperationPicker onPick={onPick} />);
    await userEvent.click(screen.getByRole('button', { name: /Mal rechnen/ }));
    expect(onPick).toHaveBeenCalledWith('MULTIPLICATION');
  });

  it('disables all buttons when disabled prop is set', () => {
    render(<OperationPicker onPick={() => {}} disabled />);
    screen.getAllByRole('button').forEach((b) => {
      expect(b).toBeDisabled();
    });
  });
});
