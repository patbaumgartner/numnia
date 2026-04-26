---
description: Create or update an Architecture Decision Record (ADR) under docs/adr/ in MADR format.
mode: agent
---

# /adr - Architecture Decision Record

Invocation: `/adr "<short title of the decision>"`

## Task

1. Find the next free ADR number under `docs/adr/`.
2. Create `docs/adr/ADR-NNN-<slug>.md` using the following MADR-oriented template:

```markdown
# ADR-NNN - <Title>

| Field | Value |
| --- | --- |
| Status | Proposed / Accepted / Rejected / Superseded by ADR-... |
| Date | YYYY-MM-DD |
| References | UC-..., FR-..., NFR-..., arc42 §... |

## Context and Problem

...

## Decision Drivers

- ...

## Considered Options

- Option A: ...
- Option B: ...

## Decision

We choose **Option X** because ...

## Consequences

- Positive: ...
- Negative: ...
- Follow-ups: ...

## Rejected Options

- Option Y - reasoning
```

## Rules

- Documentation language: English.
- ADRs are immutable once "Accepted"; changes happen via a new ADR with "Superseded by".
