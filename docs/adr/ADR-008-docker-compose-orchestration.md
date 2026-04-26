# ADR-008 - docker-compose as the Standard Orchestration

| Field | Value |
| --- | --- |
| Status | Accepted |
| Date | 2026-04-26 |
| Deciders | Numnia core team |
| References | SRS §4.1, §4.2, NFR-OPS-001..003, arc42 §7 |

## Context and Problem

Numnia is operated by a 1-2 person part-time team in a single Swiss data center, without 24/7 staffing. We need an orchestration layer that runs the full stack (backend, WebSocket node, PostgreSQL, Redis, object store, monitoring) on one or a small number of nodes, with a low cognitive load and reproducible local development.

## Decision Drivers

- Operability with a small team (NFR-OPS-001).
- Switzerland-only single-node hosting (NFR-OPS-003).
- Reproducibility: dev, CI, and production share the same composition.
- No vendor lock-in.

## Considered Options

- **Option A** - docker-compose for dev, CI, and production single-node deployments.
- Option B - Kubernetes (k3s / vanilla).
- Option C - Nomad.

## Decision

We choose **Option A**. The canonical stack lives in `compose.yaml` at the repository root. Production deployment is `docker compose up -d` on the Swiss host, behind a reverse proxy that terminates TLS. Backups, restore tests, and updates are scripted around the same compose file.

## Consequences

- Positive: minimal operational surface, identical topology across environments, fast onboarding for contributors.
- Negative: no native rolling deploys or self-healing — accepted for a small product; a brief planned downtime window is acceptable per operations policy.
- Follow-ups: revisit if multi-node scaling becomes necessary (would be its own ADR).

## Rejected Options

- Option B - operational complexity unjustified for the team size.
- Option C - smaller ecosystem and contributor familiarity than compose.

## Links

- ADR-009 (Modulith over microservices)
- arc42 §7 (Deployment View)
