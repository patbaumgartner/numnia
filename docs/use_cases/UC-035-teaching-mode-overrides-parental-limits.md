# UC-035 - Teaching mode overrides parental hard limits

| Field | Value |
| --- | --- |
| Use-Case ID | UC-035 |
| Title | Teaching mode overrides parental hard limits during school hours |
| Release | R3 |
| Primary actor | Teacher |
| Secondary actors | Identity & Consent Service, Children of the class |
| Status | Specified |
| Goal | During configured teaching hours, the school's teaching mode supersedes parental hard limits, only for assigned class content. |
| Related requirements | FR-SCH-006, FR-PAR-002, NFR-SEC-003, NFR-PRIV-001 |

## Preconditions

1. Class active.
2. Teaching hours configured.

## Trigger

The child opens the app on a school device or in a class context within teaching hours.

## Main flow

1. The system detects the teaching context (class membership, teaching hours).
2. The system temporarily overrides parental daily limits, only for class content.
3. After the teaching window the parental limits apply again.
4. The override is recorded in the audit log.

## Alternative flows

- 2a Parent has explicitly excluded the override: the parental limits remain in place.

## Exception flows

- 1x Teaching context not detectable: parental rules apply.

## Postconditions

- Success: override is effective only within the teaching window.
- Failure: parental rules apply.

## Business rules

- BR-001 The override applies only to class content, never to free play.
- BR-002 Parents can revoke the override (FR-PAR-001).

## Acceptance criteria (BDD)

```gherkin
Feature: UC-035 Teaching mode overrides parental limits

  Scenario: Override during the teaching hour
    Given a child whose parental limit is reached
    And current teaching hours are active
    When the child opens class content
    Then the system grants playtime within the teaching context
    And records the override in the audit log

  Scenario: Outside teaching hours the parental rule applies
    Given a child whose parental limit is reached
    When teaching hours are not active
    Then play is blocked
```
