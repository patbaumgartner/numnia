# UC-024 - Child uses safe signal communication in duels

| Field | Value |
| --- | --- |
| Use-Case ID | UC-024 |
| Title | Child uses predefined signals/emojis in duels |
| Release | R2 |
| Primary actor | Child (7-12) |
| Secondary actors | Multiplayer Service, Moderation |
| Status | Specified |
| Goal | The child can send predefined friendly signals/emojis/short phrases to the duel partner; free text is excluded. |
| Related requirements | FR-SAFE-001, FR-SAFE-002, FR-SAFE-004, FR-SAFE-005 |

## Preconditions

1. UC-018 second consent given.
2. The child is in a duel (UC-021/022) or class challenge (UC-032).

## Trigger

The child opens the signal panel.

## Main flow

1. The system shows up to 12 friendly signals (e.g., "Toll!", "Viel Glück", "Gut gemacht") and emojis.
2. The child picks a signal; the system sends it to the duel partner.
3. Sender, receiver and signal are logged for moderation (FR-SAFE-005).

## Alternative flows

- 1a Reduced motion: signals appear without intense animation (NFR-A11Y-003).

## Exception flows

- 2x Throughput limit reached (anti-spam): the system blocks further signals for a cooldown.

## Postconditions

- Success: signal transmitted and logged.
- Failure: signal not transmitted; no inconsistent log.

## Business rules

- BR-001 Free text is forbidden (FR-SAFE-001).
- BR-002 Anti-spam: at most 6 signals per minute per child.

## Acceptance criteria (BDD)

```gherkin
Feature: UC-024 Child uses signal communication

  Scenario: Sending a friendly signal
    Given a running duel
    When the child sends the signal "Viel Glück"
    Then the partner receives the signal
    And the event is in the moderation log

  Scenario: Anti-spam blocks too many signals
    Given six signals within one minute
    When the child sends a seventh signal
    Then the system blocks the signal for a cooldown
```
