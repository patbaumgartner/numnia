# ADR-005 - OpenAPI 3.1 as the API Contract; GraphQL Only Where Aggregation Justifies It

| Field | Value |
| --- | --- |
| Status | Accepted |
| Date | 2026-04-26 |
| Deciders | Numnia core team |
| References | SRS §4.1, §9.1, NFR-SEC-001..003, NFR-ENG-003, arc42 §3, §5 |

## Context and Problem

Numnia exposes a backend to a single first-party SPA (React + Babylon.js). We need a contract format that supports contract-driven development, server-side validation, code/SDK generation, and clear versioning. Some screens (parent dashboard, teacher reports) require client-driven aggregation across several entities and would be expensive to model as fixed REST resources.

## Decision Drivers

- Contract-driven development and CI-enforceable contracts (NFR-ENG-003/004).
- Server-side validation of every payload (NFR-SEC-002).
- Single first-party client; no third-party API consumers in scope.
- Operability with a small team (NFR-OPS-001).

## Considered Options

- **Option A** - REST + WebSocket as the default; GraphQL only for selected aggregation endpoints (parent dashboard, teacher reports).
- Option B - GraphQL-first for all read paths, REST only for writes.
- Option C - REST-only, accept N+1 round trips on aggregation screens.

## Decision

We choose **Option A**:

- All synchronous APIs are described in **OpenAPI 3.1**.
- Real-time / multiplayer flows use **WebSocket** (see ADR-006).
- A small **GraphQL** read layer is permitted only for aggregation-heavy parent/teacher views; introduction of a new GraphQL endpoint requires a follow-up note in this ADR or a successor ADR.

## Consequences

- Positive: validated contracts, generated TypeScript client, schema-driven mocks for frontend tests.
- Negative: two API styles to learn — mitigated by limiting GraphQL to a small, well-bounded surface.
- Follow-ups: enforce OpenAPI breaking-change checks in CI; document the GraphQL surface alongside OpenAPI in the same `docs/api/` folder once introduced.

## Rejected Options

- Option B - introduces a heavy runtime, complicates authorization and rate limiting.
- Option C - hurts perceived performance on dashboard screens (NFR-PERF-001).

## Links

- ADR-006 (WebSocket scope)
- arc42 §3.2 (Technical Context)
