---
name: implement
description: Implement a Numnia use case end-to-end (Spring Boot 4.0.6 backend + React 19 / Babylon.js 9.4 frontend) following Test First / TDD. Use this skill when asked to implement a use case (UC-XXX), scaffold a feature, or write production code for a specific use case.
allowed-tools: run_in_terminal
---

# implement - Implement a Use Case

Invoke with a UC ID as argument: `implement UC-005`

## Preconditions

- `docs/use_cases/UC-XXX-*.md` exists and is consolidated.
- `docs/entity_model.md` covers the required entities (otherwise run `entity-model` skill first).
- Stack scaffolding exists (`backend/`, `frontend/`, `compose.yaml`).

## Procedure (Test First, TDD)

1. **Read the use case**: actors, preconditions, main flow, business rules, BDD scenarios.
2. **Persist BDD scenarios as Cucumber features**:
   - Backend-only logic: `backend/src/test/resources/features/UC-XXX.feature`
   - UI journey: `e2e/features/UC-XXX.feature`
3. **Scaffold step definitions** so tests fail (Red).
4. **Implement the backend** (minimally to Green at each step):
   - REST/WebSocket contract (OpenAPI 3.1) first.
   - Service layer (TDD with JUnit Jupiter 6.0.x + Mockito 5.23.x + AssertJ 3.27.x).
   - Persistence with Spring Data JPA / jOOQ (per ADR), migrations via Flyway 12.4.x.
   - Authentication/authorization enforced server-side (see security instructions).
5. **Implement the frontend** (TDD with Vitest 4.1.x + RTL):
   - State, hooks, UI components generated against the OpenAPI spec.
   - 3D effects behind testable adapters; keep Babylon.js 9.4.x code minimal and isolated.
6. **Refactor** after Green; eliminate code smells (SOLID).
7. **Trace in the PR**: list `UC-XXX` and all touched `FR-/NFR-` IDs.

## Quality gates (mandatory)

- The UC's Cucumber feature passes end to end.
- Coverage thresholds met (Backend ≥ 80% line / 70% branch, Frontend ≥ 70% line).
- No regression in other UCs.
- Security/privacy checklist completed.
- arc42 update for structural changes, ADR for architectural decisions.

## Forbidden

- Alternative library/framework without an ADR.
- Mocks in production code.
- `TODO`/`FIXME` without an issue reference.
- Personal data in logs.
