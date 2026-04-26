# ADR-001 - Stack Selection

| Field | Value |
| --- | --- |
| Status | Accepted |
| Date | 2026-04-27 |
| Deciders | Numnia core team |
| References | NFR-OPS-001..003, NFR-SEC-001..004, NFR-ENG-001..006, SRS §4.1, arc42 §2, §4 |

## Context and Problem

Numnia must run web-based, with 3D content for primary-school children, hosted exclusively in Switzerland, operated by a 1-2 person part-time team. The stack must enable Test First / TDD / BDD discipline, OpenAPI 3.1 contracts, and turn-based multiplayer over WebSocket. Long-term maintainability over multiple years (children grow with the product) is a primary driver, and concrete library/runtime pins must be tracked in one place to avoid drift.

## Decision Drivers

- Long-term maintainability and LTS support.
- Strong typing, mature testing ecosystems.
- Native fit for 3D in the browser without proprietary plug-ins.
- Fully runnable via Docker / docker-compose for a single-node operator.
- Hosting in Switzerland; no hyperscaler / no external CDN.
- Small operations team; favour batteries-included frameworks.
- OSS-first licensing (AGPLv3 / Apache 2.0 / MPL); no source-restricted products.

## Considered Options

- **Option A** - Java 25 LTS + Spring Boot 4 LTS / TypeScript 6 + React 19 + Babylon.js 9 + Vite 8 / PostgreSQL 18, Redis 8 OSS, S3-compatible object storage.
- Option B - Node.js (Nest.js) end-to-end / Three.js.
- Option C - .NET 8 + Blazor + Babylon.js bindings.
- Option D - Python (FastAPI) + React + Three.js.

## Decision

We choose **Option A**. The binding pins are listed below and are the **single source of truth**; any deviation requires a follow-up ADR.

### Backend

| Component | Pin |
| --- | --- |
| Java | 25 LTS (Premier support → Sep 2030) |
| Spring Boot | 4.0.6 (Spring Framework 7.0.7) |
| Spring Modulith | 2.0.6 (stable) |
| PostgreSQL | 18.3 |
| Redis | 8.6 OSS |
| Object storage | See "Object storage" below |
| OpenAPI | 3.1 |
| JUnit Jupiter | 6.0.x (stable) |
| AssertJ | 3.27.x (stable) |
| Mockito | 5.23.x |
| Testcontainers | 2.0.x |
| Flyway | 12.4.x |
| Cucumber-JVM (`cucumber-spring`) | ≥ 7.34.3 (first release supporting Spring 7 / Spring Boot 4; embeds Spring 7.0.3, JUnit 6.0.2, Mockito 5.21.0) |

### Frontend

| Component | Pin |
| --- | --- |
| Node.js | 24.x LTS (Krypton) |
| pnpm | 10.33.x |
| TypeScript | 6.0.x |
| React | 19.2.x |
| Babylon.js | 9.4.x |
| Vite | 8.0.x |
| Vitest | 4.1.x |
| Playwright | 1.59.x |
| @cucumber/cucumber | 12.8.x |

### Object storage

MinIO Inc. archived the public `minio/minio` GitHub repository in **April 2026** and migrated the project to the paid, source-restricted **AiStor** product. The last freely available OSS release is `RELEASE.2025-10-15T17-29-55Z`; no further security patches will be published under AGPLv3.

We therefore adopt a **two-step strategy**:

1. **Interim** - pin MinIO to `RELEASE.2025-10-15T17-29-55Z` for initial scaffolding and pilot environments. The pin is recorded in `compose.yaml`. Network isolation (object store reachable only from the backend; signed URLs to the browser) mitigates the lack of CVE patches.
2. **Target** - migrate to **Garage** (Rust, AGPLv3, geo-distributed self-hosting) or **SeaweedFS** (Apache 2.0, broader feature set) **before the first public release**. A short evaluation spike (1-2 days) selects between the two; the outcome is recorded as an amendment to this ADR.

Licensing AiStor is **rejected** on cost and OSS-first grounds. Both Garage and SeaweedFS expose an S3-compatible API, so service code does not need a second major rewrite.

### Pre-release exclusions

JUnit Jupiter `6.1.0-RC1`, AssertJ `4.0.0-M1`, and Spring Modulith `2.1.0-RC1` (which targets Spring Boot 4.1 M4) are explicitly **not** adopted; we stay on the latest stable lines (6.0.x, 3.27.x, 2.0.x).

## Consequences

- Positive:
  - Long LTS horizon (Java 25 → Sep 2030 Premier) reduces maintenance toil.
  - Mature test ecosystem (JUnit 6, AssertJ, Mockito, Testcontainers) directly supports the Test First mandate (NFR-ENG-002).
  - Babylon.js 9 has first-class TypeScript types and a stable WebGPU/WebGL2 backend suitable for primary-school hardware.
  - PostgreSQL 18.3, Redis 8.6 OSS, and the chosen object store are commodity services available as compose images and self-hostable in CH.
  - All pins land on currently maintained stable lines; no orphaned dependencies. Cucumber-Spring 7.34.3 unblocks Spring Boot 4 BDD.
- Negative:
  - Two languages (Java + TypeScript) increases context-switching cost for solo maintenance.
  - The interim MinIO pin will not receive further CVE patches; mitigated by isolation and a hard migration deadline (first public release).
  - A second migration step (MinIO → Garage/SeaweedFS) is required before launch.
- Follow-ups:
  - Lock pins in `pom.xml`, `package.json`, and `compose.yaml`.
  - Run the Garage vs SeaweedFS evaluation spike; amend this ADR with the outcome.
  - Update arc42 §3 deployment view once the object-storage target is selected.

## Rejected Options

- Option B - single language is appealing, but JVM persistence/transactional tooling and observability are a better long-term fit for a 5+ year product.
- Option C - smaller community for 3D web; more friction for child-friendly tooling.
- Option D - dynamic typing increases test burden disproportionately for a 1-2 person team.
- AiStor (MinIO commercial) - paid, source-restricted; conflicts with the OSS-first / Switzerland-only operating posture.

## Links

- arc42 §2 (Architecture Constraints), §2.1 (Stack Versions), §4 (Solution Strategy)
- SRS §4.1
- Garage: <https://garagehq.deuxfleurs.fr/>
- SeaweedFS: <https://github.com/seaweedfs/seaweedfs>
