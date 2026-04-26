---
name: unit-test
description: Generate unit and integration tests for a Numnia use case. Use this skill when asked to write or generate tests for a use case (UC-XXX), add missing test coverage, or scaffold JUnit/Vitest tests. Accepts a UC ID as argument.
---

# unit-test - Unit and Integration Tests

Invocation: `unit-test UC-005`

## Task

1. Read `docs/use_cases/UC-XXX-*.md` and identify business rules, main-flow steps and exception flows.
2. At least **one** dedicated test per business rule.
3. Backend (Java 25 LTS):
   - JUnit Jupiter 6.0.x + AssertJ 3.27.x + Mockito 5.23.x.
   - Integration tests with **Testcontainers 2.0.x** (PostgreSQL 18.3, Redis 8.6, object storage per ADR-001) instead of H2.
   - Naming: `methodUnderTest_state_expectedBehaviour`.
   - No `@Disabled`/`@Ignore` without an issue reference.
4. Frontend (TypeScript):
   - Vitest + React Testing Library.
   - Hooks and pure functions are mandatory to test; Babylon.js behind testable adapters.
5. Verify coverage: Backend ≥ 80% line / 70% branch, Frontend ≥ 70% line.

## Output

- Test classes under `backend/src/test/java/.../UCXXX*Test.java` and `*IT.java`.
- Frontend tests under `frontend/src/**/*.test.ts(x)`.
- Updated coverage report in CI.

## Rules

- No tests against mocks-of-mocks; prefer real persistence via Testcontainers.
- No time-dependent tests without a `Clock` abstraction.
- Test descriptions and identifiers in English.
