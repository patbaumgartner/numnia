# ADR-001 - Stack Selection

| Field | Value |
| --- | --- |
| Status | Accepted (architecture choice). Version pins superseded by **ADR-002** (April 2026); object storage selection superseded by **ADR-003**. |
| Date | 2026-04-26 |
| Deciders | Numnia core team |
| References | NFR-OPS-001..003, NFR-SEC-001..004, NFR-ENG-001..006, arc42 §2, §4, ADR-002, ADR-003 |

## Context and Problem

Numnia must run web-based, with 3D content for primary-school children, hosted exclusively in Switzerland, operated by a 1-2 person part-time team. The stack must enable Test First / TDD / BDD discipline, OpenAPI 3.1 contracts, and turn-based multiplayer over WebSocket. Long-term maintainability over multiple years (children grow with the product) is a primary driver.

## Decision Drivers

- Long-term maintainability and LTS support
- Strong typing, mature testing ecosystems
- Native fit for 3D in the browser without proprietary plug-ins
- Fully runnable via Docker / docker-compose for a single-node operator
- Hosting in Switzerland; no hyperscaler / no external CDN
- Small operations team; favour batteries-included frameworks

## Considered Options

- **Option A**: Java 21 LTS + Spring Boot 4.0 LTS / TypeScript 5 + React 19 + Babylon.js 7 + Vite 6 / PostgreSQL 16, Redis 7, MinIO
- Option B: Node.js (Nest.js) end-to-end / Three.js
- Option C: .NET 8 + Blazor + Babylon.js bindings
- Option D: Python (FastAPI) + React + Three.js

## Decision

We choose **Option A**.

## Consequences

- Positive:
  - Long LTS horizons on JVM and Spring Boot 4.0 reduce maintenance toil.
  - Mature test ecosystem (JUnit 5, AssertJ, Mockito, Testcontainers) directly supports the Test First mandate (NFR-ENG-002).
  - Babylon.js 7 has first-class TypeScript types and a stable WebGPU/WebGL2 backend suitable for primary-school hardware.
  - PostgreSQL 16, Redis 7, MinIO are commodity, all available as compose services and self-hostable in CH.
- Negative:
  - Two languages (Java + TypeScript) increases context-switching cost for solo maintenance.
  - React 19 + Babylon.js 7 is a relatively new combination; we accept some early-adopter risk.
- Follow-ups:
  - Pin LTS versions in `pom.xml` and `package.json`.
  - Capture upgrade cadence in a separate ADR if needed.

## Rejected Options

- Option B - single language is appealing, but JVM persistence/transactional tooling and observability are a better long-term fit for a 5+ year product.
- Option C - smaller community for 3D web; more friction for child-friendly tooling.
- Option D - dynamic typing increases test burden disproportionately for a 1-2 person team.

## Links

- arc42 §2 (Architecture Constraints), §4 (Solution Strategy)
- SRS §4.1
