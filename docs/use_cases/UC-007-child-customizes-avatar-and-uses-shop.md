# UC-007 - Child customizes avatar and uses the avatar shop

| Field | Value |
| --- | --- |
| Use-Case ID | UC-007 |
| Title | Child customizes avatar and buys items with star points |
| Release | R1 |
| Primary actor | Child (7-12) |
| Secondary actors | Gamification & Inventory |
| Status | Specified |
| Goal | The child customizes its avatar, buys cosmetic items with earned star points and receives permanent ownership. |
| Related requirements | FR-CRE-005, FR-CRE-006, FR-GAM-001, FR-GAM-002, FR-GAM-003, FR-GAM-005 |

## Preconditions

1. Active child session (UC-002).
2. At least one avatar base model and one shop catalog are configured.

## Trigger

The child opens the "Avatar" area in the main menu.

## Main flow

1. The system shows the current avatar setup and the available gender-neutral base models.
2. The child optionally swaps the base model.
3. The child opens the "avatar shop"; the system shows items with transparent fixed prices in star points (FR-GAM-003).
4. The child picks an item; the system checks whether enough star points are available.
5. With enough star points: the system deducts the price and adds the item permanently to the inventory (FR-CRE-006).
6. The child can equip the item on the avatar immediately.
7. The system persists the avatar configuration.

## Alternative flows

- 4a Star points are insufficient: the system shows the notice "collect X more star points"; no purchase.
- 6a Child wants to use the item later: item remains available in the inventory.

## Exception flows

- 5x Attempt to buy the same item twice: the system declines; notice "already in inventory".
- 5y Attempt to manipulate the inventory or the star point balance client-side: the server declines; audit log.

## Postconditions

- Success: avatar configuration updated; item permanently in inventory; star points correctly booked.
- Failure: consistent star point balance; no item without booking.

## Business rules

- BR-001 Star points can only be earned by playing, never bought (FR-GAM-002).
- BR-002 Item prices are transparent and fixed (FR-GAM-003).
- BR-003 Items are permanent after purchase (FR-CRE-006); loss through errors is excluded (FR-GAM-005).
- BR-004 Avatars are based on gender-neutral base models.

## Acceptance criteria (BDD)

```gherkin
Feature: UC-007 Child customizes avatar and buys items

  Background:
    Given an active child session
    And a configured shop catalog

  Scenario: Successful purchase with star points
    Given the child has 50 star points
    And the item "Star Cap" costs 30 star points
    When the child confirms the purchase
    Then the star points balance is reduced to 20
    And the Star Cap is permanently in the inventory

  Scenario: Purchase with insufficient star points is prevented
    Given the child has 10 star points
    And the item costs 30 star points
    When the child tries the purchase
    Then the system shows a notice about collecting more star points
    And no booking takes place

  Scenario: Inventory manipulation via API is rejected
    Given an active child session
    When the client tries to unlock an item without payment
    Then the server responds with an error status
    And the incident is documented in the audit log
```
