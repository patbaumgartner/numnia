// cucumber.cjs — Cucumber-JS configuration for Numnia E2E BDD tests.
// Run via: pnpm test (inside e2e/).
// Feature files live in features/; TypeScript step definitions in steps/.
// tsx loaded via NODE_OPTIONS in package.json scripts.

/** @type {import('@cucumber/cucumber').IConfiguration} */
module.exports = {
  default: {
    paths: ['features/**/*.feature'],
    require: [],
    import: ['support/**/*.ts', 'hooks/**/*.ts', 'steps/**/*.ts'],
    format: ['progress-bar', 'html:cucumber-report.html'],
    formatOptions: { snippetInterface: 'async-await' },
    publishQuiet: true,
  },
};
