# UC-003 - Child starts training mode for chosen operation

| Field | Value |
| --- | --- |
| Use-Case ID | UC-003 |
| Title | Child starts training mode for chosen operation |
| Release | R1 |
| Primary actor | Child (7-12) |
| Secondary actors | Adaptive engine, mastery tracker, spaced repetition scheduler |
| Status | Specified |
| Goal | The child practices a self-chosen basic arithmetic operation at an appropriate level and receives adaptive feedback. |
| Related requirements | FR-LEARN-001, FR-LEARN-002, FR-LEARN-003, FR-LEARN-004, FR-LEARN-005, FR-LEARN-006, FR-LEARN-007, FR-LEARN-008, FR-LEARN-009, FR-LEARN-010, FR-LEARN-011, FR-LEARN-012, FR-GAME-001, FR-GAME-005, FR-GAME-006, FR-CRE-004, NFR-PERF-002, NFR-A11Y-001, NFR-I18N-002, NFR-I18N-004 |

## Preconditions

1. UC-002 complete; active child session.
2. The chosen world is unlocked and its task pool is configured (FR-OPS-002).
3. An active companion creature is recorded in the profile (FR-CRE-004); if not, the system picks the starter creature as default.

## Trigger

The child selects "Practice" in the main menu and picks an operation (addition, subtraction, multiplication, division with remainder).

## Main flow

1. The system shows the available operations with visual hints about difficulty (S) and speed (G).
2. The child picks an operation; the system routes to the related world/practice stage (default level per SRS Annex A).
3. The system determines the next difficulty level S and speed level G from the child profile.
4. The task generator creates a task from the configured pool (type per FR-LEARN-003); the result stays within the number range up to 1,000,000.
5. The system renders the task in the 3D scene; the companion creature is visible.
6. The child answers the task (keyboard, touch or speech-output assisted).
7. The system evaluates the answer and the response time; the mastery tracker updates the sliding window, the spaced repetition scheduler records wrong or hesitant answers.
8. The system gives immediate, child-friendly feedback (right/wrong, without penalty mechanics).
9. The adaptive engine decides whether the next task keeps the same level, adjusts speed or proposes a mode (e.g. explanation mode).
10. Steps 4-9 repeat until the child ends or the session duration (10-15 min) elapses.
11. The system writes the history, updates learning progress (UC-008) and checks mastery conditions (SRS 6.1.1).
12. The system shows a child-friendly session summary (number of tasks, correct count, companion reaction, star points if any).

## Alternative flows

- 4a Task pool for the world is empty/not configured: the system shows a friendly notice and proposes another world.
- 6a Child uses explanation mode (FR-LEARN-008): the system plays animated solution steps, without penalty mechanics.
- 8a Three consecutive errors or time-outs in the same content domain: the adaptive engine reduces the G level by 1 and proposes accuracy or explanation mode (SRS 6.1.2).
- 9a Success corridor left (>90% or <70% across the window): the adaptive engine raises or lowers difficulty/speed accordingly.
- 11a Mastery conditions met for the first time, but consolidation across two calendar days not yet given: the system keeps mastery open until the follow-up session (FR-LEARN-012).

## Exception flows

- 5x Asset loading error: the system shows a simplified 2D fallback rendering; audit/monitoring entry.
- 7x Backend not reachable: the system buffers the answer locally and retries upload on reconnect; on permanent failure the session terminates cleanly.
- Xx Attempt to generate a task outside the number range: generator declines, log entry, alternative task selected.

## Postconditions

- Success: learning history, mastery status and spaced repetition plan are updated; star points credited if applicable (FR-GAM-001/002); session summary visible.
- Failure: no inconsistent histories; aborted tasks are not double-counted.

## Business rules

- BR-001 Tasks and results stay within the number range up to 1,000,000.
- BR-002 Errors cost neither star points nor items (frustration-free, FR-GAM-005).
- BR-003 Three consecutive errors/time-outs trigger a speed downgrade plus mode suggestion.
- BR-004 Mastery is granted only after at least two sessions on two different calendar days.
- BR-005 Voice output and task texts follow Swiss High German without sharp s, with umlauts.

## Acceptance criteria (BDD)

```gherkin
Feature: UC-003 Child starts training mode for chosen operation

  Background:
    Given an active child session
    And a configured task pool for the chosen world

  Scenario: Adaptive speed downgrade after three errors
    Given the child practices multiplication on S3/G3
    When it answers three tasks in a row wrong or by time-out
    Then the adaptive engine sets the speed to G2
    And proposes accuracy or explanation mode

  Scenario: Tasks stay within the number range up to 1,000,000
    Given the child practices addition on S6
    When the task generator creates a new task
    Then the expected result lies between 0 and 1,000,000

  Scenario: Mastery is granted only after consolidation
    Given the child meets the accuracy and speed thresholds for S2 today
    And only one session on one calendar day exists so far
    When the session ends
    Then the mastery status for S2 remains "in consolidation"
    And mastery is confirmed only after a second session on another calendar day

  Scenario: Error costs no star points
    Given the child has 12 star points
    When it answers a task wrong
    Then the star points balance stays at 12
```
