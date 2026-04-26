# ADR-009 - Modulithic Spring Boot Backend Instead of Microservices

| Field | Value |
| --- | --- |
| Status | Accepted |
| Date | 2026-04-26 |
| Deciders | Numnia core team |
| References | NFR-OPS-001, SRS §D-10, arc42 §5, §7 |

## Context and Problem

Numnia consists of multiple bounded contexts (see ADR-005), but is operated by a 1-2 person part-time team and hosted on a single Swiss node (ADR-008). We must decide whether to deploy each context as its own service (microservices) or as one cohesive backend with strong internal module boundaries (modulith), and which tooling enforces those boundaries.

## Decision Drivers

- Operability with a small team, no 24/7 staff (NFR-OPS-001).
- Single-node Swiss hosting (NFR-OPS-003).
- Clear module boundaries from ADR-005, enforceable inside one process.
- Releases R1-R3 do not require independent scaling per bounded context.

## Considered Options

- **Option A** - Modulithic Spring Boot 4.0.6 backend with **Spring Modulith 2.0.6** to define, verify, test and document bounded-context modules (ADR-005) inside one deployable.
- Option B - Modulith without Spring Modulith, boundaries enforced by hand-rolled ArchUnit rules.
- Option C - Microservices per bounded context, communicating over REST / messaging.
- Option D - Two services only (web API + WebSocket gateway).

## Decision

We choose **Option A**. The backend is one Spring Boot 4.0.6 application composed of the bounded-context modules from ADR-005, plus a separately scalable WebSocket node sharing the same codebase via configuration.

**Spring Modulith 2.0.6** (current stable, supports Spring Boot 4.0.6) provides:

- Module discovery from Java package structure (`numnia.<context>` as direct sub-packages of the application root).
- Build-time verification of module boundaries via `ApplicationModules.of(Application.class).verify()` in a JUnit test.
- Per-module integration tests via `@ApplicationModuleTests`.
- Generated PlantUML component diagrams and module canvases under `target/modulith-docs`, linked into arc42.
- Event publication registry (JDBC) for reliable inter-module events without an external broker.

Milestone / RC pre-releases (e.g., Spring Modulith 2.1.0-RC1, which targets Spring Boot 4.1 M4) are explicitly **not** adopted, consistent with the stable-only policy from ADR-001.

## Consequences

- Positive: one deployment artifact, one observability surface, no distributed-tracing burden, simple local development; Spring Modulith gives us boundary verification, slice tests, and auto-generated documentation for free.
- Negative: independent scaling per bounded context is not possible without reshuffling — acceptable for R1-R3; revisit if a single context becomes a hotspot.
- Follow-ups: re-evaluate at R4 when multiplayer or asset delivery may demand isolation; pin `spring-modulith-bom:2.0.6` in `pom.xml`; add a verification test (`ApplicationModules.of(...).verify()`); wire `Documenter` into the build to refresh PlantUML snippets used by arc42.

## Rejected Options

- Option B - reinvents what Spring Modulith already provides; loses event publication registry and slice testing.
- Option C - operationally too heavy for a 1-2 person team; cross-cutting changes (e.g., audit log) would touch many repos.
- Option D - WebSocket node is already separable in Option A; no further split needed.

## Links

- ADR-005 (Bounded contexts)
- ADR-008 (docker-compose orchestration)
- arc42 §5 (Building Block View), §7 (Deployment View)
