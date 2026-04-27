Feature: UC-006 Child unlocks creature and picks companion

  Background:
    Given an active child session
    And a configured unlock threshold for the creature "Pilzar"

  Scenario: Successful unlock via mastery
    Given the child reaches mastery in the related domain
    When the system processes the unlock
    Then the creature appears permanently in the gallery
    And it can be picked as companion

  Scenario: Variable name endings are accepted
    Given the candidate names "Pilzar", "Welleno", "Zacka"
    When the system validates the names
    Then all three are accepted
    And the system rejects no name due to a missing "i" ending

  Scenario: Picking a non-unlocked creature as companion is rejected
    Given a creature that is not yet unlocked
    When the child tries to pick it as companion
    Then the server responds with status 409
    And the previous companion stays active
