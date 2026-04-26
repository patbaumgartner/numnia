---
description: Write or review use-case specifications for one or more UC IDs - one document each under docs/use_cases/.
mode: agent
---

# /use-case-spec - Use-Case Specification

Examples in chat:
- `/use-case-spec UC-005`
- `/use-case-spec UC-005 UC-006 UC-007`

## Task

For each given UC ID:

1. Read [docs/use_cases.puml](../../docs/use_cases.puml) and [docs/Requirements.md](../../docs/Requirements.md).
2. Read (if present) the existing `docs/use_cases/UC-XXX-*.md`.
3. Produce **one dedicated file** `docs/use_cases/UC-XXX-<short-slug>.md`.
4. Use **exactly** this template:

```markdown
# UC-XXX - <Title>

| Field | Value |
| --- | --- |
| Use-Case ID | UC-XXX |
| Title | <Title> |
| Release | R<n> |
| Primary actor | ... |
| Secondary actors | ... |
| Status | Specified |
| Goal | ... |
| Related requirements | FR-..., NFR-... |

## Preconditions
1. ...

## Trigger
...

## Main flow
1. ...

## Alternative flows
- 3a ...

## Exception flows
- 5x ...

## Postconditions
- Success: ...
- Failure: ...

## Business rules
- BR-001 ...

## Acceptance criteria (BDD)
\`\`\`gherkin
Feature: UC-XXX <Title>
  ...
\`\`\`
```

## Rules

- **Never** bundle multiple UCs into one file.
- Steps are free of implementation details (no class/method names).
- Acceptance criteria are executable Gherkin scenarios (Given/When/Then), the source for Cucumber in CI.
- Documentation language: English. Gherkin scenario text in English.
- Trace back: every UC references at least one FR ID.
