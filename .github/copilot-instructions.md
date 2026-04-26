# GitHub Copilot - Always-on Instructions for Numnia

This file is loaded into context for every Copilot request. It is the compact operative version of [AGENTS.md](../AGENTS.md).

## Read context first

1. [docs/Requirements.md](../docs/Requirements.md) - SRS v1.1 (truth about requirements)
2. [docs/architecture/arc42.md](../docs/architecture/arc42.md) - architecture (truth about structure)
3. [docs/use_cases/](../docs/use_cases/) - **one file per use case**, truth about implementation and tests
4. [docs/use_cases.puml](../docs/use_cases.puml) - use-case diagram

## Languages

- **Project documentation, code, identifiers, comments, ADRs, commit messages: English.**
- **In-product UI text, audio scripts, child- and parent-facing strings, audit messages shown to users: Swiss High German with umlauts (ä, ö, ü), without sharp s** (NFR-I18N-002/004). This is a product rule.

## Engineering discipline (binding)

- **Test First, TDD** for business logic (NFR-ENG-002).
- **BDD/Gherkin** as executable acceptance criteria, in CI via Cucumber (NFR-ENG-003/004).
- **Coverage**: Backend ≥ 80% line / 70% branch, Frontend ≥ 70% line (NFR-ENG-006).
- Clean Code, SOLID, small increments (NFR-ENG-001).
- Conflict between deadline and quality gate: **postpone the release** rather than compromise quality.

## Stack (no deviation without ADR)

- Backend: Java 25 LTS, Spring Boot 4.0.6 (Spring Framework 7.0.7), Spring Modulith 2.0.6, Maven Wrapper, PostgreSQL 18.3, Redis 8.6 OSS, object storage per ADR-003 (MinIO repo archived; interim pin to last OSS release, target Garage/SeaweedFS), OpenAPI 3.1, JUnit Jupiter 6.0.x, AssertJ 3.27.x, Mockito 5.23.x, Testcontainers 2.0.x, Flyway 12.4.x, Cucumber-JVM ≥ 7.34.3 (cucumber-spring 7.34.3 is the first version that supports Spring 7 / Spring Boot 4 — pin this or newer).
- Frontend: Node.js 24 LTS, pnpm 10.33.x, TypeScript 6.0.x, React 19.2.x, Babylon.js 9.4.x, Vite 8.0.x, Vitest 4.1.x, React Testing Library, Playwright 1.59.x, @cucumber/cucumber 12.8.x.
- Orchestration: Docker + docker-compose. Hosting: Switzerland only.

## Security & privacy (non-negotiable)

- TLS-only, server-side validation and authorization, rate limiting (NFR-SEC-001..004).
- Double opt-in before sensitive child functions (FR-SAFE-006).
- Pseudonymized child identification, data minimization (NFR-PRIV-001).
- No external trackers, no CDN outside CH, no data outside CH (NFR-OPS-003).
- Children must **not** be able to send free text messages (FR-SAFE-001/002).

## Pedagogy (non-negotiable)

- Mastery thresholds, S levels (S1-S6) and G levels (G0-G5) are configuration only (FR-OPS-002).
- Frustration protection: 3 errors in a row → speed downgrade + mode suggestion (SRS §6.1.2).
- Success corridor 70-90%; errors **never** cost star points or items (FR-GAM-005).
- Mastery only after consolidation across ≥ 2 sessions on ≥ 2 calendar days (FR-LEARN-012).

## Traceability (mandatory)

- Every commit/PR references at least one `UC-XXX` and the related `FR-/NFR-` IDs.
- For implementation: read the use-case spec first, derive BDD scenarios as executable tests, then write code.
- Gaps in a UC: do not guess - extend the use-case spec (`/use-case-spec`) or propose an ADR draft.

## Forbidden actions

- Edit `docs/Requirements.md` without explicit order and version bump.
- Introduce external libraries/frameworks without an ADR.
- Skip server-side FR validation by relying on client validation only.
- Use English UI strings for child/parent-facing UI, or use sharp s in any UI text.
- Throwaway code without tests; "test will be added later" comments.

## Slash commands (prompts)

Available under `.github/prompts/`:

- `/requirements` - consolidate the requirements catalog.
- `/entity-model` - derive/update the entity model.
- `/use-case-diagram` - update the PlantUML diagram.
- `/use-case-spec` - write/review a use-case spec (argument: UC-ID).
- `/implement` - end-to-end implementation of a UC (argument: UC-ID).
- `/unit-test` - generate JUnit/Vitest tests for a UC.
- `/e2e-test` - generate Playwright + Cucumber scenarios for a UC.
- `/review` - craftsmanship and quality-gate review.
