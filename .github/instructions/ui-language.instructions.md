---
description: In-product UI language rule - Swiss High German for any text shown to children or parents in the app (with umlauts, without sharp s).
applyTo: "frontend/src/**/i18n/**,frontend/public/locales/**,backend/src/main/resources/i18n/**,backend/src/main/resources/messages*.properties"
---

# UI Language Rule - Numnia

Project documentation and code are written in English. The **in-product UI** is written in **Swiss High German** because the target users are Swiss primary-school children and their parents.

## Rules for files matched by this instruction

- Use Swiss High German exclusively.
- Use umlauts (ä, ö, ü) consistently.
- Never use the sharp s (`ß`); replace it with `ss`.
- Avoid English loanwords when an established German term exists (e.g. "Anmeldung" instead of "Login" in user-facing strings).
- Tone for children: friendly, encouraging, non-judgmental; no punitive language.
- Tone for parents and teachers: factual, concise, professional.

## Out of scope

- Code identifiers, comments, log messages, technical errors that are not user-facing: **English**.
- Documentation, ADRs, commit messages, PR descriptions: **English**.

Sources: NFR-I18N-001..004, SRS §6.1.2 (language and reading load), AGENTS.md §6.
