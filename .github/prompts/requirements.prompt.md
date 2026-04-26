---
description: Consolidate the requirements catalog - reads docs/Requirements.md and ensures stable FR-/NFR-IDs, completeness and traceability.
mode: agent
---

# /requirements - Consolidate Requirements Catalog

The source of truth is [docs/Requirements.md](../../docs/Requirements.md). This file is approved (v1.1) - **never modify it without an explicit order and a version bump**.

## Task

1. Read [docs/Requirements.md](../../docs/Requirements.md) end to end.
2. Verify that all FR-/NFR- IDs are unique, sequential and stable.
3. Verify that every requirement has an acceptance/verification statement.
4. Verify that every open decision in section 14 is tracked as a decision or a work package.
5. On inconsistencies, produce a **change proposal** as a patch (`docs/Requirements.patch.md`) **without** modifying the original.

## Output

- Change proposal as a separate patch or pull request with a version bump in the change log.
- List of detected inconsistencies with requirement IDs.

## Rules

- Documentation language: English.
- No additional requirements without stakeholder confirmation.
- For new requirements: propose a backlog entry, not an SRS edit.
