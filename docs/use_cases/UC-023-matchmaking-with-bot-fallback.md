# UC-023 - Matchmaking with bot fallback

| Field | Value |
| --- | --- |
| Use-Case ID | UC-023 |
| Title | Matchmaking with bot fallback |
| Release | R2 |
| Primary actor | Matchmaking Service |
| Secondary actors | Child, Bot |
| Status | Specified |
| Goal | Children are paired with peers of similar mastery; missing opponents trigger a bot fallback so children never wait too long. |
| Related requirements | FR-MP-005, FR-MP-006, NFR-PERF-003 |

## Preconditions

1. Multiplayer enabled.
2. Bot pool with mastery profiles is configured.

## Trigger

A child enters a matchmaking queue (UC-021, UC-036).

## Main flow

1. The matchmaker computes a fairness score from learning level, accuracy, speed and connection quality.
2. Within a wait window of up to 30 s the matchmaker tries to pair real children.
3. On success the duel starts (UC-021).
4. On failure the matchmaker assigns a bot whose profile matches the child's mastery (FR-MP-006).
5. The child sees a friendly notice "Today you play against a bot helper" before start.

## Alternative flows

- 4a A real child enters during the wait window: the matchmaker prefers the human pairing.

## Exception flows

- 1x Matchmaking service is overloaded: the system shows a friendly notice and reduces queue volume.

## Postconditions

- Success: a fair duel is set up.
- Failure: queue exit with a friendly notice; no inconsistent duel.

## Business rules

- BR-001 The wait window is 30 s by default and is configurable (FR-OPS-002).
- BR-002 Bots are visibly marked.

## Acceptance criteria (BDD)

```gherkin
Feature: UC-023 Matchmaking with bot fallback

  Scenario: Pairing of two children
    Given two children with similar mastery in the queue
    When the wait window starts
    Then the system pairs the two children

  Scenario: Bot fallback after the wait window
    Given a child in the queue without a matching peer
    When 30 seconds have passed
    Then the system assigns a bot of similar mastery
    And the child is informed about the bot
```
