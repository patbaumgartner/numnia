# UC-021 - Child plays 1v1 synchronous duel

| Field | Value |
| --- | --- |
| Use-Case ID | UC-021 |
| Title | Child plays 1v1 synchronous turn-based duel |
| Release | R2 |
| Primary actor | Child (7-12) |
| Secondary actors | Multiplayer Service, Matchmaking, opponent child |
| Status | Specified |
| Goal | Two children play a fair, turn-based 1v1 duel with the default speed level G3 (15 s per move). |
| Related requirements | FR-MP-001, FR-MP-002, FR-MP-004, FR-MP-005, FR-MP-007, FR-MP-008, FR-SAFE-001, NFR-PERF-003 |

## Preconditions

1. UC-018 second consent given.
2. Active child session.
3. Connection quality is sufficient.

## Trigger

The child selects "Duel > 1v1".

## Main flow

1. The matchmaker pairs the child with a peer of similar mastery (FR-MP-005, UC-023).
2. The system shows a friendly waiting screen with the companion creature.
3. After the match is found the system loads a light, friendly arena.
4. Both children receive the same task; per move the speed level G3 (15 s) applies (FR-MP-004).
5. The system collects the answers, evaluates them and computes round points; turn-based, no real-time pressure (FR-MP-001).
6. After all rounds the system shows a friendly result screen with star points.
7. The result enters the class- or friend-circle leaderboard only (FR-MP-007/008), never a public board.

## Alternative flows

- 1a No suitable opponent within the wait window: bot fallback (UC-023).
- 4a Connection drops: the system pauses the duel and offers a reconnect.

## Exception flows

- 5x Server response time above the p95 target (NFR-PERF-003): the system shows a friendly notice and continues with extended timeouts.

## Postconditions

- Success: result persisted; star points credited; class- or friend-circle leaderboard updated if applicable.
- Failure: no inconsistent result; no false star points.

## Business rules

- BR-001 No real-time duels (FR-MP-001).
- BR-002 No public global leaderboards (FR-MP-007).
- BR-003 No free text between children (FR-SAFE-001).

## Acceptance criteria (BDD)

```gherkin
Feature: UC-021 Child plays 1v1 synchronous duel

  Scenario: Successful duel between two children at similar level
    Given two children with similar mastery and active multiplayer
    When both enter the duel queue
    Then the system pairs the two children
    And both play turn-based with 15 seconds per move
    And the result enters only the class or friend-circle leaderboard

  Scenario: Connection drop pauses the duel
    Given a running duel
    When one of the children loses the connection
    Then the duel is paused
    And a reconnect is offered
```
