# UC-053 - Visitor views Accessibility Statement

| Field | Value |
| --- | --- |
| Use-Case ID | UC-053 |
| Title | Visitor views Accessibility Statement (DE/EN) |
| Release | R1 |
| Primary actor | Visitor (any user) |
| Secondary actors | (none) |
| Status | Specified |
| Goal | Visitors learn how Numnia is accessible to children with different needs. |
| Related requirements | NFR-A11Y-001, NFR-A11Y-002, NFR-A11Y-003, NFR-A11Y-004, NFR-A11Y-005, NFR-I18N-001 |

## Preconditions

1. Accessibility statement published in DE/EN.

## Trigger

The visitor opens the footer link "Barrierefreiheit" / "Accessibility".

## Main flow

1. The system shows: standards followed (WCAG 2.2 AA target), supported features (dyscalculia mode, color-blind profiles, reduced motion, keyboard operation, screen-reader support in parent area), known limitations, contact for feedback.
2. The page is available in DE (no sharp s, with umlauts) and EN.

## Alternative flows

- 1a Browser language: see UC-050.

## Exception flows

- 1x Page cannot be loaded: static fallback.

## Postconditions

- Success: statement displayed.

## Business rules

- BR-001 Reachable from the footer of every page.

## Acceptance criteria (BDD)

```gherkin
Feature: UC-053 Accessibility Statement

  Scenario: Statement is reachable
    Given any page of the site
    When the visitor clicks the footer link "Barrierefreiheit"
    Then the accessibility statement is displayed

  Scenario: Both languages available
    Given the statement in German
    When the visitor switches the language to English
    Then the statement is displayed in English
```
