# UC-048 - Backup and restore drill

| Field | Value |
| --- | --- |
| Use-Case ID | UC-048 |
| Title | Monthly backup and restore drill |
| Release | R5 |
| Primary actor | Operator |
| Secondary actors | Monitoring Service |
| Status | Specified |
| Goal | The operator performs a restore at least monthly to verify backup integrity. |
| Related requirements | NFR-OPS-002, NFR-OPS-003 |

## Preconditions

1. Daily backups are running.

## Trigger

Calendar trigger (1st of every month).

## Main flow

1. The operator picks a backup file.
2. The system runs a restore into a sandbox environment.
3. Integrity checks are executed.
4. The result is logged.

## Alternative flows

- 3a Restore fails: incident is opened; root cause analysis.

## Exception flows

- 1x No usable backup: critical alarm; emergency procedure.

## Postconditions

- Success: restore documented and successful.
- Failure: incident open; alarm.

## Business rules

- BR-001 Backups remain in CH (NFR-OPS-003).

## Acceptance criteria (BDD)

```gherkin
Feature: UC-048 Backup restore drill

  Scenario: Successful restore
    Given a daily backup
    When the operator runs the monthly restore
    Then the restore is successful
    And the result is in the operations log

  Scenario: Failed restore opens incident
    Given a corrupted backup
    When the restore fails
    Then an incident is opened
```
