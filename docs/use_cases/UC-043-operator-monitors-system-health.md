# UC-043 - Operator monitors system health

| Field | Value |
| --- | --- |
| Use-Case ID | UC-043 |
| Title | Operator monitors system health and operational metrics |
| Release | R4 |
| Primary actor | System Admin / Operator |
| Secondary actors | Monitoring Service |
| Status | Specified |
| Goal | The operator sees in real time the system status, health and central operational metrics and acts on alarms. |
| Related requirements | FR-OPS-004, NFR-OPS-001, NFR-OPS-002, NFR-PERF-003, NFR-PERF-004 |

## Preconditions

1. Monitoring is configured.

## Trigger

The operator opens "Operations > Health".

## Main flow

1. The system shows status of services, latency p50/p95, error rate, queue depth and backup status.
2. Alarms (e.g., p95 above target) are highlighted.
3. The operator opens an alarm; the system shows context and runbooks.
4. After resolution the operator closes the alarm.

## Alternative flows

- 2a Several alarms: prioritization by impact.

## Exception flows

- 1x Monitoring is unavailable: secondary channel (email/SMS) takes over.

## Postconditions

- Success: alarms processed; health visible.
- Failure: alarm stays open until resolution.

## Business rules

- BR-001 Backups must succeed daily; restore is testable (NFR-OPS-002).

## Acceptance criteria (BDD)

```gherkin
Feature: UC-043 Operator monitoring

  Scenario: Active alarm
    Given an alarm "p95 above target"
    When the operator opens the alarm
    Then the system shows context and runbook

  Scenario: Backup status visible
    When the operator opens the health page
    Then the daily backup status is visible
```
