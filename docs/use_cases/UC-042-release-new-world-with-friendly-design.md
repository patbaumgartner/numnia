# UC-042 - Release new world with friendly creature design

| Field | Value |
| --- | --- |
| Use-Case ID | UC-042 |
| Title | Release new world with friendly creature design |
| Release | R4 |
| Primary actor | Content Manager |
| Secondary actors | Math didactics expert (review), Operator |
| Status | Specified |
| Goal | A new world is released through the content pipeline; the world follows the design system: light background, friendly pastel colors, smiling, good-hearted creatures. |
| Related requirements | FR-WORLD-001, FR-WORLD-002, FR-WORLD-005, FR-CRE-001, FR-CRE-007, NFR-PERF-002, NFR-A11Y-002, NFR-A11Y-003 |

## Preconditions

1. World concept and assets are produced.
2. Content review by didactics expert completed (SRS §11.2).

## Trigger

"LiveOps > Worlds > Release".

## Main flow

1. The content manager uploads world assets and metadata (mathematical focus, default difficulty).
2. The system runs design checks: light background, friendly color palette, no scary or violent visuals; creatures show smiling, good-hearted faces.
3. The system runs performance checks (NFR-PERF-002, LOD streaming).
4. The system runs accessibility checks (color-blind, reduced motion).
5. The content manager schedules the release.
6. After release the world is visible in the world map and unlocks per FR-WORLD-004.

## Alternative flows

- 2a Design check fails (e.g., too dark, scary creatures): release rejected; revision requested.

## Exception flows

- 3x Performance check fails: release rejected.

## Postconditions

- Success: new world available.
- Failure: world not released.

## Business rules

- BR-001 All worlds use a light background and friendly pastel colors.
- BR-002 Creatures are always smiling and good-hearted (design system rule).
- BR-003 Creature names support variable endings (FR-CRE-007).

## Acceptance criteria (BDD)

```gherkin
Feature: UC-042 Release new world

  Scenario: Successful release
    Given a new world with assets and didactics review
    When all checks pass
    Then the world is visible in the world map

  Scenario: Design check rejects scary creatures
    Given a world whose creatures look scary
    When the design check runs
    Then the release is rejected
```
