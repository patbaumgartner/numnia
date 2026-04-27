# UC-038 - Content manager configures task pool

| Field | Value |
| --- | --- |
| Use-Case ID | UC-038 |
| Title | Content manager configures and versions task pools |
| Release | R4 |
| Primary actor | Content Manager |
| Secondary actors | Adaptive Engine |
| Status | Specified |
| Goal | The content manager creates, edits, versions and publishes task pools without code deployment. |
| Related requirements | FR-LEARN-010, FR-OPS-002, NFR-OPS-001 |

## Preconditions

1. Content Manager role.
2. Editor for task pools.

## Trigger

"LiveOps > Task pools > New" or "Edit".

## Main flow

1. The system shows existing pools per world, operation and number range.
2. The content manager creates or edits a pool (task types, range, weights).
3. The system validates rules (consistency, mastery thresholds).
4. The content manager publishes the new version; the previous version remains stored.
5. The adaptive engine immediately uses the new version for new sessions.

## Alternative flows

- 4a Rollback: the content manager activates a previous version.

## Exception flows

- 3x Validation fails: publication is rejected.

## Postconditions

- Success: new pool active; old version archived.
- Failure: previous active pool preserved.

## Business rules

- BR-001 Pools are versioned and traceable (FR-OPS-002).

## Acceptance criteria (BDD)

```gherkin
Feature: UC-038 Configure task pool

  Scenario: Publishing a new pool version
    Given an existing task pool v1
    When the content manager publishes version v2
    Then v2 is active
    And v1 remains archived

  Scenario: Rollback to previous version
    Given two pool versions v1 and v2 with v2 active
    When the content manager activates v1
    Then v1 is active
```
