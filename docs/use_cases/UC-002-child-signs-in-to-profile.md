# UC-002 - Child signs in to the child profile

| Field | Value |
| --- | --- |
| Use-Case ID | UC-002 |
| Title | Child signs in to the child profile |
| Release | R1 |
| Primary actor | Child (7-12) |
| Secondary actors | Parent (owns the account, picks the PIN) |
| Status | Specified |
| Goal | A child signs in to the previously created profile in a child-friendly way, without exposing sensitive data. |
| Related requirements | FR-PAR-001, FR-SAFE-003, NFR-SEC-001, NFR-SEC-003, NFR-UX-001, NFR-A11Y-005, NFR-I18N-002 |

## Preconditions

1. UC-001 is complete; a verified parent account with at least one ready-to-play child profile exists.
2. The parent has set a 4 to 6 digit PIN for the child profile.
3. The device meets the minimum requirements for the SPA and WebGL rendering.

## Trigger

The child opens the Numnia landing page and selects "Play".

## Main flow

1. The system shows a child-friendly selection of profiles linked to the device (avatar image plus fantasy name) - no sensitive data, no parent email.
2. The child picks an avatar/fantasy name.
3. The system asks for the PIN, with large keys and a labels suitable for read-aloud.
4. The child enters the PIN.
5. The system validates the PIN server-side against the child profile.
6. The system creates a child session with restricted rights (play, learn, gallery, avatar - no access to parent/school areas).
7. The system routes to the main menu (world, training mode, gallery, learning progress).

## Alternative flows

- 1a Only one child profile is registered: the system skips selection and shows the PIN input directly.
- 5a Wrong PIN: the system shows a friendly notice, the attempt is counted; after 5 failed attempts, the profile is temporarily locked and the parent is notified by email.

## Exception flows

- 5x Backend not reachable: the system shows a "no internet" notice and a retry button; no session is created.
- Xx Attempt to navigate to parent/school areas from the child session: the server denies with 403; audit-log entry.

## Postconditions

- Success: active child session with restricted rights; main menu reachable; security-relevant actions logged.
- Failure: no session; attempt counter updated; on lock, parent informed.

## Business rules

- BR-001 Children must not be able to sign in with the parent email or parent password.
- BR-002 Sessions are restricted server-side to child rights (least privilege).
- BR-003 PIN entry must be operable via keyboard and touch and screen-reader compatible (parent/teacher areas separate).
- BR-004 After 5 failed attempts, the system locks the child profile until the parent releases it.

## Acceptance criteria (BDD)

```gherkin
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
```
