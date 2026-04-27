@uc-004
Feature: UC-004 Child practices in accuracy mode

  Background:
    Given an active child session

  Scenario: Accuracy mode runs without a timer
    Given the child starts accuracy mode for subtraction
    When a task is shown
    Then no time limit is active
    And no timer is visible in the UI

  Scenario: Explanation mode is reachable from accuracy mode
    Given a task is shown in accuracy mode
    When the child selects "Show explanation"
    Then animated solution steps are played
    And the task remains workable

  Scenario: No star point loss on error
    Given a child with 8 star points
    When it answers a task wrong in accuracy mode
    Then the star points balance stays at 8
