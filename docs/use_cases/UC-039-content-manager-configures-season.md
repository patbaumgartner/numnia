# UC-039 - Content manager configures season

| Field | Value |
| --- | --- |
| Use-Case ID | UC-039 |
| Title | Content manager configures a season with rewards |
| Release | R4 |
| Primary actor | Content Manager |
| Secondary actors | Game & Worlds Service |
| Status | Specified |
| Goal | The content manager activates a multi-week season with friendly themes, missions and seasonal rewards. |
| Related requirements | FR-OPS-001, FR-OPS-002, FR-GAM-002 |

## Preconditions

1. Season templates with friendly creature/world themes are available.

## Trigger

"LiveOps > Seasons > New".

## Main flow

1. The content manager picks a season template (e.g., "Stargazer Spring").
2. The system shows the season missions and reward catalog.
3. The content manager configures duration, eligible worlds and reward catalog.
4. The system schedules the season; on start the home screen shows a friendly creature animation.
5. After expiry the season closes; rewards earned remain.

## Alternative flows

- 3a Reward configuration violates FR-GAM-002 (paid currency): rejection.

## Exception flows

- 4x Scheduler error: season not activated; alarm.

## Postconditions

- Success: season active; rewards available.
- Failure: no season; consistent state.

## Business rules

- BR-001 Rewards are non-purchasable.
- BR-002 Visual themes follow the friendly, light design system.

## Acceptance criteria (BDD)

```gherkin
Feature: UC-039 Season configuration

  Scenario: Successfully scheduled season
    Given a season template "Stargazer Spring"
    When the content manager schedules the season for 6 weeks
    Then the season runs in the configured period
```
