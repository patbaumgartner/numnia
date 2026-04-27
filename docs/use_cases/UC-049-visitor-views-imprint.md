# UC-049 - Visitor views Imprint page

| Field | Value |
| --- | --- |
| Use-Case ID | UC-049 |
| Title | Visitor views Imprint / Impressum (DE/EN) |
| Release | R1 |
| Primary actor | Visitor (any user) |
| Secondary actors | (none) |
| Status | Specified |
| Goal | Anyone can read the imprint of the operator with full transparency in Swiss High German and English. |
| Related requirements | NFR-I18N-001, NFR-I18N-002, NFR-I18N-004, NFR-A11Y-004, NFR-PRIV-001 |

## Preconditions

1. The site is reachable over HTTPS.

## Trigger

The visitor opens the footer link "Impressum" or "Imprint", or the URL `/impressum` / `/imprint`.

## Main flow

1. The system shows the imprint with: operator name, address (CH), contact email, commercial register entry (where applicable), VAT-ID, responsible person according to the relevant CH provisions.
2. The page is available in Swiss High German (no sharp s, with umlauts) and English.
3. A clearly visible language switch toggles between DE/EN; the choice is persisted in a cookie-free local setting.
4. The page is keyboard- and screen-reader-accessible.

## Alternative flows

- 2a Browser language `de-CH` or `de`: DE is the default; otherwise EN.

## Exception flows

- 1x Page cannot be loaded: a static fallback is shown.

## Postconditions

- Success: imprint displayed.

## Business rules

- BR-001 The imprint is always reachable from the footer of every page.
- BR-002 The DE version contains no sharp s; only umlauts (NFR-I18N-002, NFR-I18N-004).
- BR-003 No personal data is collected when viewing the imprint.

## Acceptance criteria (BDD)

```gherkin
Feature: UC-049 Imprint

  Scenario: Imprint reachable from any page
    Given any page of the site
    When the visitor clicks the footer link "Impressum"
    Then the imprint is displayed

  Scenario: German version uses ss instead of sharp s
    Given the imprint in German
    Then the page contains no sharp s
    And umlauts are rendered correctly

  Scenario: Language switch DE/EN
    Given the imprint in German
    When the visitor switches the language to English
    Then the imprint is displayed in English
```
