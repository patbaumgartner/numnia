# UC-041 - Child collects season rewards

| Field | Value |
| --- | --- |
| Use-Case ID | UC-041 |
| Title | Child collects season rewards |
| Release | R4 |
| Primary actor | Child (7-12) |
| Secondary actors | Game & Worlds Service |
| Status | Specified |
| Goal | The child completes season missions and unlocks seasonal avatar items and creature variants. |
| Related requirements | FR-GAM-001, FR-GAM-002, FR-GAM-005, FR-OPS-001 |

## Preconditions

1. Active season (UC-039).
2. Active child session.

## Trigger

The child opens "Season".

## Main flow

1. The system shows missions, progress and reward path with friendly icons.
2. The child plays missions; progress is tracked.
3. On reaching reward steps the system credits seasonal items (avatar, creature variant).
4. After expiry, items already earned remain in the inventory permanently (FR-CRE-006).

## Alternative flows

- 3a Reduced motion: reward animations dimmed.

## Exception flows

- 3x Item cannot be credited: retry; on permanent failure friendly notice; audit log.

## Postconditions

- Success: items in the inventory.
- Failure: no item duplication.

## Business rules

- BR-001 Seasonal items remain after the season (FR-CRE-006).

## Acceptance criteria (BDD)

```gherkin
Feature: UC-041 Season rewards

  Scenario: Reward step reached
    Given an active season
    When the child reaches reward step 3
    Then the seasonal item is in the inventory

  Scenario: Items remain after the season
    Given an item earned in the season
    When the season ends
    Then the item remains in the child's inventory
```
