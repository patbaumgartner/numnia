// cucumber.cjs — Cucumber-JS configuration for Numnia E2E BDD tests.
// Run via: npx cucumber-js (or `pnpm test` inside e2e/).
// Feature files live in features/; TypeScript step definitions in steps/.
// Steps are loaded via tsx for zero-transpile TypeScript execution.

/** @type {import('@cucumber/cucumber').IConfiguration} */
module.exports = {
  default: {
    paths: ['features/**/*.feature'],
    require: [],
    import: ['steps/**/*.ts'],
    loader: ['tsx'],
    format: ['progress-bar', 'html:cucumber-report.html'],
    formatOptions: { snippetInterface: 'async-await' },
    publishQuiet: true,
  },
};
