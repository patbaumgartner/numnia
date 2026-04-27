# UC-012 - Child uses accessibility mode (dyscalculia, color-blind, reduced motion)

| Field | Value |
| --- | --- |
| Use-Case ID | UC-012 |
| Title | Child uses accessibility mode |
| Release | R1 |
| Primary actor | Child (7-12) |
| Secondary actors | Parent (initial activation), Game & Worlds Service |
| Status | Specified |
| Goal | The child can practice with reduced stimulus density, color-blind palette, reduced motion or extended time limits. |
| Related requirements | NFR-A11Y-001, NFR-A11Y-002, NFR-A11Y-003, NFR-A11Y-004, NFR-A11Y-005, NFR-I18N-001, NFR-I18N-002 |

## Preconditions

1. The parent has activated at least one accessibility profile in UC-001 or in the parent area.
2. Active child session (UC-002).

## Trigger

The child opens "Settings > Mode" or accessibility is auto-activated from the child profile.

## Main flow

1. The system shows three independent profiles: dyscalculia, color-blind (deuteranopia/protanopia/tritanopia), reduced motion.
2. The child or parent activates one or several profiles.
3. The system applies the rules: extended time limits in dyscalculia, contrast palette per color-blind profile, dimmed animations in reduced motion.
4. The system saves the choice on the child profile and uses it in all worlds and modes.
5. The child continues practicing; UI confirms "Mode active" via a friendly icon.

## Alternative flows

- 2a Keyboard-only operation (NFR-A11Y-005): the child reaches all settings via Tab/Enter.
- 2b Screen reader is active in the parent area (NFR-A11Y-004): all controls have ARIA labels.

## Exception flows

- 4x Persisting the profile fails: the system keeps the change in session and retries; on permanent failure friendly notice and audit log.

## Postconditions

- Success: accessibility profile is persisted and effective in all worlds.
- Failure: previous setting remains in place.

## Business rules

- BR-001 Several accessibility profiles can be combined.
- BR-002 Accessibility profiles never alter the math content, only its presentation.
- BR-003 UI texts remain in Swiss High German with umlauts (NFR-I18N-002, NFR-I18N-004).

## Acceptance criteria (BDD)

```gherkin
Feature: UC-012 Child uses accessibility mode

  Background:
    Given an active child session

  Scenario: Reduced motion dims animations
    Given the child profile has reduced motion enabled
    When the child enters a world
    Then particle effects and intense animations are reduced

  Scenario: Color-blind profile applies palette
    Given the deuteranopia profile is enabled
    When the child opens a task
    Then the system uses the deuteranopia palette

  Scenario: Dyscalculia mode extends time limits
    Given dyscalculia mode is enabled
    When the child plays in speed level G3
    Then the time limit is extended according to the dyscalculia rule
    And the math content stays unchanged
```
