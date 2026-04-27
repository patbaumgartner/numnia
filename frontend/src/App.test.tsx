import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, it, expect } from 'vitest';
import App from './App.tsx';

describe('App', () => {
  it('renders the Numnia heading in Swiss High German', () => {
    render(
      <MemoryRouter initialEntries={['/']}>
        <App />
      </MemoryRouter>,
    );
    expect(
      screen.getByRole('heading', { level: 1, name: /Numnia/i }),
    ).toBeInTheDocument();
  });

  it('does not contain the sharp s (ß) character', () => {
    const { container } = render(
      <MemoryRouter initialEntries={['/']}>
        <App />
      </MemoryRouter>,
    );
    expect(container.textContent).not.toContain('ß');
  });
});
