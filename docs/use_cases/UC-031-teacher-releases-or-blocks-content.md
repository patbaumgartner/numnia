# UC-031 - Teacher releases or blocks tasks and worlds

| Field | Value |
| --- | --- |
| Use-Case ID | UC-031 |
| Title | Teacher releases or blocks tasks and worlds for the class |
| Release | R3 |
| Primary actor | Teacher |
| Secondary actors | Children of the class, Game & Worlds Service |
| Status | Specified |
| Goal | A teacher controls which task pools and worlds are visible to the class during teaching mode. |
| Related requirements | FR-SCH-002, FR-OPS-002 |

## Preconditions

1. Class exists (UC-030).

## Trigger

The teacher opens "Class > Content".

## Main flow

1. The system shows worlds and task pools with a release toggle.
2. The teacher releases or blocks individual content items.
3. The system applies the changes immediately to the class.
4. Children see only the released content; blocked content carries a friendly "Not enabled today" notice.

## Alternative flows

- 2a The teacher restores the default: all class-internal blocks are removed.

## Exception flows

- 3x Persistence error: previous state remains; friendly notice; audit log.

## Postconditions

- Success: changes effective and persisted.
- Failure: previous state preserved.

## Business rules

- BR-001 Releases are class-bound; outside teaching mode the standard rules apply.

## Acceptance criteria (BDD)

```gherkin
Feature: UC-031 Teacher releases or blocks content

  Scenario: World blocked for the class
    Given a class with the world "Mushroom Jungle" released
    When the teacher blocks the world
    Then the world is not visible to the children of the class
    And a friendly notice is shown
```
