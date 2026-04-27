# UC-020 - Child picks fantasy display name

| Field | Value |
| --- | --- |
| Use-Case ID | UC-020 |
| Title | Child picks fantasy display name from a vetted list |
| Release | R1 |
| Primary actor | Child (7-12) |
| Secondary actors | Parent (initial setup), Identity Service |
| Status | Specified |
| Goal | The child gets a non-personal display name from a vetted fantasy-name list to ensure safe presence in classes and friend circles. |
| Related requirements | FR-SAFE-003, FR-SAFE-001, NFR-PRIV-001 |

## Preconditions

1. Active child session.
2. A vetted catalog of fantasy names exists.

## Trigger

The child opens "Profile > Display name".

## Main flow

1. The system shows a friendly carousel of fantasy names with optional creature stickers.
2. The child picks a name and confirms.
3. The system checks that the name comes from the vetted list and that it is not blocked.
4. The system updates the child profile.

## Alternative flows

- 2a The desired name is taken in the same class: the system suggests fitting variants.
- 2b The child wants to enter a free-text name: the system blocks free text (FR-SAFE-001).

## Exception flows

- 3x Persistence error: previous name remains in place; friendly notice; audit log.

## Postconditions

- Success: display name updated.
- Failure: previous name preserved.

## Business rules

- BR-001 Free text for child names is forbidden (FR-SAFE-001).
- BR-002 Names must come from the vetted list (FR-SAFE-003).

## Acceptance criteria (BDD)

```gherkin
Feature: UC-020 Child picks fantasy display name

  Scenario: Name from the vetted list is accepted
    Given an active child session
    When the child picks a name from the vetted list
    Then the display name is updated

  Scenario: Free text is rejected
    When the child tries to enter a free-text name
    Then the system blocks the input
    And no change is persisted
```
