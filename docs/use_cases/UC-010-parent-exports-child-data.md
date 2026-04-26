# UC-010 - Parent exports child data

| Field | Value |
| --- | --- |
| Use-Case ID | UC-010 |
| Title | Parent exports child data as JSON and PDF |
| Release | R1 |
| Primary actor | Parent |
| Secondary actors | Parent Self-Service, Audit, Asset Storage (signed URL) |
| Status | Specified |
| Goal | Parents receive a complete, FADP/GDPR-compliant export of their child's data in JSON and PDF. |
| Related requirements | FR-PAR-004, NFR-PRIV-002, NFR-SEC-001, NFR-SEC-003, FR-SAFE-005 |

## Preconditions

1. Verified parent account (UC-001).
2. At least one child profile is linked to the account.

## Trigger

The parent opens the parent self-service and selects "Export data".

## Main flow

1. The system shows an overview of the linked child profiles.
2. The parent picks a child profile and the format (JSON, PDF or both).
3. The parent confirms the request.
4. The system asynchronously generates a complete export (profile master data, learning history, mastery status, inventory, star point movements, consent history, audit-log summary).
5. The system writes an audit-log entry "Export triggered" (FR-SAFE-005).
6. The system stores the file in a secured area and generates a signed, time-limited download URL (e.g. 7 days).
7. The system informs the parent by email and in the UI; the parent downloads the file.
8. The system writes an audit-log entry "Export downloaded" on the first successful download.

## Alternative flows

- 2a Parent picks "All child profiles": the system generates a combined export with clear separation per child.
- 7a Parent does not download the export within the deadline: the system deletes the file and writes an audit-log entry "Export file expired".

## Exception flows

- 4x Generation fails: the system informs the parent and allows a new request.
- 6x Signed URL compromised (suspicion): the system invalidates the link and notifies the parent.

## Postconditions

- Success: complete export available; audit log contains "trigger" and possibly "download" or "expiry".
- Failure: no partially available exports; parents receive a clear error message.

## Business rules

- BR-001 The export must contain all data stored for the child profile (completeness principle).
- BR-002 Export files are made available exclusively via signed HTTPS URLs in CH hosting.
- BR-003 Every export action is recorded in an auditable way (trigger, download, expiry).
- BR-004 Exports that are not downloaded are deleted after the deadline expires.

## Acceptance criteria (BDD)

```gherkin
Feature: UC-010 Parent exports child data

  Background:
    Given a verified parent account with a child profile

  Scenario: Complete export in JSON format
    Given the child has learning history, inventory and star point movements
    When the parent requests a JSON export
    Then the JSON file contains profile, learning history, mastery status, inventory, star point movements and consent history

  Scenario: Download link expires after deadline
    Given a generated export with a signed URL and 7-day deadline
    When eight days pass without download
    Then the link is no longer usable
    And the audit log contains an entry "Export file expired"

  Scenario: Audit log documents trigger and download
    Given the parent triggers a PDF export
    And downloads the file once
    Then the audit log contains at least two entries with timestamp and parent subject
```
