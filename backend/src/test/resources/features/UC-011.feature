Feature: UC-011 Parent deletes a child account

  Background:
    Given a verified parent account with a child profile

  Scenario: Successful deletion with cool-off confirmation
    Given the parent confirms with password and the word "DELETE"
    When the parent opens the link from the confirmation email within 24 hours
    Then all personal data of the child profile is deleted
    And the parent receives a deletion record with date and data categories

  Scenario: Confirmation link expires
    Given a triggered deletion process
    When the parent does not open the confirmation link within 24 hours
    Then the child profile remains active
    And the deletion process is marked as "discarded" in the audit log

  Scenario: Backups are cleansed within the rotation window
    Given a completed deletion process
    When the next backup rotation runs
    Then the active backups no longer contain personal data of the deleted child profile
