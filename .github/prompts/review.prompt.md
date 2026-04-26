---
description: Craftsmanship, security and quality-gate review for a branch or pull request - returns a structured findings list.
mode: agent
---

# /review - Craftsmanship + Quality-Gate Review

## Task

Analyze the changed files (branch or PR diff) and check against the following criteria.

### Traceability
- [ ] PR references at least one `UC-XXX` and the related `FR-/NFR-` IDs.
- [ ] Use-case spec is consistent with the implementation.
- [ ] arc42 or ADRs updated for structural changes.

### Engineering quality
- [ ] Test First evident (tests in the same commit or earlier).
- [ ] BDD scenarios exist and pass (Cucumber).
- [ ] Coverage met (Backend ≥ 80% line / 70% branch, Frontend ≥ 70% line).
- [ ] No `TODO`/`FIXME` without an issue reference.
- [ ] Clean Code, SOLID, small methods, expressive names.

### Security & privacy
- [ ] Server-side validation and authorization in place.
- [ ] Audit log for security-relevant actions.
- [ ] No PII in logs.
- [ ] Double opt-in verified, when sensitive area is touched.

### Pedagogy (when learning/game/gamification touched)
- [ ] S/G levels and mastery thresholds are configuration, not hard-coded.
- [ ] Frustration protection effective (3 errors → speed downgrade + mode suggestion).
- [ ] Errors do not cost star points/items.

### Language
- [ ] Project documentation, code comments and identifiers in English.
- [ ] In-product UI text in Swiss High German with umlauts, no sharp s.

### Operations
- [ ] Configurable values are externally configurable (no code deployment needed).
- [ ] Health/monitoring endpoints unchanged and operational.

## Output

Structured review report (Markdown) with:
- Summary (traffic light per category)
- Concrete findings with file + line + suggestion
- Blockers vs. hints clearly separated
- Recommendation: "merge", "merge after changes", "blocked"
