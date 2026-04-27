Feature: UC-010 Parent exports child data

  Background:
    Given a verified parent account with a child profile

  Scenario: Complete export in JSON format
    Given the child has learning history, inventory and star point movements
    When the parent requests a JSON export
    Then the JSON file contains profile, learning history, mastery status, inventory, star point movements and consent history

  Scenario: Download link expires after deadline
    Given a generated export with a signed URL and 7-day deadline
    When eight days pass without download
    Then the link is no longer usable
    And the audit log contains an entry "Export file expired"

  Scenario: Audit log documents trigger and download
    Given the parent triggers a PDF export
    And downloads the file once
    Then the audit log contains at least two entries with timestamp and parent subject
