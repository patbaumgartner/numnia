# UC-034 - Teacher exports learning-status report

| Field | Value |
| --- | --- |
| Use-Case ID | UC-034 |
| Title | Teacher exports class learning-status report |
| Release | R3 |
| Primary actor | Teacher |
| Secondary actors | Reporting Service |
| Status | Specified |
| Goal | The teacher exports a learning-status report for the class as PDF or CSV. |
| Related requirements | FR-SCH-005, NFR-PRIV-001, NFR-OPS-003 |

## Preconditions

1. Class with at least one child.

## Trigger

The teacher opens "Class > Reports > Export".

## Main flow

1. The teacher picks the period and format (PDF/CSV).
2. The system generates the report, restricted to fantasy names and aggregated metrics.
3. The system delivers the report; sensitive raw data are not included.
4. The export is recorded in the audit log.

## Alternative flows

- 1a Selected period is empty: the system shows a friendly notice and does not export.

## Exception flows

- 2x Generation fails: friendly notice; audit log.

## Postconditions

- Success: report delivered.
- Failure: no inconsistent state.

## Business rules

- BR-001 Reports never contain real names of children.
- BR-002 Storage of generated reports stays in CH (NFR-OPS-003).

## Acceptance criteria (BDD)

```gherkin
Feature: UC-034 Class report

  Scenario: PDF export with fantasy names
    Given a class with progress data
    When the teacher exports a PDF report
    Then the report contains only fantasy names and aggregated metrics

  Scenario: CSV export with date range
    When the teacher exports a CSV report for the last 30 days
    Then the file contains the corresponding data
```
