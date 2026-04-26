# AGENTS.md - Numnia

This document is the mandatory entry-point for every AI agent (GitHub Copilot, Claude Code, Codex, etc.) contributing to Numnia. It follows the **AI Unified Process (AIUP)** methodology (see [unifiedprocess.ai](https://unifiedprocess.ai/)) and adapts it to our stack.

> **Project documentation language: English.**
> **In-product UI language for kids and parents: Swiss High German**, with umlauts (ä, ö, ü), **without sharp s** (NFR-I18N-002, NFR-I18N-004). This is a product rule, not a documentation rule.

---

## 1. Project Context

Numnia is a web-based 3D math learning game for primary-school children (ages 7-12).

Authoritative sources, to be consulted in this order:

1. [docs/Requirements.md](docs/Requirements.md) - Software Requirements Specification (SRS) v1.1, approved.
2. [docs/architecture/arc42.md](docs/architecture/arc42.md) - arc42 architecture documentation.
3. [docs/use_cases.puml](docs/use_cases.puml) - Use-case diagram (PlantUML).
4. [docs/use_cases/](docs/use_cases/) - Detailed use-case specifications (UC-XXX-*.md), the source of truth for implementation and tests.
5. ADRs under `docs/adr/` (when present).

**Never write code without a use-case reference.** Every pull request must reference at least one `UC-XXX` and the related `FR-/NFR-` IDs.

---

## 2. AIUP Workflow for Numnia

```
Inception            Elaboration                                          Construction                             Transition
────────────────     ──────────────────────────────────────────────────   ──────────────────────────────────────   ────────────
requirements  →  entity-model  →  use-case-diagram  →  use-case-spec  →  implement  →  unit-test  →  e2e-test  →  review
```

| Skill | Purpose | Input | Output |
| --- | --- | --- | --- |
| `requirements` | Maintain requirements catalog | `docs/Requirements.md` (existing) | Updated FR/NFR IDs |
| `entity-model` | Derive/update entity model | `docs/Requirements.md`, existing entities | `docs/entity_model.md` |
| `use-case-diagram` | Update PlantUML | `docs/Requirements.md` | `docs/use_cases.puml` |
| `use-case-spec UC-XXX` | Write/review use-case specification | `UC-ID(s)` | `docs/use_cases/UC-XXX-*.md` |
| `implement UC-XXX` | Implement on Spring Boot 4.0 + React/Babylon | `UC-ID` | Backend and frontend code with tests |
| `unit-test UC-XXX` | JUnit 5 + Mockito + AssertJ tests | `UC-ID` | `*Test.java`, `*.test.ts` |
| `e2e-test UC-XXX` | Playwright + Cucumber scenarios | `UC-ID` | `*.feature`, step defs, Playwright specs |
| `review` | Craftsmanship/quality-gate review | Changed files | Review report |
| `adr` | Create/update Architecture Decision Record | Decision title | `docs/adr/ADR-NNN-*.md` |

These are agent skills stored under `.github/skills/` and loaded automatically by the agent when relevant to the task.

---

## 3. Mandatory Engineering Principles

From [docs/Requirements.md](docs/Requirements.md) §11.4 and §7.6:

- **Test First** is mandatory (NFR-ENG-002). Tests/scenarios are written before any business-logic change.
- **TDD** for business logic: Red → Green → Refactor (backend and frontend state).
- **BDD** is mandatory for acceptance criteria (NFR-ENG-003): Gherkin scenarios run in CI via Cucumber.
- **CI quality gates** block merges (NFR-ENG-004).
- **Coverage**: Backend ≥ 80% line / 70% branch, Frontend ≥ 70% line (NFR-ENG-006).
- **Clean Code, SOLID, small increments** (NFR-ENG-001).
- **Definition of Done**: code, unit tests, integration tests, BDD scenarios, review, doc update.
- **If a deadline conflicts with a quality gate: postpone the release rather than compromise quality.**

---

## 4. Stack Specifics (binding, SRS §4.1)

### Backend

- **Java 25 LTS**, **Spring Boot 4.0.6** (Spring Framework 7.0.7 transitively), **Spring Modulith 2.0.6** (boundary verification, slice tests, event publication registry), Maven Wrapper
- **PostgreSQL 18.3** (primary), **Redis 8.6** OSS (sessions/matchmaking), object storage: **see ADR-001** (MinIO repo archived Apr 2026; interim pin to last OSS release `RELEASE.2025-10-15T17-29-55Z`, target migration to **Garage** or **SeaweedFS**)
- API: REST + WebSocket, **OpenAPI 3.1** as the contract
- Tests: **JUnit Jupiter 6.0.x** (stable), **AssertJ 3.27.x** (stable), **Mockito 5.23.x**, **Testcontainers 2.0.x** for integration tests, **Cucumber-JVM ≥ 7.34.3** for BDD (cucumber-spring 7.34.3 is the first release that supports Spring 7 / Spring Boot 4 — do not pin below; embeds Spring 7.0.3, JUnit 6.0.2, Mockito 5.21.0)
- Database migrations: **Flyway 12.4.x**

### Frontend

- **Node.js 24.x LTS** (Krypton), **pnpm 10.33.x**
- **TypeScript 6.0.x**, **React 19.2.x**, **Babylon.js 9.4.x**, **Vite 8.0.x**
- Tests: **Vitest 4.1.x** + React Testing Library, **Playwright 1.59.x** + **@cucumber/cucumber 12.8.x** for E2E/BDD

### Build & Run

- Fully orchestrated via Docker and docker-compose (SRS §4.2)
- Hosting only in Switzerland; no hyperscaler, no external CDN (NFR-OPS-003)

### Security & Privacy

- TLS-only, server-side validation and authorization, rate limiting (NFR-SEC-001..004)
- Double opt-in before sensitive functions (FR-SAFE-006)
- Data minimization, pseudonymized child identification (NFR-PRIV-001, SRS §10)

---

## 5. Project Structure (target)

```
numnia/
├── AGENTS.md                              ← you are here
├── README.md
├── docs/
│   ├── Requirements.md                    ← SRS (approved, do not edit without explicit order)
│   ├── architecture/arc42.md              ← architecture documentation
│   ├── use_cases.puml                     ← use-case diagram
│   ├── use_cases/                         ← UC-XXX-*.md (one file per use case)
│   ├── entity_model.md                    ← (produced via entity-model skill)
│   └── adr/                               ← Architecture Decision Records
├── backend/                               ← Spring Boot 4.0, Java 25
│   ├── pom.xml
│   └── src/{main,test}/java/...
├── frontend/                              ← React 19 + Babylon.js 9 + Vite 8
│   ├── package.json
│   └── src/...
├── e2e/                                   ← Playwright + Cucumber features
│   └── features/UC-XXX-*.feature
├── compose.yaml                           ← docker-compose
└── .github/
    ├── agents/                            ← custom agent profiles (architect, implementer, reviewer)
    ├── skills/                            ← AIUP agent skills (SKILL.md per workflow step)
    │   ├── requirements/
    │   ├── entity-model/
    │   ├── use-case-diagram/
    │   ├── use-case-spec/
    │   ├── implement/
    │   ├── unit-test/
    │   ├── e2e-test/
    │   ├── review/
    │   └── adr/
    └── workflows/                         ← CI (quality gates)
```

---

## 6. Working Rules for AI Agents

1. **Read SRS, arc42 and the matching UC first**, before touching code.
2. **Write tests first** (JUnit/Vitest and Cucumber feature) based on the UC's BDD scenarios.
3. **Respect the stack**: no alternative libraries without an ADR.
4. **Languages**:
   - Project documentation, code identifiers, comments, commit messages, ADRs: **English**.
   - In-product UI text, audio scripts, child-/parent-facing copy: **Swiss High German with umlauts, no sharp s** (NFR-I18N).
5. **Security first**: no endpoint without authn/authz; no free-text input field for children (FR-SAFE-001..002).
6. **Privacy**: no personal data in logs, no external trackers, no data outside CH.
7. **Configurability**: mastery thresholds, S/G levels, task pools are configuration, not code (FR-OPS-002, ADR-006).
8. **Traceability**: pull-request description includes the referenced `UC-XXX` and `FR-/NFR-` IDs and the affected BDD scenarios.
9. **When in doubt, do not guess** - the SRS/UCs are the truth; if a statement is missing, use the `use-case-spec` or `adr` skill to extend the spec or propose an ADR draft.
10. **Do not modify `docs/Requirements.md`**, except on explicit instruction with a version bump in the change log.

---

## 7. Definition of Ready (for a story)

- Linked to at least one `UC-XXX` and the related `FR-/NFR-` IDs.
- BDD scenarios (Given/When/Then) present in the use case.
- Stack and security implications reviewed.
- Pedagogical implications reviewed (relevant for learning/game stories).

## 8. Definition of Done (for a PR)

- All BDD scenarios for the affected UC pass.
- Unit and integration tests green; coverage above thresholds.
- Security/privacy checklist completed (validation, authz, audit log, data minimization).
- Documentation update (UC, arc42, ADR) performed.
- Review by a second person or via the `review` skill.

---

## 9. Further Reading

- AIUP methodology: <https://unifiedprocess.ai/>
- arc42: <https://arc42.org/>
- Up-to-date library documentation: prefer the `context7` MCP server (`/.vscode/mcp.json`).
