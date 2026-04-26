---
description: Test First, TDD and BDD - mandatory engineering discipline for every business-logic change in backend and frontend.
applyTo: "backend/**/*.java,frontend/src/**/*.{ts,tsx},e2e/**/*.{ts,feature}"
---

# Test First, TDD, BDD - Numnia

## Before writing code

1. Read the related use case under `docs/use_cases/UC-XXX-*.md`.
2. Copy every BDD scenario from the use-case spec **verbatim** into a Cucumber feature file under `e2e/features/UC-XXX.feature` (or backend `src/test/resources/features/`).
3. Write step definitions that initially **fail** (Red).
4. Write the minimal production code until they pass (Green).
5. Refactor in small steps; all tests stay green.

## Backend (Java 25 LTS / Spring Boot 4.0.6)

- **JUnit Jupiter 6.0.x**, **AssertJ 3.27.x**, **Mockito 5.23.x**; no `@Ignore`/`@Disabled` without an in-code comment plus issue reference.
- Integration tests use **Testcontainers 2.0.x** against real **PostgreSQL 18.3** / **Redis 8.6** / object storage per ADR-003.
- BDD scenarios run as **Cucumber-JVM ≥ 7.34.3** (cucumber-spring) features in the backend when they exercise backend logic only; otherwise as E2E.
- Database migrations: **Flyway 12.4.x**.
- Test naming: `methodUnderTest_state_expectedBehaviour`.
- Coverage thresholds: ≥ 80% line, ≥ 70% branch (NFR-ENG-006). The PR fails otherwise.

## Frontend (TypeScript 6.0 / React 19.2 / Babylon.js 9.4)

- **Vitest 4.1.x** + React Testing Library for component and hook tests.
- **Babylon.js 9.4.x** scenes are encapsulated behind their own adapters and remain unit-testable.
- **Playwright 1.59.x** + **@cucumber/cucumber 12.8.x** for E2E/BDD; prefer semantic locators (role, label, test-id). **No** `Thread.sleep` / `waitForTimeout` equivalents.
- Coverage threshold: ≥ 70% line (NFR-ENG-006).

## Mandatory Definition of Done

- All BDD scenarios for the UC are green and executed in CI.
- Unit and integration tests green, thresholds met.
- Security and privacy checklist completed (authn/authz, validation, audit log, data minimization).
- Documentation update done (UC, arc42, ADR).
- Manual steps are not a substitute for tests.

Sources: NFR-ENG-001..006, SRS §11.4, AGENTS.md §3.
