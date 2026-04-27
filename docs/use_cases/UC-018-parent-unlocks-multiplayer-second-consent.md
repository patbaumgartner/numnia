# UC-018 - Parent unlocks multiplayer and communication via second consent

| Field | Value |
| --- | --- |
| Use-Case ID | UC-018 |
| Title | Parent unlocks multiplayer and communication via second consent |
| Release | R1 (consent capture); R2 (effective unlock with multiplayer) |
| Primary actor | Parent |
| Secondary actors | Email service, Identity & Consent Service |
| Status | Specified |
| Goal | The parent gives an explicit second consent that activates multiplayer and safe communication for the child profile. |
| Related requirements | FR-SAFE-006, FR-PAR-001, FR-PAR-003, NFR-SEC-001, NFR-SEC-003, NFR-PRIV-001 |

## Preconditions

1. UC-001 completed; parent account verified.
2. The child profile exists.

## Trigger

The parent opens "Parent area > Multiplayer & communication".

## Main flow

1. The system explains in plain language what is unlocked: turn-based multiplayer, predefined signals/emojis, no free text (FR-SAFE-001/002).
2. The parent confirms the activation and re-enters the password.
3. The system sends a confirmation email with a one-time link.
4. The parent opens the link; the system records the second consent and activates the flag on the child profile.
5. The change is auditable in the audit log (FR-SAFE-005).

## Alternative flows

- 2a The parent cancels: nothing changes.
- 4a The link is older than 24 h: the system invalidates it and offers a fresh email.

## Exception flows

- 3x Email cannot be delivered: retry with backoff; friendly notice; audit log.

## Postconditions

- Success: child profile has multiplayer/communication enabled; effective once R2 ships.
- Failure: flag remains disabled.

## Business rules

- BR-001 Multiplayer/communication is never enabled without a documented second consent (FR-SAFE-006).
- BR-002 The parent can withdraw the consent at any time; the flag is set back.

## Acceptance criteria (BDD)

```gherkin
Feature: UC-018 Parent unlocks multiplayer via second consent

  Scenario: Successful second consent
    Given a verified parent and a child profile
    When the parent confirms multiplayer
    And opens the link from the second consent email within 24 hours
    Then multiplayer is enabled on the child profile
    And the consent is documented in the audit log

  Scenario: Withdrawal disables multiplayer
    Given an enabled multiplayer flag on the child profile
    When the parent withdraws the consent
    Then multiplayer is disabled
    And the change is in the audit log
```
