# UC-025 - Child views class or friend-circle leaderboard

| Field | Value |
| --- | --- |
| Use-Case ID | UC-025 |
| Title | Child views class or friend-circle leaderboard |
| Release | R2 |
| Primary actor | Child (7-12) |
| Secondary actors | Multiplayer Service |
| Status | Specified |
| Goal | The child sees a private, friendly leaderboard within their class or friend circle without public exposure. |
| Related requirements | FR-MP-007, FR-MP-008, FR-SAFE-003, NFR-PRIV-001 |

## Preconditions

1. The child is part of a class (R3) or has friends.
2. Display names follow the fantasy-name catalog (FR-SAFE-003).

## Trigger

The child opens "Friends > Leaderboard" or "Class > Leaderboard".

## Main flow

1. The system shows the top 10 entries with fantasy name, companion creature and star points.
2. The child sees its own position highlighted in a friendly color.
3. Periods can be selected: this week, this month, all time.

## Alternative flows

- 1a The list is shorter than 10 entries: the system shows the available entries.

## Exception flows

- 1x The leaderboard cannot load: the system shows a friendly placeholder.

## Postconditions

- Success: leaderboard displayed.
- Failure: friendly placeholder; no leakage of personal data.

## Business rules

- BR-001 No global, public leaderboards (FR-MP-007).
- BR-002 Only fantasy names; no real names (FR-SAFE-003).

## Acceptance criteria (BDD)

```gherkin
Feature: UC-025 Class or friend-circle leaderboard

  Scenario: Viewing the friends top 10
    Given a child with at least 5 friends
    When the child opens the friends leaderboard
    Then the system shows up to 10 entries with fantasy name and creature
    And no real name is visible

  Scenario: No public global leaderboard
    When the child searches for a global leaderboard
    Then the system shows that only class- and friend-circle leaderboards exist
```
