# UC-008 - Child views own learning progress

| Field | Value |
| --- | --- |
| Use-Case ID | UC-008 |
| Title | Child views own learning progress |
| Release | R1 |
| Primary actor | Child (7-12) |
| Secondary actors | Reporting & Analytics, Mastery Tracker |
| Status | Specified |
| Goal | The child receives a child-friendly overview of its learning progress per operation and world. |
| Related requirements | FR-GAME-005, FR-LEARN-009, NFR-A11Y-002, NFR-A11Y-003, NFR-UX-001, NFR-I18N-002 |

## Preconditions

1. Active child session (UC-002).
2. At least one learning unit has been completed.

## Trigger

The child selects "My progress" in the main menu.

## Main flow

1. The system loads aggregated data per content domain (accuracy in the window, mastery status, number of sessions).
2. The system shows a child-friendly visualization: per operation (Add, Sub, Mult, Div with remainder) one progress bar, a mastery marker and an icon for unlocked worlds/creatures.
3. The child can tap a domain and sees a detail view (e.g. "X tasks left until the next level"), without demotivating leaderboards or comparisons with others.
4. The system does not log the call in a personalized way (no tracking, NFR-PRIV-001).

## Alternative flows

- 1a No data exists yet: the system shows a friendly entry message "Get going and collect your first stars".
- 3a Child enables color-blind profile: all visualizations use the chosen palette.

## Exception flows

- 1x Backend not reachable: the system shows the last known data with the notice "data not current".

## Postconditions

- Success: the child sees the current learning progress; no aggregates over other children.
- Failure: no data leak, clear notice instead of an error message.

## Business rules

- BR-001 Only data of the own profile is shown.
- BR-002 No comparative leaderboards against other children (FR-MP-007 applies analogously in the learning context).
- BR-003 Language is Swiss High German without sharp s, with umlauts.

## Acceptance criteria (BDD)

```gherkin
Feature: UC-008 Child views own learning progress

  Background:
    Given an active child session

  Scenario: Progress per operation is visible
    Given at least three completed training sessions
    When the child opens "My progress"
    Then it sees a separate progress bar per operation
    And the mastery status per content domain is marked

  Scenario: No comparative leaderboards
    Given the progress view is open
    Then the view contains no leaderboard with other children

  Scenario: Color-blind profile is respected
    Given the child has the deuteranopia profile enabled
    When it opens the progress view
    Then the visualization uses the corresponding color palette
```
