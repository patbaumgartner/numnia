Feature: UC-008 Child views own learning progress

  Background:
    Given an active child session

  Scenario: Progress per operation is visible
    Given at least three completed training sessions
    When the child opens "My progress"
    Then it sees a separate progress bar per operation
    And the mastery status per content domain is marked

  Scenario: No comparative leaderboards
    Given the progress view is open
    Then the view contains no leaderboard with other children

  Scenario: Color-blind profile is respected
    Given the child has the deuteranopia profile enabled
    When it opens the progress view
    Then the visualization uses the corresponding color palette
