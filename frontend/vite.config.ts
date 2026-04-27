import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/test-setup.ts'],
    globals: true,
    coverage: {
      provider: 'v8',
      reporter: ['text', 'lcov'],
      thresholds: {
        // NFR-ENG-006: frontend ≥ 70% line coverage
        lines: 70,
      },
    },
  },
});
