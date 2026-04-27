# UC-002 — Child signs in to the child profile
# Verbatim Gherkin from docs/use_cases/UC-002-child-signs-in-to-profile.md
# Refs: FR-PAR-001, FR-SAFE-003, NFR-SEC-001, NFR-SEC-002, NFR-SEC-003, NFR-SEC-004,
#       NFR-UX-001, NFR-A11Y-005, NFR-I18N-002, NFR-ENG-002, NFR-ENG-003

Feature: UC-002 Child signs in to the child profile

  Background:
    Given a verified parent account with a ready-to-play child profile
    And a PIN set by the parent

  Scenario: Successful sign-in to the own profile
    Given the child opens the landing page
    When it picks its avatar and enters the correct PIN
    Then the system creates a child session with restricted rights
    And the main menu is visible

  Scenario: Profile is locked after five failed attempts
    Given a child profile with a valid PIN
    When a wrong PIN is entered five times in a row
    Then the child profile is locked until the parent releases it
    And the parent receives a notification email

  Scenario: Child session must not call a parent endpoint
    Given an active child session
    When an attempt is made to call a parent endpoint
    Then the server responds with status 403
    And the incident is documented in the audit log
