---
description: Derive the entity model for Numnia and maintain it in docs/entity_model.md with a Mermaid ER diagram and attribute tables.
mode: agent
---

# /entity-model - Entity Model

## Task

1. Read [docs/Requirements.md](../../docs/Requirements.md) §5 (domain model) and §6 (FRs).
2. Read the existing [docs/entity_model.md](../../docs/entity_model.md), if present.
3. Identify all domain objects from §5 (UserAccount, ChildProfile, ParentProfile, TeacherProfile, SchoolClass, LearningProgress, MathTask, World, Portal, Creature, CreatureInstance, Match, MatchResult, Event, Season, Reward, ModerationCase, AuditLog) plus any others surfaced by the use cases.
4. Produce a **Mermaid `erDiagram`** without attributes (relationships with cardinalities).
5. Produce **one attribute table per entity** with columns: Attribute, Description, Data type, Length/precision, Validation (PK, sequence, NOT NULL, UNIQUE, FK, check).
6. Use PK sequences instead of auto-increment.
7. Trace each entity to the related FR IDs.

## Output

`docs/entity_model.md` containing:
- Introduction (source, status, scope: model release).
- Mermaid ER diagram.
- Attribute tables per entity.
- Mapping table entity → FR IDs.

## Rules

- Enforce data minimization (NFR-PRIV-001) strictly: no fields without UC justification.
- Pseudonymized child identification - no clear-name field on child entities.
- Documentation language: English; identifiers in English (snake_case in DB, camelCase in Java).
