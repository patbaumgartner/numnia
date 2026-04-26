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
/requirements → /entity-model → /use-case-diagram → /use-case-spec → /implement → /unit-test → /e2e-test → /review
```

## For AI Agents

- [AGENTS.md](AGENTS.md) - mandatory entry point
- [.github/copilot-instructions.md](.github/copilot-instructions.md) - always-on instructions for GitHub Copilot
- [.github/instructions/](.github/instructions/) - scope-specific instructions (documentation language, UI language, TDD/BDD, security, pedagogy)
- [.github/prompts/](.github/prompts/) - slash commands for the AIUP phases
- [.vscode/mcp.json](.vscode/mcp.json) - MCP servers (context7 for library docs, Playwright for E2E)

## Stack

- Backend: Java 25 LTS, Spring Boot 4.0.6 (Spring 7.0.7), Spring Modulith 2.0.6, PostgreSQL 18.3, Redis 8.6 OSS, object storage per ADR-003, OpenAPI 3.1, Flyway 12.4.x
- Frontend: Node.js 24 LTS, pnpm 10.33.x, TypeScript 6.0.x, React 19.2.x, Babylon.js 9.4.x, Vite 8.0.x
- Tests: JUnit Jupiter 6.0.x, AssertJ 3.27.x, Mockito 5.23.x, Testcontainers 2.0.x, Cucumber-JVM 7.34.3+, Vitest 4.1.x, Playwright 1.59.x, @cucumber/cucumber 12.8.x
- Orchestration: Docker, docker-compose

## Use Cases (Release 1)

See [docs/use_cases/](docs/use_cases/) - one file per use case (UC-001 to UC-011).
