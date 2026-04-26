---
description: Pedagogical guardrails - apply to every change in task generator, adaptive engine, mastery, spaced repetition, gamification.
applyTo: "backend/**/learning/**,backend/**/game/**,backend/**/gamification/**,frontend/src/**/learning/**,frontend/src/**/game/**"
---

# Pedagogical Guardrails - Numnia

## Mastery & adaptivity (SRS §6.1.1 / §6.1.2)

- Difficulty levels: **S1 to S6**. Speed levels: **G0 to G5**. Default values see SRS §6.1.1.
- Mastery thresholds per level are **configuration**, not code (FR-OPS-002, ADR-008). Stored as a versioned configuration entity.
- Mastery is granted only after **at least 2 sessions on at least 2 different calendar days** (FR-LEARN-012).
- At least **30 tasks** per content domain before the first mastery is granted.
- **Hold threshold**: accuracy < 70% in the window → relapse, downgrade in spaced repetition (FR-LEARN-011).
- Re-test intervals per level: 3 / 5 / 7 / 10 / 14 days.

## Frustration protection

- 3 consecutive errors or timeouts in the same domain → automatic speed downgrade (G − 1) and a mode suggestion (accuracy or explanation mode).
- No mastery loss from a single failed re-test (spaced repetition instead of demotion).

## Gamification

- **Star points are the only soft currency**, only earned through play (FR-GAM-001/002).
- Item prices are transparent and fixed (FR-GAM-003).
- **Errors never cost** star points or items (FR-GAM-005).
- Avatar items and unlocked creatures are **permanent** in the inventory (FR-CRE-006).
- Risk mechanic only as a reversible mid variant (shield + round pool, FR-GAM-006), default off.

## Multiplayer fairness

- Turn-based, **never** real-time (FR-MP-001).
- Turn time limit derived exclusively from G levels (FR-MP-004).
- **No global** leaderboards (FR-MP-007). Only class-/friend-circle-internal with fantasy names (FR-MP-008).

## Language

- Task texts and voice output use **Swiss High German without sharp s, with umlauts** (NFR-I18N-002/004).
- Story problems (S6) are calibrated to grade-level reading ability.

Sources: SRS §6.1.1, §6.1.2, §6.5, §6.6.
