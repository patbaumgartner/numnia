# UC-044 - Adaptive engine triggers pace downgrade

| Field | Value |
| --- | --- |
| Use-Case ID | UC-044 |
| Title | Adaptive engine triggers pace downgrade and mode change |
| Release | R4 |
| Primary actor | Adaptive Engine |
| Secondary actors | Child (passive recipient) |
| Status | Specified |
| Goal | On overload (errors, time-outs) the engine reduces the speed level by one step and proposes a fitting mode (accuracy or explanation). |
| Related requirements | FR-LEARN-006, FR-GAME-006, FR-LEARN-008 |

## Preconditions

1. Active session with running speed level.
2. Frustration-protection rules configured (SRS §6.1.2).

## Trigger

Three consecutive errors or time-outs in the same content domain.

## Main flow

1. The engine detects the trigger.
2. The system reduces the speed level by one (e.g., G3 -> G2).
3. The system proposes accuracy mode (G0) or explanation mode (UC-013).
4. The child confirms; the system applies the change.

## Alternative flows

- 3a The child rejects: the system stays at the new lower G level.

## Exception flows

- 2x Persistence error: change retried; previous level remains until success.

## Postconditions

- Success: pace and mode adjusted; history updated.
- Failure: previous level preserved.

## Business rules

- BR-001 The engine never raises the speed level automatically without success criteria.

## Acceptance criteria (BDD)

```gherkin
Feature: UC-044 Pace downgrade

  Scenario: Three errors in a row trigger the downgrade
    Given the child plays at G3
    When three consecutive errors occur
    Then the system sets G2
    And proposes accuracy or explanation mode
```
