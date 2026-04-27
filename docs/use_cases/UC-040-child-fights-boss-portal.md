# UC-040 - Child enters boss portal and fights boss

| Field | Value |
| --- | --- |
| Use-Case ID | UC-040 |
| Title | Child enters boss portal and fights a friendly creature boss |
| Release | R4 |
| Primary actor | Child (7-12) |
| Secondary actors | Game & Worlds Service |
| Status | Specified |
| Goal | The child plays a special boss task in a world; the boss is friendly and good-hearted. |
| Related requirements | FR-WORLD-003, FR-WORLD-004, FR-GAM-005, NFR-A11Y-003 |

## Preconditions

1. Mastery in the world's content domain reached.
2. Boss portal unlocked.

## Trigger

The child selects the boss portal in a world.

## Main flow

1. The system loads the boss arena on a light background with friendly pastels.
2. The boss is a smiling, good-hearted creature with playful animations.
3. The child solves multi-step tasks; correct answers reduce the boss's "Energy" without violence.
4. After success the boss thanks the child and grants a special reward.
5. The result enters the learning history and the gallery.

## Alternative flows

- 3a Defeat: no penalty, friendly motivational notice (FR-GAM-005); the child can retry.
- 1a Reduced motion: animations are dimmed.

## Exception flows

- 2x Asset stream fails: the system loads a low-detail fallback.

## Postconditions

- Success: special reward credited.
- Failure: no inconsistent state.

## Business rules

- BR-001 No violence in animations; bosses are friendly creatures.
- BR-002 Defeats never cost progress (FR-GAM-005).

## Acceptance criteria (BDD)

```gherkin
Feature: UC-040 Boss portal

  Scenario: Successful boss completion
    Given an unlocked boss portal
    When the child correctly answers all boss tasks
    Then the special reward is credited
    And the boss waves friendly

  Scenario: Defeat without penalty
    When the child does not complete the boss
    Then no progress is lost
    And a retry is offered
```
