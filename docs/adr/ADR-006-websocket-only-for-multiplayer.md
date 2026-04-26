# ADR-006 - WebSocket Only for Multiplayer and Live Events

| Field | Value |
| --- | --- |
| Status | Accepted |
| Date | 2026-04-26 |
| Deciders | Numnia core team |
| References | FR-MP-001..008, NFR-PERF-003, NFR-SEC-001..004, arc42 §3, §5 |

## Context and Problem

Numnia is turn-based by design (FR-MP-001). Multiplayer requires low-latency move exchange (p95 <= 500 ms, NFR-PERF-003), and a small set of live events (matchmaking notifications, turn timers, opponent left, LiveOps banners) need to be pushed to the client. We must avoid persistent stateful connections for everything else to keep server resources predictable for a small operations team.

## Decision Drivers

- Move processing latency p95 <= 500 ms (NFR-PERF-003).
- Operability and cost control (NFR-OPS-001..003).
- Clear separation between request/response (REST) and push (WebSocket).
- Security: TLS-only, server-side authorization on every frame (NFR-SEC-001..003).

## Considered Options

- **Option A** - WebSocket scoped exclusively to multiplayer and a small set of live events; everything else over REST/HTTP.
- Option B - WebSocket as the universal transport (also for non-real-time CRUD).
- Option C - Server-Sent Events (SSE) for push, REST for everything else.

## Decision

We choose **Option A**:

- WebSocket sessions are opened only when a child enters multiplayer or subscribes to a live event channel (LiveOps banner, turn timer).
- The WebSocket protocol carries authenticated, JSON-framed turn moves and a small set of server-pushed events.
- All other interactions (training, gamification, parent dashboard) use REST over HTTPS.

## Consequences

- Positive: bounded number of long-lived connections, simpler scaling, focused security review surface.
- Negative: dual-channel client code (REST + WS) — mitigated by a single typed client wrapper.
- Follow-ups: per-frame authorization and rate limiting on WS gateway; enforce WSS only.

## Rejected Options

- Option B - turns every request into a stateful concern, complicates load balancing, expands the attack surface.
- Option C - SSE lacks bidirectional turn submission semantics.

## Links

- ADR-005 (OpenAPI / GraphQL scope)
- arc42 §3.2 (Technical Context), §6.3 (Turn-Based Runtime View)
