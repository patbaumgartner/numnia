# UC-017 - Spaced repetition retest probe

| Field | Value |
| --- | --- |
| Use-Case ID | UC-017 |
| Title | Spaced repetition retest probe for mastered content |
| Release | R1 |
| Primary actor | Child (7-12) |
| Secondary actors | Adaptive Engine |
| Status | Specified |
| Goal | After the level-specific interval the system probes a previously mastered content domain with 3-5 tasks to verify retention. |
| Related requirements | FR-LEARN-007, FR-LEARN-009, FR-LEARN-011, FR-LEARN-012 |

## Preconditions

1. The child has reached mastery in at least one content domain.
2. The retest interval per stage is configured (SRS §6.1.1).

## Trigger

The retest interval has elapsed since mastery confirmation.

## Main flow

1. The system schedules a short retest of 3-5 tasks for the next session.
2. The child enters a session; the system runs the retest before the regular task pool.
3. The system evaluates the retest:
   - Accuracy >= 80%: mastery remains valid; the next interval extends.
   - Accuracy below 80%: the system queues spaced repetition (no immediate demotion).
   - Sliding-window accuracy below 70%: relapse, demotion (FR-LEARN-011).
4. The system writes the retest event into the learning history.

## Alternative flows

- 2a The child does not start a session within 7 days after the trigger: the system extends the schedule and shows a friendly reminder on the next sign-in.

## Exception flows

- 3x Adaptive engine fails: the system delays the retest and continues with the regular session.

## Postconditions

- Success: retest evaluated; status updated; history updated.
- Failure: previous status preserved.

## Business rules

- BR-001 Mastery is never demoted on a single retest fail (BR follows desirable difficulties).
- BR-002 The retest is short (3-5 tasks).

## Acceptance criteria (BDD)

```gherkin
Feature: UC-017 Spaced repetition retest probe

  Scenario: Retest passes, interval extends
    Given a mastered domain at stage S3 with retest due
    When the child completes the retest with 90 percent accuracy
    Then mastery remains valid
    And the next retest is scheduled at the extended interval

  Scenario: Retest fails, spaced repetition queued
    Given a mastered domain at stage S3 with retest due
    When the child completes the retest with 60 percent accuracy
    Then spaced repetition tasks are queued
    And mastery is not demoted immediately
```
