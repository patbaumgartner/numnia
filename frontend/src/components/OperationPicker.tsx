/**
 * OperationPicker — UC-003 step 1: child chooses an operation to train.
 *
 * UI copy is Swiss High German with umlauts, without sharp s
 * (NFR-I18N-002, NFR-I18N-004). FR-LEARN-002.
 */
import type { Operation } from '../api/types';

const OPERATIONS: ReadonlyArray<{ id: Operation; label: string }> = [
  { id: 'ADDITION', label: 'Plus rechnen' },
  { id: 'SUBTRACTION', label: 'Minus rechnen' },
  { id: 'MULTIPLICATION', label: 'Mal rechnen' },
  { id: 'DIVISION', label: 'Geteilt rechnen' },
];

export interface OperationPickerProps {
  onPick: (operation: Operation) => void;
  disabled?: boolean;
}

export default function OperationPicker({ onPick, disabled }: OperationPickerProps) {
  return (
    <fieldset aria-label="Rechenart waehlen">
      <legend>Welche Rechenart moechtest du ueben?</legend>
      <ul>
        {OPERATIONS.map((op) => (
          <li key={op.id}>
            <button
              type="button"
              onClick={() => onPick(op.id)}
              disabled={disabled}
              data-operation={op.id}
            >
              {op.label}
            </button>
          </li>
        ))}
      </ul>
    </fieldset>
  );
}
