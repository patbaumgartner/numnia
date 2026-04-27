# Numnia — convenience targets for local development.
# Authoritative orchestration: docker compose (ADR-008).

SHELL := /usr/bin/env bash
.DEFAULT_GOAL := help

# ─────────────────────────────────────────────────────────────────
.PHONY: help
help: ## Show this help
	@awk 'BEGIN{FS=":.*##"; printf "\nUsage: make \033[36m<target>\033[0m\n\n"} \
	     /^[a-zA-Z_-]+:.*##/ {printf "  \033[36m%-18s\033[0m %s\n", $$1, $$2}' $(MAKEFILE_LIST)

# ─── Local (host) dev ────────────────────────────────────────────
.PHONY: infra
infra: ## Start only infra (postgres, redis, minio)
	docker compose --profile infra up -d

.PHONY: infra-down
infra-down: ## Stop infra
	docker compose --profile infra down

.PHONY: backend-dev
backend-dev: ## Run backend on host (Spring Boot DevTools)
	cd backend && ./mvnw spring-boot:run

.PHONY: frontend-dev
frontend-dev: ## Run frontend on host (Vite dev server)
	cd frontend && pnpm install && pnpm dev

# ─── Full container stack ────────────────────────────────────────
.PHONY: build
build: ## Build all container images
	docker compose --profile full build

.PHONY: up
up: ## Start the full stack (build + up -d)
	docker compose --profile full up -d --build

.PHONY: down
down: ## Stop and remove the full stack
	docker compose --profile full down

.PHONY: logs
logs: ## Tail logs of all services
	docker compose logs -f --tail=200

.PHONY: ps
ps: ## Show service status
	docker compose ps

.PHONY: clean
clean: ## Stop stack and remove volumes (DESTRUCTIVE)
	docker compose --profile full down -v

# ─── Tests ───────────────────────────────────────────────────────
.PHONY: test-backend
test-backend: ## Run backend unit + integration tests
	cd backend && ./mvnw -B verify

.PHONY: test-frontend
test-frontend: ## Run frontend unit tests
	cd frontend && pnpm install && pnpm test

.PHONY: test-e2e
test-e2e: ## Run Playwright + Cucumber E2E suite
	cd e2e && pnpm install && pnpm test

.PHONY: test
test: test-backend test-frontend ## Run backend + frontend test suites
