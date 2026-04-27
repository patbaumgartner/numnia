# UC-001 — Set up parent account and child profile with double opt-in
# Verbatim Gherkin from docs/use_cases/UC-001-set-up-parent-account-and-child-profile.md
# Refs: FR-SAFE-006, FR-SAFE-003, FR-PAR-001, FR-CRE-005,
#       NFR-SEC-001, NFR-SEC-002, NFR-SEC-003, NFR-PRIV-001,
#       NFR-I18N-001, NFR-I18N-002

Feature: UC-001 Set up parent account and child profile with double opt-in

  Background:
    Given the Numnia landing page is reachable over HTTPS
    And the language is Swiss High German without sharp s

  Scenario: Successful registration with double opt-in
    Given a new parent with a valid email address
    When the parent fully completes the registration form
    And confirms the link from the first verification email
    And creates a child profile with a fantasy name, year of birth 9 and avatar base model
    And confirms the link from the second confirmation email
    Then the parent account is verified
    And the child profile exists under a pseudonym
    And the two-step consent is documented in the audit log

  Scenario: Year of birth outside the target group is rejected
    Given a verified parent in the "Create child profile" step
    When the parent picks a year of birth corresponding to an age below 7
    Then the system shows a notice about the 7-12 target group
    And no child profile is created

  Scenario: First verification email expired
    Given a registered but unverified parent account
    And the verification link is older than 24 hours
    When the parent opens the link
    Then the system offers "request new verification email"
    And the account remains unverified

  Scenario: Duplicate registration is prevented
    Given an already registered email address
    When another registration with the same address is attempted
    Then the system shows a notice about the existing account
    And no new account is created
