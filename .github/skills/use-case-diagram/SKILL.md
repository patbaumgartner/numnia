---
name: use-case-diagram
description: Update the Numnia PlantUML use-case diagram. Use this skill when asked to update docs/use_cases.puml, add or remove actors/use cases, or maintain the use-case overview diagram.
---

# use-case-diagram - Update Use-Case Diagram

## Task

1. Read [docs/Requirements.md](../../../docs/Requirements.md) and the current [docs/use_cases.puml](../../../docs/use_cases.puml).
2. Identify actors (Child, Parent, Teacher, School Admin, Support, Content Manager, System Admin, external systems such as the email service).
3. Identify all use cases per release (scope: SRS §12).
4. Assign stable IDs (`UC-XXX`).
5. Write a PlantUML diagram with `left to right direction` and appropriate `<<include>>`/`<<extend>>` relations.
6. Each UC traces to at least one FR ID (documented in the use-case spec, not in the diagram itself).

## Output

`docs/use_cases.puml` - one file per system boundary, separated by release marker if needed.

## Rules

- No implementation details in use-case names.
- Do not move stable IDs.
- Documentation language: English.
