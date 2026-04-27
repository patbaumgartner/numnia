# UC-050 - Visitor views Privacy Policy

| Field | Value |
| --- | --- |
| Use-Case ID | UC-050 |
| Title | Visitor views Privacy Policy (FADP/GDPR, DE/EN) |
| Release | R1 |
| Primary actor | Visitor (any user) |
| Secondary actors | (none) |
| Status | Specified |
| Goal | Visitors can fully read the privacy policy in Swiss High German or English in compliance with FADP and GDPR. |
| Related requirements | NFR-PRIV-001, NFR-PRIV-002, NFR-OPS-003, NFR-I18N-001, NFR-I18N-002, NFR-A11Y-004 |

## Preconditions

1. The site is reachable over HTTPS.
2. The privacy policy is published in DE and EN.

## Trigger

The visitor opens the footer link "Datenschutz" / "Privacy" or the URL `/datenschutz` / `/privacy`.

## Main flow

1. The system shows the privacy policy with the following sections (binding):
   - Operator and data controller (CH)
   - Categories of data processed (especially child data)
   - Purposes of processing (learning operations, security, moderation)
   - Legal bases (FADP / GDPR Art. 6, parental consent)
   - Recipients (none outside CH where avoidable, no third-party trackers, no public CDN)
   - Retention periods and deletion (data minimization, FR-PAR-005)
   - Rights of data subjects: information, correction, deletion, restriction, data portability, complaint with FDPIC (CH)
   - Children's data and parental responsibility
   - Cookies and local storage (technically necessary only)
   - Contact for data-protection enquiries (DPO contact)
   - Data location: Switzerland (NFR-OPS-003)
2. The page is available in DE (no sharp s, with umlauts) and EN.
3. A clearly visible language switch toggles between DE/EN.

## Alternative flows

- 2a Browser language `de-CH` / `de`: DE is the default; otherwise EN.

## Exception flows

- 1x Page cannot be loaded: a static fallback is shown.

## Postconditions

- Success: policy displayed.

## Business rules

- BR-001 Reachable from the footer of every page.
- BR-002 No external trackers on this page.
- BR-003 The DE version contains no sharp s.

## Acceptance criteria (BDD)

```gherkin
Feature: UC-050 Privacy Policy

  Scenario: Reachable from any page
    Given any page of the site
    When the visitor clicks the footer link "Datenschutz"
    Then the privacy policy is displayed

  Scenario: Both languages available
    Given the privacy policy in German
    When the visitor switches the language to English
    Then the privacy policy is displayed in English

  Scenario: No external trackers on the page
    When the visitor opens the privacy policy
    Then no requests go to external trackers
```
