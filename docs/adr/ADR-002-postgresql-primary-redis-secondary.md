# ADR-002 - PostgreSQL as Primary Persistence, Redis Only for Session and Matchmaking

| Field | Value |
| --- | --- |
| Status | Accepted |
| Date | 2026-04-26 |
| Deciders | Numnia core team |
| References | SRS §4.1, NFR-PRIV-001, NFR-OPS-003, arc42 §2, §5, ADR-001 (stack pins) |

## Context and Problem

Numnia stores long-lived learning histories, mastery state, audit logs, parent/teacher data, and gamification inventory. It also needs a fast, ephemeral store for sessions, matchmaking queues, and turn timers. We must decide which workloads belong in a relational store versus an in-memory store, and avoid splitting the source of truth across two systems.

## Decision Drivers

- Strict data minimization and auditability for child data (NFR-PRIV-001).
- Switzerland-only, self-hosted single-node operability (NFR-OPS-003).
- Strong relational integrity for learning history, gamification ledger, and audit log.
- Low-latency ephemeral state for matchmaking and WebSocket sessions (NFR-PERF-003).

## Considered Options

- **Option A** - PostgreSQL as the single source of truth; Redis only for ephemeral session, matchmaking, and pub/sub state.
- Option B - Redis as primary store with periodic snapshot to PostgreSQL.
- Option C - Document store (MongoDB) as primary plus Redis cache.

## Decision

We choose **Option A**. All durable domain data lives in PostgreSQL. Redis holds only:

- HTTP/WebSocket session tokens with TTL.
- Matchmaking queues and per-match turn timers.
- Pub/sub channels for WebSocket fan-out across nodes.

Redis state is reconstructible; loss of a Redis node is a degraded but recoverable condition.

## Consequences

- Positive: single source of truth, transactional integrity for learning/gamification, simple backup/restore (NFR-OPS-002), clear audit boundary.
- Negative: relational schema evolves with the domain — Flyway migrations required for every change (mitigated via CI gate).
- Follow-ups: Redis must never be the only place sensitive data lives; enforced via code review and a startup self-check.

## Rejected Options

- Option B - operationally fragile, snapshot lag risks data loss for child progress.
- Option C - no transactional joins for the gamification ledger, increased schema validation burden.

## Links

- ADR-001 (Stack Selection)
- arc42 §5 (Building Block View), §7 (Deployment View)
