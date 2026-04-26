# UC-004 - Child practices in accuracy mode (without time pressure)

| Field | Value |
| --- | --- |
| Use-Case ID | UC-004 |
| Title | Child practices in accuracy mode without time pressure |
| Release | R1 |
| Primary actor | Child (7-12) |
| Secondary actors | Adaptive engine, mastery tracker |
| Status | Specified |
| Goal | The child practices an operation without time pressure (G0), to build confidence or to reduce frustration. |
| Related requirements | FR-GAME-001, FR-GAME-002, FR-LEARN-004, FR-LEARN-006, FR-LEARN-008, FR-GAM-005, NFR-A11Y-001, NFR-A11Y-003, NFR-I18N-002 |

## Preconditions

1. Active child session (UC-002).
2. At least one operation/world unlocked.

## Trigger

The child selects "Accuracy mode" in the main menu or as a suggestion from the adaptive engine.

## Main flow

1. The system shows the operation and world selection; speed is fixed to G0 (no time limit).
2. The child starts the practice.
3. The system creates a task; the UI explicitly highlights: "You have as much time as you need".
4. The child answers the task; no time-out tracking.
5. The system evaluates the answer, gives friendly feedback and shows a "Show explanation" button when appropriate (FR-LEARN-008).
6. Steps 3-5 repeat until the child ends.
7. The system updates the learning history (no G-related mastery update, since G0).
8. The system shows the session summary; star points are credited without speed bonus.

## Alternative flows

- 5a Child invokes "Show explanation": the system plays animated solution steps.
- The adaptive engine continually detects high accuracy: at the end the system proposes a switch to training mode at higher speed.

## Exception flows

- 3x Asset loading error: 2D fallback as in UC-003.
- 4x Connection drop: local buffering; on permanent failure clean termination without double counting.

## Postconditions

- Success: accuracy per domain is logged in the history; mastery conditions are influenced only via accuracy (not speed), provided the level allows it.
- Failure: consistent termination without faulty mastery counting.

## Business rules

- BR-001 G0 allows no time limit; the UI must not show a timer.
- BR-002 Errors in accuracy mode cost no star points or items.
- BR-003 Mastery still requires consolidation (see UC-003 BR-004).

## Acceptance criteria (BDD)

```gherkin
Feature: UC-004 Child practices in accuracy mode

  Background:
    Given an active child session

  Scenario: Accuracy mode runs without a timer
    Given the child starts accuracy mode for subtraction
    When a task is shown
    Then no time limit is active
    And no timer is visible in the UI

  Scenario: Explanation mode is reachable from accuracy mode
    Given a task is shown in accuracy mode
    When the child selects "Show explanation"
    Then animated solution steps are played
    And the task remains workable

  Scenario: No star point loss on error
    Given a child with 8 star points
    When it answers a task wrong in accuracy mode
    Then the star points balance stays at 8
```
