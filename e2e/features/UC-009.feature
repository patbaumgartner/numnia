Feature: UC-009 Parent sets daily limit and risk mechanic

  Background:
    Given a verified parent account with at least one child profile

  Scenario: Daily limit takes effect immediately
    Given the child has already played 25 minutes today
    When the parent sets the daily limit to 20 minutes
    Then the system terminates the running child session cleanly
    And a new child session can no longer be started today

  Scenario: Risk mechanic is disabled by default
    Given a newly created child profile
    When the parent opens the play-time settings
    Then the risk mechanic is marked as disabled

  Scenario: Even an active risk mechanic causes no permanent loss
    Given an enabled risk mechanic
    When the child answers a task wrong
    Then star points and items remain unchanged or are restored within the same match

  Scenario: Change is recorded in an auditable way
    Given the parent changes the daily limit from 30 to 45 minutes
    Then the audit log contains an entry with before and after value as well as a timestamp
