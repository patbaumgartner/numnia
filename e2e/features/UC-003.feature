Feature: UC-003 Child starts training mode for chosen operation

  Background:
    Given an active child session
    And a configured task pool for the chosen world

  Scenario: Adaptive speed downgrade after three errors
    Given the child practices multiplication on S3/G3
    When it answers three tasks in a row wrong or by time-out
    Then the adaptive engine sets the speed to G2
    And proposes accuracy or explanation mode

  Scenario: Tasks stay within the number range up to 1,000,000
    Given the child practices addition on S6
    When the task generator creates a new task
    Then the expected result lies between 0 and 1,000,000

  Scenario: Mastery is granted only after consolidation
    Given the child meets the accuracy and speed thresholds for S2 today
    And only one session on one calendar day exists so far
    When the session ends
    Then the mastery status for S2 remains "in consolidation"
    And mastery is confirmed only after a second session on another calendar day

  Scenario: Error costs no star points
    Given the child has 12 star points
    When it answers a task wrong
    Then the star points balance stays at 12
