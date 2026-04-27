# UC-015 - Child plays level mode (S1-S6 progression)

| Field | Value |
| --- | --- |
| Use-Case ID | UC-015 |
| Title | Child plays level mode |
| Release | R1 |
| Primary actor | Child (7-12) |
| Secondary actors | Adaptive Engine, Game & Worlds Service |
| Status | Specified |
| Goal | The child works through stages S1-S6 systematically and unlocks the next stage on mastery. |
| Related requirements | FR-GAME-001, FR-GAME-003, FR-LEARN-004, FR-LEARN-009, FR-LEARN-012 |

## Preconditions

1. Active child session.
2. The mastery model is configured (SRS §6.1.1).

## Trigger

The child selects "Level" in the main menu.

## Main flow

1. The system shows the current stage (S1-S6) with progress per content domain.
2. The child picks a content domain and starts.
3. The system serves tasks of the current stage; result and times are recorded.
4. On reaching the mastery thresholds (accuracy, response time, sessions on different days) the system marks the domain as mastered (FR-LEARN-009/012).
5. The system unlocks the next stage and announces it with a friendly creature animation.

## Alternative flows

- 4a Mastery not yet reached: the system continues with the current stage and offers spaced repetition.
- 4b Retention falls below the threshold (FR-LEARN-011): the system demotes the domain into the spaced-repetition queue.

## Exception flows

- 4x Persistence error: the system keeps progress in session and retries; friendly notice; audit log.

## Postconditions

- Success: stage progress and mastery status persisted.
- Failure: previous status preserved.

## Business rules

- BR-001 Mastery is granted only after at least 2 sessions on at least 2 different calendar days (FR-LEARN-012).
- BR-002 Stage transitions are visually announced in a friendly, light style.

## Acceptance criteria (BDD)

```gherkin
Feature: UC-015 Child plays level mode

  Scenario: Mastery unlocks the next stage
    Given the child is at stage S2 in addition up to 100
    And the mastery thresholds for S2 are met across 2 days
    When the child plays one more session
    Then the domain is marked as mastered
    And stage S3 is unlocked

  Scenario: Retention drop demotes the domain
    Given the domain is mastered
    When the sliding-window accuracy falls below 70 percent
    Then the domain is moved into the spaced-repetition queue of the previous stage
```
