# UC-054 - User submits FADP/GDPR data subject request

| Field | Value |
| --- | --- |
| Use-Case ID | UC-054 |
| Title | User submits FADP/GDPR data subject request (access, rectification, deletion) |
| Release | R1 |
| Primary actor | Parent (on behalf of child) or registered adult user |
| Secondary actors | Data Protection Officer, Email service |
| Status | Specified |
| Goal | A user submits a formal data subject request and receives a complete, lawful answer within statutory time limits. |
| Related requirements | NFR-PRIV-001, NFR-PRIV-002, FR-PAR-004, FR-PAR-005, FR-SAFE-005, NFR-I18N-001 |

## Preconditions

1. The user has a verified account (UC-001).

## Trigger

The user opens "Privacy > My data" or sends an email to the published DPO contact.

## Main flow

1. The system shows the form with selectable request type: information, correction, deletion, restriction, data portability, complaint.
2. The user submits the request; the system creates a ticket with deadline (max. 30 days, in line with FADP/GDPR).
3. The DPO processes the request and prepares the response.
4. For "information" the user receives a structured export (UC-010 reused for child data).
5. For "deletion" UC-011 is reused for child accounts.
6. The full process is recorded in the audit log (FR-SAFE-005).

## Alternative flows

- 3a Identity in doubt: the system asks for an additional proof (no over-collection, NFR-PRIV-001).
- 3b Statutory exception (e.g., legal retention obligation): the partial answer explains the reason.

## Exception flows

- 2x Persistence error: friendly notice; ticket is queued.

## Postconditions

- Success: response delivered within deadline; result documented.
- Failure: request remains open; deadline tracked; alarm.

## Business rules

- BR-001 Statutory deadlines are tracked.
- BR-002 Responses contain only data of the requesting party.
- BR-003 Form is available in DE/EN (no sharp s in DE).

## Acceptance criteria (BDD)

```gherkin
Feature: UC-054 Data subject request

  Scenario: Information request answered within deadline
    Given a verified user
    When the user submits an information request
    Then the system creates a ticket with a 30-day deadline
    And the response is delivered within the deadline

  Scenario: Deletion request triggers child account deletion
    Given a parent with a child account
    When the parent submits a deletion request
    Then UC-011 is triggered
    And the deletion is documented in the audit log
```
