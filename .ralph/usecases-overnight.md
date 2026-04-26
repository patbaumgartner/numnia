Run the project implementation loop for all documented use cases.

Mandatory context:

- AGENTS.md
- README.md
- .github/copilot-instructions.md
- .github/instructions/*.instructions.md
- .github/chatmodes/architect.chatmode.md
- .github/chatmodes/implementer.chatmode.md
- .github/chatmodes/reviewer.chatmode.md
- .github/prompts/use-case-spec.prompt.md
- .github/prompts/entity-model.prompt.md
- .github/prompts/adr.prompt.md
- .github/prompts/implement.prompt.md
- .github/prompts/unit-test.prompt.md
- .github/prompts/e2e-test.prompt.md
- .github/prompts/review.prompt.md
- docs/Requirements.md
- docs/architecture/arc42.md
- docs/use_cases.puml
- docs/use_cases/*.md

Task:
Implement UC-001 through UC-011 in order.

Always-on instructions (apply in every phase, every iteration):

- .github/copilot-instructions.md (project rules and forbidden actions)
- .github/instructions/documentation-language.instructions.md (English for docs/code/comments)
- .github/instructions/tdd-bdd.instructions.md (Test-First, TDD, BDD)
- .github/instructions/security-and-privacy.instructions.md (TLS, authn/z, data minimization)
- .github/instructions/pedagogy.instructions.md (mastery, frustration protection, S/G levels)
- .github/instructions/ui-language.instructions.md (Swiss High German, no sharp s, only in i18n bundles)

For each use case, follow these three phases in order. At the start of each phase,
read the corresponding chatmode file and the listed prompt file, then adopt that role
for the duration of the phase.

PHASE 1 - Architect
  Read:
    - .github/chatmodes/architect.chatmode.md (adopt persona)
    - .github/prompts/use-case-spec.prompt.md (gap-check the UC spec)
    - .github/prompts/entity-model.prompt.md (only if new entities are introduced)
    - .github/prompts/adr.prompt.md (only if a stack/architectural decision is needed)
  Do:
    - Read the matching use-case file under docs/use_cases/.
    - Identify entities, API contracts, persistence and security implications.
    - Confirm the change fits SRS section 4.1 and arc42; if a stack or
      architectural decision is needed, draft an ADR under docs/adr/ first.
    - If the UC spec has gaps, extend it via use-case-spec.prompt.md before coding.
    - Produce a short plan listing referenced FR-/NFR- IDs and the BDD scenarios
      that must be made executable.

PHASE 2 - Implementer (STRICT TDD/BDD - software-crafter discipline)
  Read:
    - .github/chatmodes/implementer.chatmode.md (adopt persona)
    - .github/prompts/implement.prompt.md (end-to-end implementation procedure)
    - .github/prompts/unit-test.prompt.md (JUnit 5 / Vitest patterns)
    - .github/prompts/e2e-test.prompt.md (Playwright + Cucumber patterns)

  Iterate strictly Red -> Green -> Refactor for EVERY behavior in the UC.
  Do NOT batch tests and implementation. Do NOT write production code without
  a currently-failing test that requires it.

  Per-behavior cycle (repeat for every BDD scenario AND every unit-level rule):

    STEP A - RED (test must fail for the right reason)
      1. Copy the next Gherkin scenario verbatim into the appropriate feature file:
           - backend-only logic: backend/src/test/resources/features/UC-XXX.feature
           - UI journey:         e2e/features/UC-XXX.feature
      2. Add or extend the matching unit test (JUnit 5 + AssertJ + Mockito for
         backend, Vitest + RTL for frontend). One test = one behavior.
      3. RUN the test command (Maven/pnpm/Cucumber).
      4. Confirm it FAILS, and fails for the expected reason
         (assertion / missing class / missing method - NOT a compile error in an
         unrelated file, NOT a configuration error).
      5. Append the test name and the failure reason to .ralph/usecase-progress.md
         under "RED" for the current UC.
      6. NEVER skip this step. If you accidentally write production code first,
         delete it and restart this cycle.

    STEP B - GREEN (smallest change to pass)
      1. Write the MINIMUM production code that makes the failing test pass.
         No speculative generality. No code paths the current tests do not cover.
      2. RUN the same test command.
      3. Confirm the previously-failing test PASSES and no other tests regressed.
      4. If anything else turned red, fix it before continuing.
      5. Append "GREEN" entry with the passing test name to
         .ralph/usecase-progress.md.

    STEP C - REFACTOR (keep all tests green)
      1. Apply Clean Code and SOLID: extract methods/classes, rename, remove
         duplication, push behavior to the right object.
      2. RUN the full test suite for the touched module after each refactor.
      3. If anything breaks, revert that single refactor immediately.
      4. Do NOT add features here. Refactor changes structure, not behavior.

  Hard rules for the implementer phase:
    - No production code in a commit without an accompanying failing-then-passing
      test. The test must appear in the same commit as the production code,
      OR in an earlier commit on the same UC branch.
    - No `@Disabled` / `it.skip` / `xit` / `@Ignore` to make CI green.
    - No assertion-free tests (e.g. tests that only call a method without
      asserting outcomes).
    - No mocking of the system under test. Mocks only for collaborators that
      cross a boundary (HTTP, DB, time, randomness, file system).
    - Test naming: backend `methodUnderTest_state_expectedBehaviour`,
      frontend describe-it phrased as a behavior.
    - Server-side validation and authorization are themselves driven by tests
      (negative tests for forbidden / unauthenticated / invalid input).
    - Honor every always-on instruction listed above
      (security, privacy, pedagogy, UI language, documentation language).
    - Implement only one use case per iteration.

  Definition of Done for PHASE 2 (must hold before entering PHASE 3):
    - Every Gherkin scenario from the UC spec exists as an executable Cucumber
      scenario and is GREEN.
    - Every business rule listed in the UC has at least one unit test covering
      the happy path and at least one covering a failure path.
    - The full test suite of the touched module is green locally.
    - .ralph/usecase-progress.md contains a RED entry and a matching GREEN entry
      for every behavior added in this iteration.

PHASE 3 - Reviewer
  Read:
    - .github/chatmodes/reviewer.chatmode.md (adopt persona)
    - .github/prompts/review.prompt.md (review checklist and quality gates)
  Do:
    - Run all required checks from README.md / AGENTS.md.
    - Verify coverage gates (backend >= 80% line / 70% branch, frontend >= 70% line).
    - Verify traceability: commit message and code reference UC-XXX and FR-/NFR- IDs.
    - Verify no English UI strings for child/parent-facing UI and no sharp s in UI text.
    - Re-check all always-on instructions before committing.
    - TDD audit (anti-cheat - all of these must hold, or send the UC back to PHASE 2):
        *.ralph/usecase-progress.md contains a RED entry and a matching GREEN
          entry for every behavior in this UC.
        * No new test file uses `@Disabled`, `@Ignore`, `it.skip`, `xit`,
          `describe.skip`, or empty bodies.
        *Every new test contains at least one assertion (assertThat / expect).
        * Every Gherkin scenario from the UC spec maps to exactly one
          executable scenario in a `.feature` file under `backend/.../features`
          or `e2e/features`.
        *`git log` for this UC shows tests added at or before the matching
          production code (no production-only commits without prior or paired
          tests).
        * Mutation-style sanity check: pick one production method introduced
          for this UC, mentally invert one branch (or change a constant),
          and confirm at least one test would fail. If not, the test set is
          insufficient - return to PHASE 2.
    - Fix any failure before continuing. Do not commit on red.
    - Commit after the use case is fully green and documented.
      Commit message MUST follow Conventional Commits and reference UC-XXX
      and the touched FR-/NFR- IDs.

Progress tracking:

- Maintain .ralph/usecase-progress.md
- Record completed UC IDs, test commands, failing checks, decisions and open risks.

Stop condition:
When UC-001 through UC-011 are implemented, documented and all checks pass, print:

ALL_USE_CASES_DONE
