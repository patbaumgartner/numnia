# UC-001 - Set up parent account and child profile with double opt-in

| Field | Value |
| --- | --- |
| Use-Case ID | UC-001 |
| Title | Set up parent account and child profile with double opt-in |
| Release | R1 |
| Primary actor | Parent |
| Secondary actors | Email service (sending verification emails), child (passive, receives the profile) |
| Status | Specified |
| Goal | Parents create an account, set up a child profile and unlock sensitive functions through a two-step consent. |
| Related requirements | FR-SAFE-006, FR-SAFE-003, FR-PAR-001, FR-CRE-005, NFR-SEC-001, NFR-SEC-002, NFR-SEC-003, NFR-PRIV-001, NFR-I18N-001, NFR-I18N-002 |

## Preconditions

1. The application is reachable and served over HTTPS.
2. The parent owns a valid email address.
3. A vetted list of fantasy names exists for child-profile selection (FR-SAFE-003).
4. A catalog of gender-neutral avatar base models exists (FR-CRE-005).

## Trigger

The parent opens the Numnia landing page and selects "Create account".

## Main flow

1. The system shows the parent registration form in Swiss High German (with umlauts, without sharp s).
2. The parent enters first name, salutation, email address and password and accepts the privacy and terms of use.
3. The system validates the inputs server-side (mandatory fields, email format, minimum password strength) and creates the parent account in status "not verified".
4. The system sends verification email #1 to the provided email address.
5. The parent opens the link from email #1; the system marks the email address as verified.
6. The system routes the parent to the "Create child profile" area.
7. The parent picks a fantasy name from the predefined list, a year of birth (range 7-12 years) and a gender-neutral avatar base model.
8. The system creates the child profile under a pseudonym and links it to the parent account.
9. The system explains the second confirmation step for sensitive functions (multiplayer, communication) and sends confirmation email #2.
10. The parent opens the link from email #2; the system records the second consent.
11. The system marks the child profile as ready to play; multiplayer and communication remain disabled in Release 1 (out of scope), but the flag is persisted.
12. The system writes the complete process to the audit log and shows the parent a confirmation page with the next step (sign the child in).

## Alternative flows

- 3a Validation fails: the system shows field-level error messages, no account is created.
- 3b Email already registered: the system shows a "Account exists" notice with a link to sign-in; no new account.
- 5a Link from email #1 expired (>24 h): the system offers "request new verification email".
- 7a Parent picks a year of birth outside 7-12: the system shows a notice "Numnia is designed for ages 7-12" and blocks creation.
- 10a Link from email #2 expired: account and child profile remain in place, sensitive functions stay locked; the parent can request a new email.

## Exception flows

- 4x Email delivery fails: the system retries with backoff; on permanent failure, notice to the parent and a log entry.
- 8x Persistence error on the child profile: the transaction is rolled back, the parent receives an error message, no orphaned profile.
- Xx Brute-force attempts on verification links: rate limiting kicks in (NFR-SEC-004), the audit log flags the incident.

## Postconditions

- Success: parent account verified, child profile present under a pseudonym, double opt-in documented, audit logs complete.
- Failure: no partially visible accounts or child profiles; data is either not persisted or rolled back consistently.

## Business rules

- BR-001 Parents must confirm the email address through two separate emails (double opt-in) before sensitive functions can be unlocked.
- BR-002 Child profiles may only carry fantasy names from the vetted list.
- BR-003 Avatars are based on gender-neutral base models.
- BR-004 Only data necessary for sign-in, consent and learning operations is captured (data minimization).
- BR-005 Security-relevant steps (creation, verification, consent) are recorded in an auditable way.

## Acceptance criteria (BDD)

```gherkin
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
```
