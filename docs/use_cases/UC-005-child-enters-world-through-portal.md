# UC-005 - Child enters a world through a portal

| Field | Value |
| --- | --- |
| Use-Case ID | UC-005 |
| Title | Child enters a world through a training portal |
| Release | R1 |
| Primary actor | Child (7-12) |
| Secondary actors | Game & Worlds Service, Asset Storage (per ADR-001) |
| Status | Specified |
| Goal | The child picks an unlocked world, enters it through a training portal and can tackle tasks there. |
| Related requirements | FR-WORLD-001, FR-WORLD-002, FR-WORLD-003, FR-WORLD-004, FR-WORLD-005, NFR-PERF-002, NFR-A11Y-002, NFR-A11Y-003 |

## Preconditions

1. Active child session (UC-002).
2. Exactly 3 worlds are available in R1 (SRS 12.1).
3. At least the portal of type "Training" is available per world.

## Trigger

The child selects "Discover world" in the main menu.

## Main flow

1. The system shows a 3D map with the three available worlds and visual hints about difficulty and progress (FR-WORLD-005).
2. The child picks a world.
3. The system loads world assets from MinIO with LOD streaming (target p75 <= 5 s, NFR-PERF-002).
4. The system shows the training portal of the world; further portal types (duel, team, event, boss, class, season) are visibly locked in R1 with a "coming later" notice.
5. The child interacts with the training portal.
6. The system checks unlock rules (level, task pool, event window) and opens the portal (FR-WORLD-004).
7. The system routes to the practice stage of the world; UC-003 or UC-004 starts depending on the choice.

## Alternative flows

- 4a Reduced-motion is active (NFR-A11Y-003): the system reduces animations and particle effects.
- 4b Color-blind profile active: the system uses the corresponding color palette (NFR-A11Y-002).
- 6a Unlock rule not satisfied: the system shows a child-friendly notice and proposes a fitting world.

## Exception flows

- 3x Asset stream fails: the system loads a low-detail fallback and reports the incident to monitoring.
- 6x Backend error during the rule check: the portal stays closed, friendly notice, audit log.

## Postconditions

- Success: the child is in the practice stage of the chosen world; world load time is measured.
- Failure: no stage change; main menu still reachable.

## Business rules

- BR-001 In Release 1 exactly three worlds can be entered; further worlds are visibly locked.
- BR-002 Only portals whose rules are satisfied may be opened.
- BR-003 Visual hints must make difficulty and progress clearly recognizable.

## Acceptance criteria (BDD)

```gherkin
Feature: UC-005 Child enters a world through a portal

  Background:
    Given an active child session
    And three worlds are unlocked in Release 1

  Scenario: Training portal opens when rules are satisfied
    Given the child has reached level S2
    And the task pool of the world "Mushroom Jungle" is configured
    When the child enters the training portal of Mushroom Jungle
    Then the system switches to the practice stage of the world

  Scenario: Reduced-motion reduces animations
    Given the child has reduced-motion enabled
    When it enters a world
    Then particle effects and intense animations are reduced

  Scenario: Locked portal stays closed
    Given a portal of type "Duel"
    When the child taps on it in Release 1
    Then the system shows the notice "coming later"
    And the portal stays closed
```
