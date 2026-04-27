/**
 * Unit tests for ChildProfileForm (UC-001, FR-002, FR-003, NFR-I18N-002).
 *
 * Tests TDD Red→Green discipline — written before the component.
 */
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import ChildProfileForm, { FANTASY_NAMES, AVATAR_MODELS } from './ChildProfileForm';
import * as client from '../api/client';

vi.mock('../api/client', () => ({
  createChildProfile: vi.fn(),
  ApiError: class ApiError extends Error {
    status: number;
    code: string;
    constructor(status: number, code: string, message: string) {
      super(message);
      this.name = 'ApiError';
      this.status = status;
      this.code = code;
    }
  },
}));

const mockedCreateChildProfile = vi.mocked(client.createChildProfile);
const CURRENT_YEAR = new Date().getFullYear();
const VALID_YEAR = String(CURRENT_YEAR - 9); // 9-year-old is within 7-12

describe('ChildProfileForm', () => {
  const onSuccess = vi.fn();
  const user = userEvent.setup();

  beforeEach(() => {
    vi.clearAllMocks();
    render(<ChildProfileForm parentId="parent-uuid" onSuccess={onSuccess} />);
  });

  // ── Catalog integrity ───────────────────────────────────────────────────

  it('renders exactly 26 fantasy name options (plus placeholder)', () => {
    const select = screen.getByLabelText('Fantasiename');
    const options = Array.from(select.querySelectorAll('option'));
    // 1 placeholder + 26 names
    expect(options).toHaveLength(27);
  });

  it('renders only names from the predefined catalog', () => {
    const select = screen.getByLabelText('Fantasiename');
    const optionValues = Array.from(select.querySelectorAll('option'))
      .map(o => o.value)
      .filter(v => v !== '');
    for (const name of optionValues) {
      expect(FANTASY_NAMES).toContain(name);
    }
  });

  it('renders exactly 8 avatar model options (plus placeholder)', () => {
    const select = screen.getByLabelText('Avatar');
    const options = Array.from(select.querySelectorAll('option'));
    expect(options).toHaveLength(9); // 1 placeholder + 8
  });

  it('renders only models from the predefined catalog', () => {
    const select = screen.getByLabelText('Avatar');
    const optionValues = Array.from(select.querySelectorAll('option'))
      .map(o => o.value)
      .filter(v => v !== '');
    for (const model of optionValues) {
      expect(AVATAR_MODELS).toContain(model);
    }
  });

  // ── Required-field validation ────────────────────────────────────────────

  it('shows errors for all empty fields on submit', async () => {
    await user.click(screen.getByRole('button', { name: /Profil erstellen/i }));
    expect(
      await screen.findByText(/Bitte waehle einen Fantasienamen/i),
    ).toBeInTheDocument();
    expect(screen.getByText('Geburtsjahr ist erforderlich.')).toBeInTheDocument();
    expect(screen.getByText(/Bitte waehle einen Avatar/i)).toBeInTheDocument();
  });

  // ── Year-of-birth boundary tests ─────────────────────────────────────────

  it('accepts a year-of-birth for a 7-year-old', async () => {
    mockedCreateChildProfile.mockResolvedValueOnce({
      childProfileId: 'cid',
      pseudonym: 'Luna',
    });
    const yearFor7 = String(CURRENT_YEAR - 7);
    await user.selectOptions(screen.getByLabelText('Fantasiename'), 'Luna');
    await user.clear(screen.getByLabelText('Geburtsjahr'));
    await user.type(screen.getByLabelText('Geburtsjahr'), yearFor7);
    await user.selectOptions(screen.getByLabelText('Avatar'), 'star');
    await user.click(screen.getByRole('button', { name: /Profil erstellen/i }));
    await waitFor(() => expect(mockedCreateChildProfile).toHaveBeenCalled());
  });

  it('accepts a year-of-birth for a 12-year-old', async () => {
    mockedCreateChildProfile.mockResolvedValueOnce({
      childProfileId: 'cid',
      pseudonym: 'Nova',
    });
    const yearFor12 = String(CURRENT_YEAR - 12);
    await user.selectOptions(screen.getByLabelText('Fantasiename'), 'Nova');
    await user.clear(screen.getByLabelText('Geburtsjahr'));
    await user.type(screen.getByLabelText('Geburtsjahr'), yearFor12);
    await user.selectOptions(screen.getByLabelText('Avatar'), 'moon');
    await user.click(screen.getByRole('button', { name: /Profil erstellen/i }));
    await waitFor(() => expect(mockedCreateChildProfile).toHaveBeenCalled());
  });

  it('rejects a year-of-birth for a 6-year-old (too young)', async () => {
    const yearFor6 = String(CURRENT_YEAR - 6);
    await user.selectOptions(screen.getByLabelText('Fantasiename'), 'Astra');
    await user.clear(screen.getByLabelText('Geburtsjahr'));
    await user.type(screen.getByLabelText('Geburtsjahr'), yearFor6);
    await user.selectOptions(screen.getByLabelText('Avatar'), 'cloud');
    await user.click(screen.getByRole('button', { name: /Profil erstellen/i }));
    expect(
      await screen.findByText(/Kinder im Alter von 7 bis 12/i),
    ).toBeInTheDocument();
    expect(mockedCreateChildProfile).not.toHaveBeenCalled();
  });

  it('rejects a year-of-birth for a 13-year-old (too old)', async () => {
    const yearFor13 = String(CURRENT_YEAR - 13);
    await user.selectOptions(screen.getByLabelText('Fantasiename'), 'Echo');
    await user.clear(screen.getByLabelText('Geburtsjahr'));
    await user.type(screen.getByLabelText('Geburtsjahr'), yearFor13);
    await user.selectOptions(screen.getByLabelText('Avatar'), 'fire');
    await user.click(screen.getByRole('button', { name: /Profil erstellen/i }));
    expect(
      await screen.findByText(/Kinder im Alter von 7 bis 12/i),
    ).toBeInTheDocument();
    expect(mockedCreateChildProfile).not.toHaveBeenCalled();
  });

  // ── Successful submission ────────────────────────────────────────────────

  it('calls createChildProfile with correct payload on valid submission', async () => {
    mockedCreateChildProfile.mockResolvedValueOnce({
      childProfileId: 'cid-1',
      pseudonym: 'Orion',
    });
    await user.selectOptions(screen.getByLabelText('Fantasiename'), 'Orion');
    await user.clear(screen.getByLabelText('Geburtsjahr'));
    await user.type(screen.getByLabelText('Geburtsjahr'), VALID_YEAR);
    await user.selectOptions(screen.getByLabelText('Avatar'), 'sun');
    await user.click(screen.getByRole('button', { name: /Profil erstellen/i }));

    await waitFor(() => {
      expect(mockedCreateChildProfile).toHaveBeenCalledWith('parent-uuid', {
        pseudonym: 'Orion',
        yearOfBirth: parseInt(VALID_YEAR, 10),
        avatarBaseModel: 'sun',
      });
    });
  });

  it('calls onSuccess with childId and pseudonym on success', async () => {
    mockedCreateChildProfile.mockResolvedValueOnce({
      childProfileId: 'cid-2',
      pseudonym: 'Vega',
    });
    await user.selectOptions(screen.getByLabelText('Fantasiename'), 'Vega');
    await user.clear(screen.getByLabelText('Geburtsjahr'));
    await user.type(screen.getByLabelText('Geburtsjahr'), VALID_YEAR);
    await user.selectOptions(screen.getByLabelText('Avatar'), 'wind');
    await user.click(screen.getByRole('button', { name: /Profil erstellen/i }));

    await waitFor(() => {
      expect(onSuccess).toHaveBeenCalledWith('cid-2', 'Vega');
    });
  });

  // ── Server error surfacing ────────────────────────────────────────────────

  it('shows server 422 error message to the user', async () => {
    const { ApiError } = await import('../api/client');
    mockedCreateChildProfile.mockRejectedValueOnce(
      new ApiError(422, 'INVALID_CHILD_PROFILE', 'Das Geburtsjahr ist ungueltig.'),
    );
    await user.selectOptions(screen.getByLabelText('Fantasiename'), 'Kite');
    await user.clear(screen.getByLabelText('Geburtsjahr'));
    await user.type(screen.getByLabelText('Geburtsjahr'), VALID_YEAR);
    await user.selectOptions(screen.getByLabelText('Avatar'), 'rock');
    await user.click(screen.getByRole('button', { name: /Profil erstellen/i }));
    expect(
      await screen.findByText('Das Geburtsjahr ist ungueltig.'),
    ).toBeInTheDocument();
  });

  // ── NFR-I18N-004: no sharp s ─────────────────────────────────────────────

  it('does not contain the sharp s (ß) anywhere', () => {
    expect(document.body.textContent).not.toContain('ß');
  });
});
