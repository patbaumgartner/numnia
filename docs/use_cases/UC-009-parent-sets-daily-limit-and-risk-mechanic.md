# UC-009 - Parent sets daily limit and controls the risk mechanic

| Field | Value |
| --- | --- |
| Use-Case ID | UC-009 |
| Title | Parent sets daily hard limit and disables the risk mechanic |
| Release | R1 |
| Primary actor | Parent |
| Secondary actors | Gamification & Inventory (shield + round pool), Audit |
| Status | Specified |
| Goal | Parents configure daily play-time hard limits and decide whether the risk mechanic (shield + round pool) is active. |
| Related requirements | FR-PAR-001, FR-PAR-002, FR-PAR-003, FR-GAM-005, FR-GAM-006, FR-SAFE-005, NFR-SEC-003, NFR-PRIV-001 |

## Preconditions

1. Verified parent account (UC-001).
2. At least one child profile exists.

## Trigger

The parent opens the parent self-service and selects "Play time and risk".

## Main flow

1. The system shows for each child profile: current daily hard limit (default: 30 min), break recommendation, status of the risk mechanic (default: off).
2. The parent picks a child profile.
3. The parent sets the daily hard limit (e.g. 15-90 min in 5-min steps) and the break recommendation (e.g. every 15 min).
4. The parent enables or disables the risk mechanic (FR-PAR-003) - default is disabled.
5. The parent confirms the change.
6. The system persists the configuration and records it in an auditable way (FR-SAFE-005).
7. The system shows a confirmation and the next effective date (immediate).

## Alternative flows

- 3a Parent picks "no limit" (only in justified cases, e.g. school holidays): the system requires active confirmation.
- 4a Parent enables the risk mechanic: the system explicitly explains that this is the reversible mid-variant (shield + round pool).

## Exception flows

- 6x Persistence error: configuration stays unchanged; parent receives an error message; audit log.

## Postconditions

- Success: configuration for the child profile is immediately active; audit log contains before/after value.
- Failure: no partial change; the previous configuration remains valid.

## Business rules

- BR-001 The daily hard limit must be enforced server-side; once reached, the child session is terminated cleanly.
- BR-002 The risk mechanic is disabled by default and may be disabled at any time via self-service.
- BR-003 Even an active risk mechanic must not cause permanent loss (FR-GAM-005, FR-GAM-006).
- BR-004 Changes are auditable, including parent subject and timestamp.

## Acceptance criteria (BDD)

```gherkin
Feature: UC-009 Parent sets daily limit and risk mechanic

  Background:
    Given a verified parent account with at least one child profile

  Scenario: Daily limit takes effect immediately
    Given the child has already played 25 minutes today
    When the parent sets the daily limit to 20 minutes
    Then the system terminates the running child session cleanly
    And a new child session can no longer be started today

  Scenario: Risk mechanic is disabled by default
    Given a newly created child profile
    When the parent opens the play-time settings
    Then the risk mechanic is marked as disabled

  Scenario: Even an active risk mechanic causes no permanent loss
    Given an enabled risk mechanic
    When the child answers a task wrong
    Then star points and items remain unchanged or are restored within the same match

  Scenario: Change is recorded in an auditable way
    Given the parent changes the daily limit from 30 to 45 minutes
    Then the audit log contains an entry with before and after value as well as a timestamp
```
