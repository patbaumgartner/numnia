# UC-045 - Child browses full 24-world catalog

| Field | Value |
| --- | --- |
| Use-Case ID | UC-045 |
| Title | Child browses full 24-world catalog |
| Release | R5 |
| Primary actor | Child (7-12) |
| Secondary actors | Game & Worlds Service |
| Status | Specified |
| Goal | The child explores all 24 worlds in a friendly, light overview. |
| Related requirements | FR-WORLD-001, FR-WORLD-002, FR-WORLD-005, NFR-PERF-001, NFR-A11Y-002 |

## Preconditions

1. All 24 worlds released (R5 target build-out).

## Trigger

The child opens "Worlds".

## Main flow

1. The system shows a 3D world map with 24 friendly worlds on a light background.
2. Each world has a visual difficulty hint and a progress indicator.
3. The child picks a world and proceeds via UC-005.

## Alternative flows

- 1a Locked worlds appear with friendly silhouettes.

## Exception flows

- 1x Asset stream slow: low-detail fallback.

## Postconditions

- Success: world map displayed.

## Business rules

- BR-001 World map design is consistent: light background, friendly pastel palette.

## Acceptance criteria (BDD)

```gherkin
Feature: UC-045 Full world catalog

  Scenario: All 24 worlds visible
    Given the target build-out is reached
    When the child opens the world map
    Then 24 worlds are visible

  Scenario: Locked world as silhouette
    Given a world the child has not yet unlocked
    Then the world appears as a friendly silhouette
```
