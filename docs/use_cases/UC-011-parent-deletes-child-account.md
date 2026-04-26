# UC-011 - Parent deletes child account

| Field | Value |
| --- | --- |
| Use-Case ID | UC-011 |
| Title | Parent deletes a child account via self-service |
| Release | R1 |
| Primary actor | Parent |
| Secondary actors | Parent Self-Service, Audit, Backup System |
| Status | Specified |
| Goal | Parents delete a child profile in an FADP/GDPR-compliant way, including all personal data, and receive a deletion record. |
| Related requirements | FR-PAR-005, NFR-PRIV-002, NFR-SEC-003, FR-SAFE-005, NFR-OPS-002 |

## Preconditions

1. Verified parent account (UC-001).
2. At least one child profile is linked to the account.
3. The system has informed the parent about the option of a prior export (UC-010).

## Trigger

The parent opens the parent self-service and selects "Delete child profile".

## Main flow

1. The system shows the chosen child profile with a notice about consequences (irreversible deletion, recommendation to export beforehand).
2. The parent confirms with password and explicit entry of the word "DELETE".
3. The system sends a confirmation email with a confirmation link (cool-off protection: 24 h deadline).
4. The parent opens the link.
5. The system marks the child profile as "in deletion" and locks any access.
6. The system deletes all personal data of the child profile, anonymizes remaining statistical aggregates and removes the profile from active backups within the backup rotation (NFR-OPS-002).
7. The system writes a deletion record (audit log and confirmation email to the parent) with date, subject and affected data categories.
8. The system fully deletes the child profile from the live database once the step is complete; confirmation in the UI.

## Alternative flows

- 4a Confirmation link not opened within 24 h: the deletion is discarded, the profile remains active.
- The parent cancels the process in the UI: the process is discarded.
- 6a Deletion from backups is delayed due to the rotation schedule: the system documents the expected date and communicates it to the parent.

## Exception flows

- 2x Wrong password: the system declines; after 5 attempts, temporary lock.
- 6x Persistence error during deletion: the process is rolled back cleanly, the parent receives an error message; the audit log documents the incident.

## Postconditions

- Success: child profile and all personal data deleted; deletion record issued; backups are cleansed within the rotation window.
- Failure: consistent state; no partially deleted data.

## Business rules

- BR-001 Deletion requires the parent password, explicit entry and cool-off confirmation by email.
- BR-002 The deletion record is mandatory and contains date, subject and data categories.
- BR-003 Statistical remainders must be anonymized and must not be traceable to the deleted child.
- BR-004 Backups must be cleansed within the rotation deadline.

## Acceptance criteria (BDD)

```gherkin
Feature: UC-011 Parent deletes a child account

  Background:
    Given a verified parent account with a child profile

  Scenario: Successful deletion with cool-off confirmation
    Given the parent confirms with password and the word "DELETE"
    When the parent opens the link from the confirmation email within 24 hours
    Then all personal data of the child profile is deleted
    And the parent receives a deletion record with date and data categories

  Scenario: Confirmation link expires
    Given a triggered deletion process
    When the parent does not open the confirmation link within 24 hours
    Then the child profile remains active
    And the deletion process is marked as "discarded" in the audit log

  Scenario: Backups are cleansed within the rotation window
    Given a completed deletion process
    When the next backup rotation runs
    Then the active backups no longer contain personal data of the deleted child profile
```
