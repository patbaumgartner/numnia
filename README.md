# Numnia

Web-based 3D math learning game for primary-school children (ages 7-12). Hosted in Switzerland, privacy-friendly, pedagogically grounded.

> **Project documentation language: English.**
> In-product UI for kids and parents: Swiss High German (with umlauts, without sharp s) - this is a product rule (NFR-I18N).

## Documentation

- [docs/Requirements.md](docs/Requirements.md) - SRS v1.1 (approved)
- [docs/architecture/arc42.md](docs/architecture/arc42.md) - arc42 architecture documentation
- [docs/use_cases.puml](docs/use_cases.puml) - use-case diagram (PlantUML)
- [docs/use_cases/](docs/use_cases/) - use-case specifications (R1)

## Methodology

Numnia follows the **AI Unified Process** ([unifiedprocess.ai](https://unifiedprocess.ai/)) - requirements at the center, AI-assisted implementation, test-protected iteration.

Workflow:

```
requirements → entity-model → use-case-diagram → use-case-spec → implement → unit-test → e2e-test → review
```

## For AI Agents

- [AGENTS.md](AGENTS.md) - mandatory entry point for every agent (Copilot, Claude Code, Codex, …)
- [.github/agents/](.github/agents/) - custom agent profiles (architect, implementer, reviewer)
- [.github/skills/](.github/skills/) - AIUP agent skills (one SKILL.md per workflow step)
- [.vscode/mcp.json](.vscode/mcp.json) - MCP servers (context7 for library docs, Playwright for E2E)

## Stack

- Backend: Java 25 LTS, Spring Boot 4.0.6 (Spring 7.0.7), Spring Modulith 2.0.6, PostgreSQL 18.3, Redis 8.6 OSS, object storage per ADR-001, OpenAPI 3.1, Flyway 12.4.x
- Frontend: Node.js 24 LTS, pnpm 10.33.x, TypeScript 6.0.x, React 19.2.x, Babylon.js 9.4.x, Vite 8.0.x
- Tests: JUnit Jupiter 6.0.x, AssertJ 3.27.x, Mockito 5.23.x, Testcontainers 2.0.x, Cucumber-JVM 7.34.3+, Vitest 4.1.x, Playwright 1.59.x, @cucumber/cucumber 12.8.x
- Orchestration: Docker, docker-compose

## Run

Two supported modes — pick one. Both are wrapped by [Makefile](Makefile) targets.

### A. Full container stack

Builds and starts Postgres, Redis, MinIO, the Spring Boot backend and the Vite/React frontend (served by nginx with `/api` reverse-proxy to the backend).

```sh
cp .env.example .env          # optional — overrides defaults
make up                       # = docker compose --profile full up -d --build
```

- Frontend: <http://localhost:5173>
- Backend health: <http://localhost:8080/actuator/health>
- MinIO console: <http://localhost:9001> (user/pass from `.env`)

Stop: `make down` — wipe volumes too: `make clean`.

PostgreSQL 18 stores data below a major-version-specific directory. If an older
local dev volume was created with the pre-18 mount layout, recreate the local
volume with `make clean && make up`. Preserve important data with `pg_upgrade`
instead of wiping the volume.

### B. Local dev (hot reload)

Run only the infrastructure in containers and the apps on the host for fast feedback:

```sh
make infra            # postgres + redis + minio
make backend-dev      # ./mvnw spring-boot:run on :8080
make frontend-dev     # vite dev server on :5173 (proxies /api → :8080)
```

### Tests

```sh
make test-backend     # JUnit + Cucumber-JVM (NFR-ENG-006: ≥80% line / ≥70% branch)
make test-frontend    # Vitest (NFR-ENG-006: ≥70% line)
make test-e2e         # Playwright + Cucumber against a running stack
```

Prerequisites: Docker (with Compose v2), Java 25, Node.js 24 LTS, pnpm 10.33+.

## Use Cases (Release 1)

See [docs/use_cases/](docs/use_cases/) - one file per use case (UC-001 to UC-011).
