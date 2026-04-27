# UC-014 - Child plays sprint mode

| Field | Value |
| --- | --- |
| Use-Case ID | UC-014 |
| Title | Child plays sprint mode (60-90 s short session) |
| Release | R1 |
| Primary actor | Child (7-12) |
| Secondary actors | Game & Worlds Service |
| Status | Specified |
| Goal | The child plays a focused, short session of approximately 60-90 seconds and earns star points. |
| Related requirements | FR-GAME-001, FR-GAME-004, FR-GAM-001, NFR-PERF-002, NFR-A11Y-005 |

## Preconditions

1. Active child session (UC-002).
2. The child is at least at level S1.

## Trigger

The child selects "Sprint" in the main menu.

## Main flow

1. The system shows a sprint configuration: duration 60 or 90 seconds, content domain.
2. The child confirms; the system loads a fitting task pool.
3. A friendly start animation counts down 3-2-1.
4. Tasks appear one after another in a light, friendly UI; the child answers via touch or keyboard (NFR-A11Y-005).
5. After expiry the system shows the result: number of correct tasks, star points, friendly motivational message.
6. The system writes the result into the learning history.

## Alternative flows

- 2a Network slow: the system pre-caches the task pool and shows a loading hint (NFR-PERF-002).
- 4a The child stops early: the partial result is saved; no penalty.

## Exception flows

- 5x Result cannot be persisted: the system keeps the result in session and retries; friendly notice; audit log.

## Postconditions

- Success: result and earned star points persisted.
- Failure: no star points; no inconsistent history.

## Business rules

- BR-001 Sprint duration is configurable to 60 or 90 seconds only.
- BR-002 Errors in sprint do not cause loss of progress (FR-GAM-005).
- BR-003 Star points are earned only by playing (FR-GAM-002).

## Acceptance criteria (BDD)

```gherkin
Feature: UC-014 Child plays sprint mode

  Scenario: Successful 60-second sprint
    Given an active child session at level S2
    When the child starts a 60-second sprint on addition up to 100
    Then the system shows tasks for 60 seconds
    And star points are credited based on correct answers
    And the result is in the learning history

  Scenario: Stopping early keeps partial result
    Given a running sprint
    When the child stops the sprint early
    Then the partial result is saved
    And no star points are deducted
```
