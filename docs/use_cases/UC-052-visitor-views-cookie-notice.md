# UC-052 - Visitor views Cookie Notice and consent

| Field | Value |
| --- | --- |
| Use-Case ID | UC-052 |
| Title | Visitor views Cookie Notice and gives or withdraws consent (DE/EN) |
| Release | R1 |
| Primary actor | Visitor (any user) |
| Secondary actors | Identity & Consent Service |
| Status | Specified |
| Goal | Visitors are informed about cookies/local storage and can give or revoke consent in line with FADP/GDPR. |
| Related requirements | NFR-PRIV-001, NFR-PRIV-002, NFR-SEC-001, NFR-I18N-001, NFR-I18N-002 |

## Preconditions

1. Numnia uses only technically necessary cookies/local storage in Release 1; no advertising trackers (SRS §10.2).

## Trigger

First visit, or visitor opens "Cookies" in the footer.

## Main flow

1. The system shows a cookie notice in friendly tone in DE or EN: explanation, categories (technically necessary, optional analytics in CH), control elements.
2. The visitor can accept all, only required, or selectively activate categories.
3. The choice is persisted as a cookie-free local consent record.
4. The visitor can revoke or change the choice in the footer at any time.
5. The system never sets advertising or external CDN cookies.

## Alternative flows

- 2a Visitor closes the dialog without choosing: only technically necessary cookies are used; no implicit consent for the rest.

## Exception flows

- 3x Persistence error: dialog is shown again on next visit.

## Postconditions

- Success: choice persisted; only consented categories active.
- Failure: only technically necessary cookies are active.

## Business rules

- BR-001 No pre-checked optional categories.
- BR-002 No external trackers without active consent.
- BR-003 The DE version contains no sharp s.

## Acceptance criteria (BDD)

```gherkin
Feature: UC-052 Cookie Notice and consent

  Scenario: Only required without consent
    Given a first visit without confirming the dialog
    Then only technically necessary cookies are used

  Scenario: Withdrawal of consent at any time
    Given an existing consent
    When the visitor opens "Cookies" in the footer and revokes
    Then optional categories are deactivated
```
