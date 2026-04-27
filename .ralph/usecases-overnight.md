# Numnia overnight loop — implement UC-001 through UC-011

You are running inside a Ralph iteration loop. The system prompt bundle
already contains AGENTS.md, .ralph/guardrails.md, every agent profile under
`.github/agents/` and every `SKILL.md` under `.github/skills/`. Treat the
bundle as authoritative.

**Authoritative rules** (do not paraphrase, do not weaken):

- `AGENTS.md` — project rules, stack, languages, DoR/DoD.
- `.ralph/guardrails.md` — forbidden actions and test-first discipline.

If anything below conflicts with those two files, those two files win.

## This loop adds

- **One use case per iteration.** Pick the lowest-numbered UC under
  `docs/use_cases/` that is not yet GREEN in `.ralph/usecase-progress.md`.
- **Full stack per UC.** Backend AND frontend slices both reach GREEN before
  the UC is marked done. A UC with a UI main flow MUST have an executable
  E2E Cucumber scenario in `e2e/features/UC-XXX.feature` driving Playwright.
  Backend-only UCs must justify the absence of an E2E feature in the
  progress log.
- **Commit only on green.** Conventional Commits referencing UC-XXX and the
  touched FR-/NFR- IDs.
- **Do not edit `docs/Requirements.md`.**

## Phase 0 — Bootstrap check (every iteration, before Phase 1)

Verify scaffolding exists. If any item is missing, stop the UC work and
bootstrap first; record the bootstrap commit before resuming the UC.

- `backend/pom.xml`, `backend/src/main/java`, `backend/src/test/resources/features/`
- `frontend/package.json`, `frontend/src`, `frontend/vite.config.ts`
- `e2e/package.json`, `e2e/features/`, `e2e/steps/`
- `compose.yaml` at workspace root

## Per-iteration procedure

For the selected UC, run all three phases in order. At the start of each
phase, `read_file` the listed agent profile and skill files and adopt that
role for the duration of the phase.

### Phase 1 — Architect

- Adopt: `.github/agents/architect.agent.md`
- Skills: `use-case-spec` (always), `entity-model` (if new entities),
  `adr` (if a stack/architectural decision is needed).
- Read the UC under `docs/use_cases/UC-XXX-*.md`.
- If the UC has gaps, extend the spec via the `use-case-spec` skill.
- Output a short plan listing the FR-/NFR- IDs and the BDD scenarios to
  make executable.

### Phase 2 — Implementer

- Adopt: `.github/agents/implementer.agent.md`
- Skills: `implement`, `unit-test`, `e2e-test`.
- Follow the test-first discipline in `.ralph/guardrails.md` for every
  BDD scenario and every unit-level rule. For each behavior:
  - **RED:** copy the Gherkin scenario verbatim into the matching feature
    file (`backend/src/test/resources/features/UC-XXX.feature` or
    `e2e/features/UC-XXX.feature`); add the failing unit test; run; confirm
    it fails for the right reason; log a RED entry in
    `.ralph/usecase-progress.md`.
  - **GREEN:** smallest production change to pass; run; log a GREEN entry.
  - **REFACTOR:** Clean Code/SOLID with all tests green; revert any
    refactor that turns red.
- Done when every UC scenario is an executable Cucumber scenario and green,
  every business rule has happy- and failure-path tests, the module suite
  is green locally, and progress file has paired RED/GREEN entries for
  every behavior added.

### Phase 3 — Reviewer

- Adopt: `.github/agents/reviewer.agent.md`
- Skill: `review`.
- Run the full checklist in the `review` skill (coverage gates,
  traceability, UI language, security/privacy, TDD audit). If any check
  fails, return to Phase 2.
- Commit on green.

## Stop condition

When UC-006 through UC-011 are all green, documented and committed, emit
exactly:

<promise>ALL_USE_CASES_DONE</promise>
