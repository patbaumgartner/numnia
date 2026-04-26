# ADR-007 - Test Pyramid: JUnit/AssertJ/Mockito + Testcontainers, Cucumber for BDD, Playwright for E2E

| Field | Value |
| --- | --- |
| Status | Accepted |
| Date | 2026-04-26 |
| Deciders | Numnia core team |
| References | NFR-ENG-002..006, SRS §4.1, §11.4, arc42 §8.9 |

## Context and Problem

Numnia mandates Test First, TDD for business logic, BDD for acceptance criteria, and CI-enforced quality gates (NFR-ENG-002..004). We need a coherent test pyramid that the small operations team can run locally and in CI in reasonable time, and that supports the version pins from ADR-001.

## Decision Drivers

- Test First / TDD discipline (NFR-ENG-002).
- BDD scenarios as executable acceptance criteria (NFR-ENG-003).
- Coverage thresholds: backend ≥ 80 % line / 70 % branch, frontend ≥ 70 % line (NFR-ENG-006).
- Realistic integration tests against PostgreSQL, Redis, and the object store.
- Stack pins from ADR-001 (Java 25, Spring Boot 4.0.6, Spring 7.0.7).

## Considered Options

- **Option A** - JUnit Jupiter 6.0.x + AssertJ 3.27.x + Mockito 5.23.x for unit, Testcontainers 2.0.x for integration, Cucumber-JVM ≥ 7.34.3 for backend BDD; Vitest 4.1.x + RTL for frontend unit, Playwright 1.59.x + @cucumber/cucumber 12.8.x for E2E.
- Option B - In-memory H2 instead of Testcontainers.
- Option C - Custom DSL instead of Gherkin.

## Decision

We choose **Option A**. The pyramid is:

- **Unit (most)**: JUnit Jupiter 6.0.x, AssertJ 3.27.x, Mockito 5.23.x (backend); Vitest 4.1.x + React Testing Library (frontend).
- **Integration**: Testcontainers 2.0.x — real PostgreSQL 18.3, Redis 8.6, S3-compatible object store.
- **BDD acceptance**: Cucumber-JVM ≥ 7.34.3 (`cucumber-spring 7.34.3` is the first release supporting Spring 7 / Spring Boot 4) for backend; @cucumber/cucumber 12.8.x for frontend/E2E.
- **E2E**: Playwright 1.59.x against a docker-compose stack.

## Consequences

- Positive: realistic tests, BDD scenarios in CI, no false-green from H2 quirks.
- Negative: integration tests need Docker — accepted; CI runners already have it.
- Follow-ups: enforce coverage thresholds in CI; fail merges on red BDD or coverage regression (NFR-ENG-004).

## Rejected Options

- Option B - H2 misrepresents PostgreSQL behaviour (JSONB, sequences, locking).
- Option C - Gherkin is the lingua franca for non-engineering stakeholders; a custom DSL would isolate them.

## Links

- ADR-001 (Stack Selection)
- arc42 §8.9 (Engineering quality)
