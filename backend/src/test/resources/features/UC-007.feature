Feature: UC-007 Child customizes avatar and buys items

  Background:
    Given an active child session
    And a configured shop catalog

  Scenario: Successful purchase with star points
    Given the child has 50 star points
    And the item "Star Cap" costs 30 star points
    When the child confirms the purchase
    Then the star points balance is reduced to 20
    And the Star Cap is permanently in the inventory

  Scenario: Purchase with insufficient star points is prevented
    Given the child has 10 star points
    And the item costs 30 star points
    When the child tries the purchase
    Then the system shows a notice about collecting more star points
    And no booking takes place

  Scenario: Inventory manipulation via API is rejected
    Given an active child session
    When the client tries to unlock an item without payment
    Then the server responds with an error status
    And the incident is documented in the shop audit log
