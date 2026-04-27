Feature: UC-005 Child enters a world through a portal

  Background:
    Given an active child session
    And three worlds are unlocked in Release 1

  Scenario: Training portal opens when rules are satisfied
    Given the child has reached level S2
    And the task pool of the world "Mushroom Jungle" is configured
    When the child enters the training portal of Mushroom Jungle
    Then the system switches to the practice stage of the world

  Scenario: Reduced-motion reduces animations
    Given the child has reduced-motion enabled
    When it enters a world
    Then particle effects and intense animations are reduced

  Scenario: Locked portal stays closed
    Given a portal of type "Duel"
    When the child taps on it in Release 1
    Then the system shows the notice "coming later"
    And the portal stays closed
