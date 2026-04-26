---
name: e2e-test
description: Generate end-to-end BDD tests for a Numnia use case using Playwright and Cucumber. Use this skill when asked to write E2E tests, create feature files, implement step definitions, or add Playwright scenarios for a use case (UC-XXX).
---

# e2e-test - E2E + BDD

Invocation: `e2e-test UC-005`

## Task

1. Read `docs/use_cases/UC-XXX-*.md`.
2. Copy **all** Gherkin scenarios verbatim into `e2e/features/UC-XXX.feature`.
3. Implement step definitions in TypeScript under `e2e/steps/`.
4. Use **Playwright 1.59.x** with semantic locators (role, label, test-id).
5. **Forbidden**: `page.waitForTimeout`, raw XPath, `Thread.sleep` equivalents.
6. Test data: via a backend setup endpoint or a seed migration under `e2e/seed/`.
7. Targets the local `compose.yaml` setup (`http://localhost:8080`).

## Output

- `e2e/features/UC-XXX.feature`
- `e2e/steps/UC-XXX.steps.ts`
- Page objects/adapters under `e2e/pages/` if needed
- CI job entry if newly introduced

## Rules

- BDD scenarios must not know implementation details (black box).
- Feature and scenario text in English.
- Tests are deterministic and parallelizable.
