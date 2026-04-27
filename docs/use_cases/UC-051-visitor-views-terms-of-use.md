# UC-051 - Visitor views Terms of Use

| Field | Value |
| --- | --- |
| Use-Case ID | UC-051 |
| Title | Visitor views Terms of Use / Nutzungsbedingungen (DE/EN) |
| Release | R1 |
| Primary actor | Visitor (any user) |
| Secondary actors | (none) |
| Status | Specified |
| Goal | Visitors and parents can read the terms of use in Swiss High German and English. |
| Related requirements | NFR-I18N-001, NFR-I18N-002, NFR-I18N-004, NFR-A11Y-004, FR-SAFE-006 |

## Preconditions

1. Terms of use published in DE/EN.

## Trigger

The visitor opens the footer link "Nutzungsbedingungen" / "Terms" or the URL `/nutzungsbedingungen` / `/terms`.

## Main flow

1. The system shows the terms with: scope, target group (children 7-12 with parental consent), permissible use, prohibited use, child safety, account suspension, liability, changes, governing law (Swiss law), place of jurisdiction.
2. The page is available in DE (no sharp s, with umlauts) and EN.
3. The current version and effective date are visible at the top.
4. The history of older versions is linked.

## Alternative flows

- 2a Browser language: see UC-050.

## Exception flows

- 1x Page cannot be loaded: static fallback.

## Postconditions

- Success: terms displayed.

## Business rules

- BR-001 Reachable from the footer of every page.
- BR-002 Changes are versioned and announced.

## Acceptance criteria (BDD)

```gherkin
Feature: UC-051 Terms of Use

  Scenario: Version and effective date visible
    Given the terms in German
    Then version and effective date are visible at the top

  Scenario: Both languages available
    Given the terms in German
    When the visitor switches the language to English
    Then the terms are displayed in English
```
