# UC-026 - Admin configures simple event

| Field | Value |
| --- | --- |
| Use-Case ID | UC-026 |
| Title | Content manager configures a simple event |
| Release | R2 |
| Primary actor | Content Manager |
| Secondary actors | LiveOps Admin, Game & Worlds Service |
| Status | Specified |
| Goal | The content manager activates a time-bounded event (e.g., "Number Festival") with rules and rewards without code deployment. |
| Related requirements | FR-OPS-001, FR-OPS-002, NFR-OPS-001 |

## Preconditions

1. Content Manager role with permission for events.
2. Catalog of event templates exists.

## Trigger

The content manager opens "LiveOps > Events > New".

## Main flow

1. The system shows event templates with default rules.
2. The content manager picks a template and sets validity (start, end), eligible worlds, content domains and rewards.
3. The system validates the configuration (consistency, reward limits, FR-GAM-002).
4. The system schedules the event and announces it on the home screen with a friendly creature animation.
5. After expiry the event closes automatically.

## Alternative flows

- 3a Validation fails: the configuration is rejected; the content manager corrects.

## Exception flows

- 4x Scheduler error: event is not activated; alarm in monitoring (FR-OPS-004).

## Postconditions

- Success: event runs in the configured period.
- Failure: no event; consistent state.

## Business rules

- BR-001 Rewards may not be paid currency (FR-GAM-002).
- BR-002 Configurations are versioned (FR-OPS-002).

## Acceptance criteria (BDD)

```gherkin
Feature: UC-026 Configure a simple event

  Scenario: Successfully scheduled event
    Given an event template "Number Festival"
    When the content manager schedules the event for 7 days
    Then the event runs in the configured period
    And the home screen shows a friendly announcement

  Scenario: Invalid reward is rejected
    When the content manager picks paid currency as reward
    Then the system rejects the configuration
```
