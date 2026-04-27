# UC-022 - Child plays asynchronous duel

| Field | Value |
| --- | --- |
| Use-Case ID | UC-022 |
| Title | Child plays asynchronous duel against a friend |
| Release | R2 |
| Primary actor | Child (7-12) |
| Secondary actors | Multiplayer Service, friend (child) |
| Status | Specified |
| Goal | A child sends a duel challenge to a friend; the friend completes their part later; the result is shown to both. |
| Related requirements | FR-MP-002, FR-MP-003, FR-MP-007, FR-SAFE-001, FR-SAFE-002 |

## Preconditions

1. Multiplayer is enabled (UC-018).
2. The child has at least one friend in the friend circle.

## Trigger

The child selects "Friend duel > Send".

## Main flow

1. The system shows the friend list with fantasy names and companion creatures.
2. The child picks a friend and a content domain.
3. The system generates the duel and stores the child's round.
4. The friend gets a child-friendly notification on next sign-in.
5. The friend plays the round; the system computes the result of both rounds.
6. Both children see the result on next sign-in via a friendly creature animation.

## Alternative flows

- 5a The friend does not respond within 7 days: the duel expires; both see a friendly notice.

## Exception flows

- 3x Persistence error: no duel is created; friendly notice.

## Postconditions

- Success: duel result persisted; star points credited.
- Failure: no duel; no inconsistent state.

## Business rules

- BR-001 Communication is limited to predefined signals/emojis (FR-SAFE-002).
- BR-002 Asynchronous duels are valid for 7 days.

## Acceptance criteria (BDD)d

```gherkin
Feature: UC-022 Child plays asynchronous duel

  Scenario: Duel is completed by both sides
    Given an asynchronous duel from child A to child B
    When child B plays its round within 7 days
    Then both children see the result on next sign-in

  Scenario: Duel expires after 7 days
    Given an asynchronous duel without a response
    When more than 7 days have passed
    Then the duel is marked as expired
    And both see a friendly notice
```
