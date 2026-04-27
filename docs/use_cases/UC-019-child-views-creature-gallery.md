# UC-019 - Child views creature gallery

| Field | Value |
| --- | --- |
| Use-Case ID | UC-019 |
| Title | Child views creature gallery |
| Release | R1 |
| Primary actor | Child (7-12) |
| Secondary actors | Asset Storage (per ADR-001) |
| Status | Specified |
| Goal | The child browses already unlocked creatures in a friendly, light gallery and inspects each creature in 3D. |
| Related requirements | FR-CRE-001, FR-CRE-002, FR-CRE-003, FR-CRE-004, FR-CRE-007, NFR-PERF-001, NFR-A11Y-005 |

## Preconditions

1. Active child session.
2. At least one creature has been unlocked (UC-006).

## Trigger

The child selects "Gallery" in the main menu.

## Main flow

1. The system shows the gallery on a light background with soft pastel colors and rounded shapes (design system).
2. Unlocked creatures are shown with a friendly, smiling, good-hearted look; locked creatures appear as silhouettes.
3. The child picks a creature; the system shows a 3D preview with name, evolution stage and a short, friendly biography.
4. The child can play the creature's animation (wave, jump, smile).
5. The child can mark a creature as the active companion (FR-CRE-004).

## Alternative flows

- 3a Reduced motion is active: animations are dimmed (NFR-A11Y-003).
- 5a A different creature is already the companion: the system swaps it.

## Exception flows

- 3x 3D asset cannot be loaded: the system shows a 2D placeholder with the same friendly look; incident logged.

## Postconditions

- Success: companion choice persisted; gallery shows current state.
- Failure: previous state preserved.

## Business rules

- BR-001 Creatures are always depicted as friendly and smiling (design system rule).
- BR-002 The background of the gallery is light; colors are friendly pastels.
- BR-003 Creature names support variable endings (FR-CRE-007).

## Acceptance criteria (BDD)

```gherkin
Feature: UC-019 Child views creature gallery

  Scenario: Picking the active companion
    Given the child has unlocked at least 2 creatures
    When the child selects a creature as companion
    Then the companion is set on the child profile
    And the gallery shows the new companion

  Scenario: Locked creatures appear as silhouettes
    Given the child has not yet unlocked all creatures
    When the gallery is opened
    Then locked creatures are shown as silhouettes
```
