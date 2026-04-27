# UC-033 - Child completes homework assignment

| Field | Value |
| --- | --- |
| Use-Case ID | UC-033 |
| Title | Child completes homework assignment |
| Release | R3 |
| Primary actor | Child (7-12) |
| Secondary actors | Teacher |
| Status | Specified |
| Goal | The child works on homework with a due date assigned by the teacher. |
| Related requirements | FR-SCH-004, FR-LEARN-009 |

## Preconditions

1. Class exists; teacher has assigned homework.
2. Active child session.

## Trigger

The child opens "Class > Homework".

## Main flow

1. The system shows open homework with due date and content domain.
2. The child plays the assigned tasks; result is recorded.
3. After completion the system marks the homework as done and informs the teacher in the dashboard.
4. After the due date the homework is automatically locked.

## Alternative flows

- 4a Late submission with teacher tolerance: the teacher can extend the due date individually.

## Exception flows

- 3x Persistence error: the partial result is retained; on next sign-in the child can complete the homework.

## Postconditions

- Success: homework status updated.
- Failure: status unchanged.

## Business rules

- BR-001 Homework follows mastery rules (FR-LEARN-009).

## Acceptance criteria (BDD)

```gherkin
Feature: UC-033 Homework

  Scenario: Homework completed on time
    Given an open homework with a due date in 3 days
    When the child completes the tasks
    Then the homework is marked as done
    And the teacher sees it in the dashboard

  Scenario: Late submission blocked
    Given a homework whose due date has passed
    When the child tries to play the tasks
    Then the homework is locked
```
