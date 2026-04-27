# UC-027 - Child enters event portal

| Field | Value |
| --- | --- |
| Use-Case ID | UC-027 |
| Title | Child enters an event portal |
| Release | R2 |
| Primary actor | Child (7-12) |
| Secondary actors | Game & Worlds Service |
| Status | Specified |
| Goal | The child plays event tasks during the event window and earns event rewards. |
| Related requirements | FR-WORLD-003, FR-WORLD-004, FR-GAM-004 |

## Preconditions

1. Active event (UC-026).
2. Active child session.

## Trigger

The child selects an event portal in a world.

## Main flow

1. The system checks the event window and unlock rules (FR-WORLD-004).
2. The system loads the event stage with friendly decoration on a light background.
3. The child plays event tasks; the system credits star points and event rewards.
4. After expiry the event portal becomes a non-interactive memorial; the rewards already earned remain.

## Alternative flows

- 1a Event window expired: portal is closed with a friendly "ended" notice.

## Exception flows

- 3x Reward cannot be credited: retry; on permanent failure friendly notice; audit log.

## Postconditions

- Success: event tasks counted; rewards credited.
- Failure: no rewards; no inconsistent state.

## Business rules

- BR-001 Event rewards are added to existing rewards (FR-GAM-005).

## Acceptance criteria (BDD)

```gherkin
Feature: UC-027 Child enters event portal

  Scenario: Active event during the window
    Given an active event in the world "Mushroom Jungle"
    When the child enters the event portal
    Then the event stage opens
    And event rewards can be earned

  Scenario: Closed portal after the event
    Given an expired event
    When the child tries to enter the event portal
    Then the system shows the notice "ended"
```
