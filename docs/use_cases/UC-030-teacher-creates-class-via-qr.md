# UC-030 - Teacher creates class via QR or code

| Field | Value |
| --- | --- |
| Use-Case ID | UC-030 |
| Title | Teacher creates class and onboards children via QR/code |
| Release | R3 |
| Primary actor | Teacher |
| Secondary actors | Parent, Child |
| Status | Specified |
| Goal | A teacher creates a class and lets children join via QR/code without using personal data. |
| Related requirements | FR-SCH-001, FR-SAFE-003, NFR-PRIV-001 |

## Preconditions

1. Teacher account active (UC-029).

## Trigger

The teacher opens "Classes > New".

## Main flow

1. The teacher enters a class name (e.g., "3a") and the school year.
2. The system creates the class and generates a QR code and a join code.
3. The teacher prints/projects the code.
4. Children join via QR/code in the parent area; the parent confirms the join (FR-PAR-001).
5. The class is visible in the teacher dashboard.

## Alternative flows

- 4a No parental confirmation: the join stays pending.

## Exception flows

- 2x QR generation fails: the system retries; friendly notice.

## Postconditions

- Success: class active; children members.
- Failure: no inconsistent class state.

## Business rules

- BR-001 Children appear with fantasy names only (FR-SAFE-003).
- BR-002 Joining requires parental consent.

## Acceptance criteria (BDD)

```gherkin
Feature: UC-030 Teacher creates class

  Scenario: Class with QR code
    Given an active teacher
    When the teacher creates a class
    Then a QR code and a join code are generated

  Scenario: Child joins after parental consent
    Given a class with a join code
    When the parent confirms the join
    Then the child is a member of the class
```
