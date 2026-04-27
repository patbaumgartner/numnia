# UC-028 - Moderation auto-hides suspected case

| Field | Value |
| --- | --- |
| Use-Case ID | UC-028 |
| Title | Moderation auto-hides suspected case |
| Release | R2 |
| Primary actor | Moderation Service |
| Secondary actors | Support, child (passive) |
| Status | Specified |
| Goal | The system auto-hides suspected cases (signal abuse, atypical behavior) and creates a moderation case. |
| Related requirements | FR-SAFE-004, FR-SAFE-005, FR-OPS-003 |

## Preconditions

1. Moderation rules are configured.

## Trigger

A signal/event matches a heuristic (e.g., spam, inappropriate signal pattern).

## Main flow

1. The system marks the affected element as auto-hidden (visible only to moderation).
2. The system opens a moderation case with priority, evidence and audit reference.
3. The case is assigned for review.
4. After review the moderator releases or definitively blocks the element.
5. Notifications go to the affected children in a friendly tone (no shaming).

## Alternative flows

- 4a Review takes longer than the SLA: the case is escalated.

## Exception flows

- 1x Auto-hide cannot be applied: the system raises a critical alarm in monitoring.

## Postconditions

- Success: case processed; result persisted in the audit log.
- Failure: case stays open; alarm.

## Business rules

- BR-001 No public list of moderation cases.
- BR-002 No personal data in user-facing notifications.

## Acceptance criteria (BDD)

```gherkin
Feature: UC-028 Moderation auto-hides suspected case

  Scenario: Auto-hide on suspected signal abuse
    Given a heuristic for signal abuse
    When the heuristic triggers
    Then the affected element is auto-hidden
    And a moderation case is created

  Scenario: Moderator releases the element
    Given an open moderation case
    When the moderator releases the element after review
    Then the auto-hide is removed
    And the result is in the audit log
```
