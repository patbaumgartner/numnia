Do not:

- rewrite all architecture at once
- skip tests
- commit failing code
- invent requirements not present in docs
- weaken security or privacy constraints
- remove existing documentation
- implement more than one use case per iteration

Test-first discipline (software-crafter style, non-negotiable):

- Write a failing test BEFORE the production code that would make it pass.
- Confirm the test fails for the right reason (assertion / missing symbol),
  not because of an unrelated compile or config error.
- Make the smallest production change to turn it green. No speculative code.
- Refactor only with all tests green; revert any refactor that turns red.
- One behavior per test. Every test contains at least one assertion.
- No `@Disabled`, `@Ignore`, `it.skip`, `xit`, `describe.skip` to silence CI.
- No mocking the system under test; mock only collaborators across boundaries.
- Production code without a paired prior or same-commit failing test is forbidden.
- Every Gherkin scenario from the UC must exist as an executable Cucumber
  scenario before the matching production code is written.
- Log every Red and matching Green in .ralph/usecase-progress.md.
