# ADR-007 - Bounded Contexts Aligned with SRS Chapters 6.x

| Field | Value |
| --- | --- |
| Status | Accepted |
| Date | 2026-04-26 |
| Deciders | Numnia core team |
| References | SRS §6, NFR-ENG-001, arc42 §5 |

## Context and Problem

Numnia spans several distinct concerns: identity/consent, learning/mastery, game/worlds, multiplayer/matchmaking, gamification, parent self-service, school/teacher, LiveOps/admin, safety/moderation, reporting. We need module boundaries that match how the SRS reasons about the system, so that each use case (`UC-XXX`) maps cleanly to one owning module and traceability is preserved (AIUP).

## Decision Drivers

- AIUP traceability: every PR references a `UC-XXX` and lives in one bounded context (NFR-ENG-001).
- High functional cohesion / low coupling between modules.
- Operability with a small team — module ownership must be clear without a per-module deployment.

## Considered Options

- **Option A** - One bounded context per SRS chapter 6.x area, each as a Spring Boot module inside the modulith (see ADR-011).
- Option B - Technical layering only (controller / service / repository) without domain modules.
- Option C - Per-feature microservices.

## Decision

We choose **Option A**. The backend is organized into the following bounded contexts, each aligned with an SRS §6 area: `identity`, `learning`, `game`, `multiplayer`, `gamification`, `parent`, `school`, `liveops`, `safety`, `reporting`. Cross-context calls go through public, intent-revealing APIs only; no direct repository sharing.

## Consequences

- Positive: one-to-one mapping from SRS §6 → module → use cases → tests; reviews and CI gates can be scoped per module.
- Negative: requires discipline to keep cross-module calls explicit — enforced via static module boundaries (e.g., ArchUnit) once the codebase exists.
- Follow-ups: add ArchUnit rules in the test stage to prevent boundary violations.

## Rejected Options

- Option B - blurs ownership, makes traceability painful.
- Option C - operationally too heavy for a 1-2 person team (see ADR-011).

## Links

- ADR-011 (Modulith over microservices)
- arc42 §5 (Building Block View)
