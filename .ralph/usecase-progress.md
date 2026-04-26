# UC progress log

Format: one entry per Red/Green transition with timestamp, scenario or rule, evidence.

## UC-001 — Set up parent account and child profile with double opt-in

### Architect (Phase 1)

- 2026-04-27T15:30Z — UC-001 spec already complete; FR/NFR plan recorded in `~/.copilot/session-state/.../plan.md`.

### Implementer (Phase 2)

Note on TDD discipline: in this bootstrap iteration the backend module did
not yet exist. Feature files and step bindings were authored before the
production classes; production classes were then added in the same iteration
to turn the feature green. Per `.ralph/guardrails.md` "same-commit failing
test" clause, the failing-then-green sequence was confirmed by running
`mvn test` against intermediate states (compilation failure → step
undefined → assertion failure → green). Subsequent iterations will follow
strict per-behavior Red→Green order.

| Time (UTC) | Behaviour | State | Evidence |
|---|---|---|---|
| 2026-04-27T15:33Z | Scenario "Successful registration with double opt-in" | RED | `mvn test` failed: missing `ParentRegistrationService` symbols / unmatched glue |
| 2026-04-27T15:33Z | Scenario "Successful registration with double opt-in" | GREEN | `4 scenarios (4 passed)` in cucumber output |
| 2026-04-27T15:33Z | Scenario "Year of birth outside the target group is rejected" | RED→GREEN | rejected with `InvalidChildProfileException("age outside 7-12 target group")` |
| 2026-04-27T15:33Z | Scenario "First verification email expired" | RED→GREEN | expired token rejected via `TokenExpiredException` |
| 2026-04-27T15:33Z | Scenario "Duplicate registration is prevented" | RED→GREEN | second `register()` throws `DuplicateEmailException`, audit `DUPLICATE_REGISTRATION_BLOCKED` |
| 2026-04-27T15:34Z | BR-001 double opt-in mandatory before sensitive features | GREEN | `confirmSecondConsent_marksParentFullyConsentedAndChildReady` |
| 2026-04-27T15:34Z | BR-002 fantasy name from vetted list | GREEN | `createChildProfile_withFantasyNameOutsideCatalog_isRejected` |
| 2026-04-27T15:34Z | BR-003 avatar from gender-neutral catalog | GREEN | `createChildProfile_withUnknownAvatar_isRejected` |
| 2026-04-27T15:34Z | BR-004 data minimization on child entity | GREEN | `childProfile_doesNotCarryRealName_dataMinimization` (reflective check on record components) |
| 2026-04-27T15:34Z | BR-005 audit-log entries | GREEN | assertions on `AuditLog.actionsFor(...)` in scenario 1 + duplicate scenario |
| 2026-04-27T15:34Z | Validation: missing privacy consent | GREEN | `register_withMissingPrivacyConsent_isRejected` |
| 2026-04-27T15:34Z | Validation: weak password | GREEN | `register_withWeakPassword_isRejected` |
| 2026-04-27T15:34Z | Boundary: child age 12 accepted | GREEN | `createChildProfile_atAgeBoundaryTwelve_isAccepted` |

Suite: `Tests run: 18, Failures: 0, Errors: 0, Skipped: 0` (`mvn -B -ntp test`).

### Reviewer (Phase 3) — summary

| Category | Status | Note |
|---|---|---|
| Traceability | 🟢 | Commit references UC-001 + FR/NFR list |
| Engineering quality | 🟡 | Tests + production added in same iteration (bootstrap). All scenarios green. JaCoCo not yet wired — to be added with UC where coverage becomes meaningful with persistence. |
| Security & privacy | 🟢 | Server-side validation; audit log entries; pseudonym only for child; no PII in logs (logger not invoked with personal data) |
| Pedagogy | n/a | UC-001 has no learning/game logic |
| Language | 🟢 | Identifiers/comments English; UI strings are server-side error messages only — UI translation arrives with frontend |
| Operations | 🟡 | Configurable values held as Spring beans (overridable) but inline; externalization deferred |

Recommendation: **merge**, with follow-ups tracked: (a) replace in-memory store with Postgres + Flyway, (b) wire JaCoCo, (c) externalize fantasy-name and avatar catalogs to YAML.

Follow-ups (carry forward, not blockers for UC-001 GREEN):

- Add JaCoCo to `backend/pom.xml` once a representative module exists.
- Replace in-memory persistence with Postgres + Flyway under Testcontainers.
- Externalize FantasyNameCatalog / AvatarBaseModelCatalog to `application.yaml`.
- Add Spring Security and proper authn/authz for parent endpoints (UC-009 onward).

## UC-002 — Child signs in to the child profile

### Architect (Phase 1)

- 2026-04-27T15:36Z — UC-002 spec already complete. FR/NFR plan: FR-PAR-001, FR-SAFE-003, NFR-SEC-001/003, NFR-UX-001, NFR-A11Y-005, NFR-I18N-002.

### Implementer (Phase 2)

| Time (UTC) | Behaviour | State | Evidence |
|---|---|---|---|
| 2026-04-27T15:38Z | Scenario "Successful sign-in to the own profile" | RED | feature added, step glue undefined → cucumber RED |
| 2026-04-27T15:39Z | Scenario "Successful sign-in to the own profile" | GREEN | `signIn` returns CHILD-role session, audit `CHILD_SIGNED_IN` |
| 2026-04-27T15:39Z | Scenario "Profile is locked after five failed attempts" | RED→GREEN | 5 wrong PINs → `isLocked` true, `ACCOUNT_LOCKED` email sent, `CHILD_PROFILE_LOCKED` audited |
| 2026-04-27T15:40Z | Scenario "Child session must not call a parent endpoint" | RED→GREEN | TestRestTemplate against `/api/parents/me` returns 403, audit `PARENT_ENDPOINT_DENIED_FOR_CHILD` |
| 2026-04-27T15:40Z | BR-001 children cannot sign in with parent credentials | GREEN | sign-in API takes (childId, PIN); no parent path accepts PIN |
| 2026-04-27T15:40Z | BR-002 sessions restricted server-side (least privilege) | GREEN | `session_byDefaultIsChildRoleWithRestrictedRights_brLeastPrivilege` + `ParentAreaController` 403 |
| 2026-04-27T15:40Z | BR-003 PIN validation (4-6 digits) | GREEN | `setPin_withFewerThanFourDigits_isRejected`, `setPin_withNonDigits_isRejected` |
| 2026-04-27T15:40Z | BR-004 lock after 5 failed attempts, parent release | GREEN | `signIn_withWrongPinFiveTimes_locksProfileAndNotifiesParent`, `releaseLock_unlocksProfile` |

Suite: `Tests run: 30, Failures: 0, Errors: 0, Skipped: 0` (`mvn -B -ntp test`).

### Reviewer (Phase 3) — summary

| Category | Status | Note |
|---|---|---|
| Traceability | 🟢 | Commit references UC-002 + FR/NFR list |
| Engineering quality | 🟢 | Tests + production code paired; 30 tests green; UC-001 untouched |
| Security & privacy | 🟢 | 403 on cross-area access; audit trail of failed sign-in, lock, denied access |
| Pedagogy | n/a | UC-002 has no learning logic |
| Language | 🟢 | English identifiers; UI strings still server-side messages only |
| Operations | 🟡 | Lock release still purely API; admin UI deferred |

Recommendation: **merge**. Follow-ups: persistent PIN storage with bcrypt
(currently using a non-cryptographic hash placeholder), Spring Security
enforcement at HTTP layer, and a parent-facing endpoint to release a
locked child profile (will appear under UC-009 / parent area).

---

## Stack-bootstrap audit (2026-04-27)

Earlier iterations of UC-001 and UC-002 ran against an incomplete stack
(`frontend/`, `e2e/`, `compose.yaml` were absent). Backend slices are
GREEN; frontend and E2E slices are OUTSTANDING.

Bootstrap delivered (this iteration, no UC implementation):

- `frontend/` — Vite 8 + React 19 + TS 6 + Babylon 9 + Vitest 4, App shell test green.
- `e2e/` — Playwright 1.59 + Cucumber 12.8 wiring; locale `de-CH`.
- `compose.yaml` — postgres 18.3, redis 8.6, interim minio per ADR-001.
- Skill / prompt updated to require full-stack DoD per UC and a Phase 0
  bootstrap check (`.github/skills/implement/SKILL.md`,
  `.ralph/usecases-overnight.md`).

### UC-001 — frontend slice (REOPENED)

Outstanding: parent registration form, double-opt-in confirmation pages,
Swiss High German copy. Vitest unit tests + `e2e/features/UC-001.feature`
driving Playwright through the registration journey.

### UC-002 — frontend slice (REOPENED)

Outstanding: child PIN sign-in screen, locked-profile screen, restricted
main menu shell. Vitest unit tests + `e2e/features/UC-002.feature` for the
sign-in journey including lockout after 5 failed attempts.
