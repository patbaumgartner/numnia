# UC-032 - Teacher configures class challenge

| Field | Value |
| --- | --- |
| Use-Case ID | UC-032 |
| Title | Teacher configures cooperative or competitive class challenge |
| Release | R3 |
| Primary actor | Teacher |
| Secondary actors | Children of the class |
| Status | Specified |
| Goal | A teacher launches a class challenge in cooperative or competitive mode. |
| Related requirements | FR-SCH-003, FR-MP-002, FR-MP-008 |

## Preconditions

1. Class exists.

## Trigger

The teacher opens "Class > Challenge > New".

## Main flow

1. The teacher picks a template (cooperative: collective goal; competitive: ranking within the class).
2. The teacher configures duration, content domain and rewards.
3. The system activates the challenge for the class.
4. Children see a friendly challenge tile on the home screen.
5. After expiry the system shows the result; rewards are credited.

## Alternative flows

- 5a Cooperative goal not reached: friendly motivational notice; no penalty (FR-GAM-005).

## Exception flows

- 3x Activation fails: friendly notice; audit log.

## Postconditions

- Success: challenge runs; result and rewards persisted.
- Failure: no challenge.

## Business rules

- BR-001 Class rankings stay class-internal (FR-MP-008).

## Acceptance criteria (BDD)

```gherkin
Feature: UC-032 Class challenge

  Scenario: Cooperative class challenge reached
    Given an active cooperative class challenge
    When the class collectively reaches the goal
    Then all children of the class get the reward

  Scenario: Competitive challenge with class-internal ranking
    Given an active competitive class challenge
    When the challenge ends
    Then the system shows a class-internal ranking only
```
