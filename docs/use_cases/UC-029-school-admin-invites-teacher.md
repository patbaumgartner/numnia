# UC-029 - School admin invites teacher

| Field | Value |
| --- | --- |
| Use-Case ID | UC-029 |
| Title | School admin invites teacher |
| Release | R3 |
| Primary actor | School Admin |
| Secondary actors | Teacher, Email service |
| Status | Specified |
| Goal | A school admin onboards a teacher exclusively by invitation. |
| Related requirements | FR-SCH-007, NFR-SEC-003, NFR-PRIV-001 |

## Preconditions

1. School-admin account exists.
2. Email of the prospective teacher is known.

## Trigger

The school admin opens "School > Teachers > Invite".

## Main flow

1. The school admin enters first name, last name and email of the teacher and assigns role "Teacher".
2. The system sends a one-time invitation link with a 7-day validity.
3. The teacher opens the link and sets a password.
4. The system activates the teacher account and connects it to the school.
5. The action is recorded in the audit log (FR-SAFE-005).

## Alternative flows

- 3a Link expired: a new invitation can be triggered.

## Exception flows

- 2x Email cannot be delivered: friendly notice; audit log.

## Postconditions

- Success: teacher account active and linked to the school.
- Failure: no orphan teacher account.

## Business rules

- BR-001 Teachers can never self-register (FR-SCH-007).
- BR-002 Invitation links are one-time only and expire after 7 days.

## Acceptance criteria (BDD)

```gherkin
Feature: UC-029 School admin invites teacher

  Scenario: Successful invitation
    Given a school admin
    When the admin invites a teacher
    Then the teacher receives an invitation email
    And the teacher account is active after setting a password

  Scenario: Self-registration is blocked
    When a teacher tries to self-register
    Then the system blocks the registration
```
