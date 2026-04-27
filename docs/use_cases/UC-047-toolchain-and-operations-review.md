# UC-047 - Toolchain and operations review

| Field | Value |
| --- | --- |
| Use-Case ID | UC-047 |
| Title | Half-yearly toolchain and operations review |
| Release | R5 |
| Primary actor | System Admin / Architect |
| Secondary actors | Operator |
| Status | Specified |
| Goal | The team reviews stack and operations every six months and documents the result. |
| Related requirements | NFR-ENG-005, NFR-OPS-001 |

## Preconditions

1. Established toolchain (SRS §4.1, ADR-001).

## Trigger

Quarter-end every 6 months.

## Main flow

1. The architect runs a status check of all toolchain components.
2. Outdated or vulnerable components are flagged.
3. The team decides via ADR whether to upgrade, replace or pin.
4. The result is documented in the architecture log.

## Alternative flows

- 3a Critical CVE: an out-of-band review is triggered immediately.

## Exception flows

- (none)

## Postconditions

- Success: documented decision; ADR updated where needed.

## Business rules

- BR-001 At least one toolchain review per half-year (NFR-ENG-005).

## Acceptance criteria (BDD)

```gherkin
Feature: UC-047 Toolchain review

  Scenario: Half-yearly review held
    Given six months have passed since the last review
    When the architect performs the review
    Then the result is documented in the architecture log
```
