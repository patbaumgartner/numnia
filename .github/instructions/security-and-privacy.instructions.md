---
description: Security and privacy guardrails - apply to every endpoint, every persistence layer and every frontend data flow.
applyTo: "backend/**/*.java,frontend/src/**/*.{ts,tsx}"
---

# Security and Privacy - Numnia

## Per-endpoint duties

- HTTPS only, TLS via reverse proxy (NFR-SEC-001).
- Validate inputs **server-side** (Bean Validation / Hibernate Validator) - client validation is UX only (NFR-SEC-002).
- Enforce authentication and role-based authorization server-side (least privilege; NFR-SEC-003).
- Rate-limit auth and critical endpoints (NFR-SEC-004).
- Audit log for security-relevant actions (FR-SAFE-005).

## Child protection

- No free-text input field in the child UI (FR-SAFE-001/002).
- Profile names only from a curated fantasy-name list (FR-SAFE-003).
- Double opt-in before sensitive functions (FR-SAFE-006). In code: `ParentConsent` must satisfy `level == DOUBLE_OPT_IN_CONFIRMED`.

## Privacy / FADP / GDPR

- Data minimization in the data model (NFR-PRIV-001). No optional "nice to have" fields without justification in a UC.
- Pseudonymized child identification - never log a child's real name in clear text.
- Parent self-service for export (JSON/PDF) and deletion (UC-010, UC-011) must cover all data.
- Backups and live data **only in Switzerland** (NFR-OPS-003).
- **No external trackers** (analytics, ads, external fonts, external CDNs).

## Communication

- Validate and rate-limit WebSocket frames server-side.
- No sensitive data in URL parameters or logs.

## Forbidden patterns

- `String userInput = request.getParameter(...)` without validation.
- `@CrossOrigin(origins = "*")`.
- Logging a child's clear-text name, date of birth or parent email address.
- Storing personal data outside the configured CH databases.

Sources: NFR-SEC-001..004, NFR-PRIV-001/002, FR-SAFE-001..006, NFR-OPS-003.
