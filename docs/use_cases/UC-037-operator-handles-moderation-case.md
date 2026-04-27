# UC-037 - Operator handles moderation case

| Field | Value |
| --- | --- |
| Use-Case ID | UC-037 |
| Title | Operator handles moderation case end-to-end |
| Release | R3 |
| Primary actor | Support / Moderator |
| Secondary actors | Moderation Service, affected child (passive) |
| Status | Specified |
| Goal | A moderator processes an open moderation case with prioritization and clean documentation. |
| Related requirements | FR-OPS-003, FR-SAFE-004, FR-SAFE-005 |

## Preconditions

1. At least one open moderation case (UC-028).

## Trigger

The moderator opens "Moderation > Cases".

## Main flow

1. The system shows the case list sorted by priority.
2. The moderator picks a case; the system shows evidence and audit reference.
3. The moderator decides: release, definitively block, escalate.
4. The system writes the decision into the audit log and informs the affected child in a friendly tone if needed.

## Alternative flows

- 3a Escalation: the case goes to a senior support level.

## Exception flows

- 2x Evidence cannot be loaded: incident logged; case stays open.

## Postconditions

- Success: case closed and documented.
- Failure: case stays open; alarm.

## Business rules

- BR-001 Decisions are auditable.
- BR-002 No personal data in user-facing notifications.

## Acceptance criteria (BDD)

```gherkin
Feature: UC-037 Handle moderation case

  Scenario: Definitive block
    Given an open case with verified evidence
    When the moderator picks "definitively block"
    Then the element stays hidden
    And the decision is in the audit log

  Scenario: Release
    Given an open case without verified evidence
    When the moderator picks "release"
    Then the auto-hide is removed
```
