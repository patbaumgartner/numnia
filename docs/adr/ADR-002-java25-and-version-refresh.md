# ADR-002 - Java 25 LTS and Stack Version Refresh (April 2026)

| Field | Value |
| --- | --- |
| Status | Accepted |
| Date | 2026-04-26 |
| Deciders | Numnia core team |
| Supersedes | Parts of ADR-001 (JDK pin Java 21 → Java 25; framework/library version pins) |
| References | NFR-OPS-001..003, NFR-ENG-001..006, arc42 §2 |

## Context and Problem

ADR-001 was authored when Java 21 LTS was the current LTS line and most ecosystem libraries had not yet released their Spring 7 / Spring Boot 4-compatible majors. As of April 2026:

- Java 25 LTS is generally available (Premier support → Sep 2030, Extended → Sep 2033).
- Spring Boot 4.0.6 GA on Spring Framework 7.0.7 has been released and is the active development line.
- Cucumber-JVM `cucumber-spring 7.34.3` is the first release that supports Spring 7 / Spring Boot 4 (embeds Spring 7.0.3, JUnit 6.0.2, Mockito 5.21.0).
- Several dependencies have crossed major-version boundaries since the original pin (Babylon.js 7 → 9, Vite 6 → 8, TypeScript 5 → 6, Testcontainers 1 → 2).
- PostgreSQL 18.3 and Redis 8.6 (OSS) are current stable lines.

Continuing on the original pins would block use of Spring Boot 4, prevent the validated Cucumber-Spring pin from working, and accumulate upgrade debt.

## Decision

Adopt the following binding pins. **No deviation without a follow-up ADR.**

### Backend

| Component | Pin |
| --- | --- |
| Java | 25 LTS |
| Spring Boot | 4.0.6 (Spring Framework 7.0.7) |
| Spring Modulith | 2.0.6 (stable; supports Spring Boot 4.0.6) |
| PostgreSQL | 18.3 |
| Redis | 8.6 OSS |
| Object storage | per ADR-003 |
| OpenAPI | 3.1 |
| JUnit Jupiter | 6.0.x (stable) |
| AssertJ | 3.27.x (stable) |
| Mockito | 5.23.x |
| Testcontainers | 2.0.x |
| Flyway | 12.4.x |
| Cucumber-JVM (`cucumber-spring`) | ≥ 7.34.3 |

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

### Pre-release exclusions

JUnit Jupiter `6.1.0-RC1`, AssertJ `4.0.0-M1`, and Spring Modulith `2.1.0-RC1` (which targets Spring Boot 4.1 M4) are explicitly **not** adopted; we stay on the latest stable lines (6.0.x, 3.27.x, 2.0.x).

## Consequences

- Positive:
  - Unblocks Spring Boot 4 / Spring 7 + the validated `cucumber-spring 7.34.3` pin, removing a known integration risk.
  - Java 25 LTS extends the Premier support horizon by 2 years over Java 21 (Sep 2028 → Sep 2030).
  - All pins land on currently maintained stable lines; no orphaned dependencies.
- Negative:
  - Major-version migrations (Babylon 7→9, Vite 6→8, TypeScript 5→6, Testcontainers 1→2) require non-trivial code changes once the codebase is bootstrapped.
  - Cucumber-JVM is now strictly tied to ≥ 7.34.3; older snippets/tutorials will not work.
- Follow-ups:
  - Bump SRS §4.1 to v1.2 with this decision in the changelog (separate, explicitly authorised change).
  - Lock pins via `pom.xml`, `package.json`, and `compose.yaml` images on first scaffolding.

## Links

- ADR-001 (Stack Selection) — superseded in part
- ADR-003 (Object storage replacement)
- arc42 §2.1
