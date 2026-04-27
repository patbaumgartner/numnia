# UC-016 - Child achieves daily and weekly goals

| Field | Value |
| --- | --- |
| Use-Case ID | UC-016 |
| Title | Child achieves daily and weekly goals |
| Release | R1 |
| Primary actor | Child (7-12) |
| Secondary actors | Game & Worlds Service |
| Status | Specified |
| Goal | The child sees daily and weekly goals, completes them and receives reward star points. |
| Related requirements | FR-GAM-001, FR-GAM-002, FR-GAM-004, NFR-A11Y-001 |

## Preconditions

1. Active child session.
2. Configured goal catalog (FR-OPS-001).

## Trigger

The child opens the home screen.

## Main flow

1. The system shows up to three daily and one weekly goal with a friendly progress bar.
2. The child plays and progresses in the goals.
3. On reaching a goal the system plays a short, friendly creature animation and credits star points.
4. The system marks the goal as completed; remaining goals are still visible.
5. Daily goals reset at the start of the next calendar day; weekly goals on Monday 00:00 CH local time.

## Alternative flows

- 1a No active goals (e.g., reset just happened): the system shows the next goals.
- 3a Reduced motion is active: animation is dimmed (NFR-A11Y-003).

## Exception flows

- 3x Star points cannot be credited: the system retries; on permanent failure friendly notice; audit log; no double credit.

## Postconditions

- Success: completed goal persisted; star points credited exactly once.
- Failure: goal stays open; no incorrect star points.

## Business rules

- BR-001 Star points are earned only by playing, never purchased (FR-GAM-002).
- BR-002 Reset times follow CH local time.

## Acceptance criteria (BDD)

```gherkin
Feature: UC-016 Child achieves daily and weekly goals

  Scenario: Daily goal completed
    Given a daily goal "Solve 20 tasks correctly"
    When the child solves the 20th task correctly
    Then the goal is marked as completed
    And star points are credited exactly once

  Scenario: Daily goals reset at midnight CH local time
    Given completed daily goals on day D
    When the date changes to D+1 in CH local time
    Then new daily goals are visible
```
