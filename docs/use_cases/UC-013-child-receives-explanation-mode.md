# UC-013 - Child receives explanation mode after repeated errors

| Field | Value |
| --- | --- |
| Use-Case ID | UC-013 |
| Title | Child receives explanation mode after repeated errors |
| Primary actor | Child (7-12) |
| Release | R1 |
| Secondary actors | Adaptive Engine, Game & Worlds Service |
| Status | Specified |
| Goal | After three consecutive errors or time-outs in the same content domain the child is offered a guided visual explanation. |
| Related requirements | FR-LEARN-006, FR-LEARN-008, FR-GAME-006, NFR-A11Y-001, NFR-I18N-002 |

## Preconditions

1. Active child session in training or accuracy mode.
2. The adaptive engine tracks the last n results per content domain.

## Trigger

Three consecutive errors or time-outs in the same content domain (frustration protection, SRS §6.1.2).

## Main flow

1. The system pauses the current task pool and shows a friendly notice "Wir schauen uns das gemeinsam an" (CH High German, no sharp s).
2. The system switches to explanation mode and downgrades the speed level by one (G level -1, FR-GAME-006).
3. The system shows visual solution steps (manipulatives, number line, place-value blocks) in a light, friendly look.
4. The child confirms each step; the system explains and animates the step.
5. After 2-3 guided tasks the system offers to return to the previous mode.
6. The system records the explanation event in the learning history (no penalty, FR-GAM-005).

## Alternative flows

- 5a The child wants to keep practicing in accuracy mode: the system stays in G0.
- 5b Reduced motion is active: animations are dimmed (NFR-A11Y-003).

## Exception flows

- 3x Visual asset cannot be loaded: the system shows a textual fallback with audio (NFR-I18N-003).

## Postconditions

- Success: explanation completed; speed level adjusted; history updated.
- Failure: child continues at the previous level; incident logged.

## Business rules

- BR-001 Explanation mode never costs star points or items (FR-GAM-005).
- BR-002 The trigger is exactly three consecutive errors or time-outs in one content domain.
- BR-003 The explanation always uses a light background and friendly characters per design system.

## Acceptance criteria (BDD)

```gherkin
Feature: UC-013 Child receives explanation mode after repeated errors

  Scenario: Three errors in a row trigger the explanation
    Given the child practices subtraction up to 100
    When three tasks in a row are answered incorrectly
    Then the system switches to explanation mode
    And the speed level is reduced by one
    And no star points are deducted

  Scenario: Reduced motion dims explanation animations
    Given reduced motion is enabled
    When the explanation mode runs
    Then the animations are dimmed
```
