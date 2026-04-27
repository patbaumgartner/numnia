# UC-046 - Full creature evolution and gallery

| Field | Value |
| --- | --- |
| Use-Case ID | UC-046 |
| Title | Full 24-creature evolution and gallery |
| Release | R5 |
| Primary actor | Child (7-12) |
| Secondary actors | Game & Worlds Service |
| Status | Specified |
| Goal | The child collects all 24 starter creatures and follows their evolution stages tied to learning progress. |
| Related requirements | FR-CRE-001, FR-CRE-002, FR-CRE-003, FR-CRE-007, FR-LEARN-009 |

## Preconditions

1. All 24 starter creatures released (R5 target build-out).

## Trigger

The child opens the gallery (UC-019).

## Main flow

1. The system shows 24 creature slots, with friendly pastel colors and smiling faces.
2. Unlocking each creature is tied to mastery in matching content domains.
3. Creatures evolve in stages; each stage shows a unique smiling animation.
4. The child can mark a creature as a companion (UC-019).

## Alternative flows

- 2a Mastery not yet reached: the slot stays a friendly silhouette.

## Exception flows

- 1x Asset stream fails: 2D placeholder.

## Postconditions

- Success: gallery and evolution status persisted.

## Business rules

- BR-001 Creatures must always look smiling and good-hearted.
- BR-002 Names support variable endings (FR-CRE-007).

## Acceptance criteria (BDD)

```gherkin
Feature: UC-046 Full creature evolution

  Scenario: Evolution after mastery
    Given mastery in the matching content domain
    When the threshold is reached
    Then the creature reaches the next stage
    And shows a friendly animation
```
