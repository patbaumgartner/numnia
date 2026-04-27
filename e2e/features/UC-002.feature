# UC-002 — Child signs in to the child profile
# E2E BDD scenarios exercising the full browser + backend stack.
# Refs: FR-006, FR-007, FR-008, BR-001, BR-003, BR-004,
#       NFR-SEC-001, NFR-SEC-002, NFR-SEC-003, NFR-I18N-002, NFR-I18N-004

Feature: UC-002 Child signs in to the child profile

  Background:
    Given a verified parent account with a ready-to-play child profile
    And a PIN set by the parent

  Scenario: Successful child sign-in with correct PIN
    When the child navigates to the sign-in page and enters the correct PIN
    Then the child is signed in and sees the child area

  Scenario: Profile is locked after five failed attempts
    When a wrong PIN is entered five times in a row
    Then the child profile is locked
    And the locked screen is shown

  Scenario: Child session must not call a parent endpoint
    Given an active child session stored in the browser
    When the child navigates to the parent dashboard URL
    Then the child area is shown instead
