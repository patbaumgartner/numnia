# UC-036 - Children play 2v2 team challenge

| Field | Value |
| --- | --- |
| Use-Case ID | UC-036 |
| Title | Children play cooperative 2v2 team challenge |
| Release | R3 |
| Primary actor | Child (7-12) |
| Secondary actors | Multiplayer Service |
| Status | Specified |
| Goal | Two pairs play a turn-based 2v2 challenge with team scoring. |
| Related requirements | FR-MP-001, FR-MP-002, FR-MP-005, FR-SAFE-001 |

## Preconditions

1. Multiplayer enabled (UC-018).
2. Matchmaker can form two pairs (UC-023).

## Trigger

The child selects "Duel > 2v2".

## Main flow

1. The matchmaker forms two pairs of similar mastery.
2. The system loads the team arena with a light, friendly look.
3. Each round both pairs play turn-based; total team scores are computed.
4. The result lands on the class- or friend-circle leaderboard.

## Alternative flows

- 1a No suitable pair within the wait window: bot fallback.

## Exception flows

- 3x Connection issue with one team: the system pauses the round and offers a reconnect.

## Postconditions

- Success: team result persisted.
- Failure: no inconsistent score.

## Business rules

- BR-001 Team rankings stay class-internal.

## Acceptance criteria (BDD)

```gherkin
Feature: UC-036 2v2 team challenge

  Scenario: Successful team challenge
    Given two pairs of children with similar mastery
    When the team challenge starts
    Then both teams play turn-based
    And the result lands only on the class or friend-circle leaderboard
```
