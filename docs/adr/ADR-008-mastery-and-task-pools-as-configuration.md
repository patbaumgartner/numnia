# ADR-008 - Mastery Thresholds, G Levels and Task Pools Are Configuration Data

| Field | Value |
| --- | --- |
| Status | Accepted |
| Date | 2026-04-26 |
| Deciders | Numnia core team |
| References | FR-OPS-001, FR-OPS-002, FR-LEARN-001..012, AP-02, SRS §6.1 |

## Context and Problem

Mastery thresholds, the success corridor (70-90 %), pace levels G0-G5 with their time limits, and task pools per content domain must be tuned during the pilot phase and afterwards by content managers — not by engineers. Hard-coding any of these values would force a code deployment for every pedagogical adjustment and would block LiveOps.

## Decision Drivers

- LiveOps without code deployment (FR-OPS-001/002).
- Pilot-class fine-tuning of mastery thresholds (AP-02).
- Auditability and reversibility of pedagogical changes.
- Test First: configuration must be testable in isolation.

## Considered Options

- **Option A** - All pedagogical parameters and task pools are versioned configuration entities in PostgreSQL, edited via the admin UI and validated server-side.
- Option B - YAML files in the repository, deployed with the application.
- Option C - Hard-coded constants.

## Decision

We choose **Option A**:

- A `LearningConfiguration` entity holds mastery thresholds, success corridor bounds, and re-test intervals (3/5/7/10/14 days).
- A `PaceConfiguration` entity holds G0-G5 time limits per task type.
- A `TaskPool` entity holds task templates per content domain, with version, validity window, and audit log.
- Every change is versioned and audited (FR-SAFE-005); rollouts are explicit.

## Consequences

- Positive: pilot adjustments without deployment; reversible via version pin; testable via fixtures.
- Negative: requires admin UI surface and validation rules — covered by FR-OPS-001/002 and tested via BDD scenarios.
- Follow-ups: define a cache-invalidation strategy (Redis) for hot reads of `PaceConfiguration`.

## Rejected Options

- Option B - still requires a deployment, defeats LiveOps goal.
- Option C - violates FR-OPS-002 outright.

## Links

- ADR-004 (PostgreSQL as primary persistence)
- arc42 §8.3 (Learning pedagogy), §8.10 (Operations and LiveOps)
