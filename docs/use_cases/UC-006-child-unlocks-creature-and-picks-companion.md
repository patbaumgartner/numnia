# UC-006 - Child unlocks creature and picks companion

| Field | Value |
| --- | --- |
| Use-Case ID | UC-006 |
| Title | Child unlocks a creature and picks it as companion |
| Release | R1 |
| Primary actor | Child (7-12) |
| Secondary actors | Gamification & Inventory, Learning & Mastery |
| Status | Specified |
| Goal | The child receives a new creature through learning progress and can activate it as companion. |
| Related requirements | FR-CRE-001, FR-CRE-002, FR-CRE-003, FR-CRE-004, FR-CRE-007, FR-GAM-001, FR-GAM-005 |

## Preconditions

1. Active child session (UC-002).
2. At least one of the three R1 creatures is not yet unlocked.
3. A learning-progress threshold (e.g. mastery in a domain) is configured.

## Trigger

A learning-progress event (e.g. mastery in a content domain) triggers the unlock, or the child opens the gallery.

## Main flow

1. The system detects from the learning progress that a new creature can be unlocked (FR-CRE-002).
2. The system shows a child-friendly unlock animation with the name of the creature (variable ending allowed, FR-CRE-007).
3. The creature is permanently added to the child's inventory (FR-CRE-006 applies analogously to creatures).
4. The child opens the gallery (FR-CRE-003) and sees the new creature.
5. The child picks a creature as companion (FR-CRE-004).
6. The system updates the profile; in training/accuracy mode the creature will appear in the scene from now on.

## Alternative flows

- 1a Threshold reached but all three R1 creatures already unlocked: the system shows a "more creatures will follow" notice and grants star points as a consolation reward (FR-GAM-001).
- 4a Child does not want to swap companion: no change; previous creature remains active.

## Exception flows

- 3x Persistence error: transaction rolled back, the unlock is offered again at the next sign-in.
- 5x Attempt to pick a creature that is not unlocked as companion: server denies with 409; audit log.

## Postconditions

- Success: creature permanently in the inventory; companion choice persisted in the profile.
- Failure: no inconsistent inventories.

## Business rules

- BR-001 Creatures are permanent; loss through errors is excluded.
- BR-002 Creature names must support variable endings; an enforced ending (e.g. on "i") is forbidden.
- BR-003 Swapping the companion is allowed at any time.

## Acceptance criteria (BDD)

```gherkin
Feature: UC-006 Child unlocks creature and picks companion

  Background:
    Given an active child session
    And a configured unlock threshold for the creature "Pilzar"

  Scenario: Successful unlock via mastery
    Given the child reaches mastery in the related domain
    When the system processes the unlock
    Then the creature appears permanently in the gallery
    And it can be picked as companion

  Scenario: Variable name endings are accepted
    Given the candidate names "Pilzar", "Welleno", "Zacka"
    When the system validates the names
    Then all three are accepted
    And the system rejects no name due to a missing "i" ending

  Scenario: Picking a non-unlocked creature as companion is rejected
    Given a creature that is not yet unlocked
    When the child tries to pick it as companion
    Then the server responds with status 409
    And the previous companion stays active
```
