#!/usr/bin/env bash
# Run Ralph against the use-case overnight prompt.
# Usage:
#   ./scripts/ralph-run-usecases.sh                # foreground
#   nohup ./scripts/ralph-run-usecases.sh &        # background
#   RALPH_MODEL=gpt-5.4 ./scripts/ralph-run-usecases.sh
set -euo pipefail

BRANCH="ralph/usecases-overnight"
PROMPT=".ralph/usecases-overnight.md"
BUNDLE=".ralph/system-prompt.bundle.md"
PROMISE="ALL_USE_CASES_DONE"
MODEL="${RALPH_MODEL:-claude-opus-4.7}"
LOG="logs/ralph-$(date +%Y%m%d-%H%M%S).log"

CONTEXT=(
  AGENTS.md
  .ralph/guardrails.md
  .github/agents/architect.agent.md
  .github/agents/implementer.agent.md
  .github/agents/reviewer.agent.md
  .github/skills/use-case-spec/SKILL.md
  .github/skills/entity-model/SKILL.md
  .github/skills/adr/SKILL.md
  .github/skills/implement/SKILL.md
  .github/skills/unit-test/SKILL.md
  .github/skills/e2e-test/SKILL.md
  .github/skills/review/SKILL.md
)

command -v git    >/dev/null || { echo "ERROR: git missing";    exit 1; }
command -v ralph  >/dev/null || { echo "ERROR: ralph missing (go install github.com/patbaumgartner/copilot-ralph/cmd/ralph@latest)"; exit 1; }
[[ -f $PROMPT ]] || { echo "ERROR: $PROMPT missing"; exit 1; }
for f in "${CONTEXT[@]}"; do [[ -f $f ]] || { echo "ERROR: $f missing"; exit 1; }; done
[[ -z "$(git status --porcelain)" ]] || { echo "ERROR: working tree dirty"; git status --short; exit 1; }

mkdir -p logs .ralph

# Bundle AGENTS.md + guardrails + agents + skills into one system prompt.
# Ralph's --system-prompt only takes one file, and Copilot CLI does not
# auto-load .github/agents/ or .github/skills/.
{
  echo "# Numnia agent context bundle (auto-generated $(date -u +%FT%TZ))"
  for f in "${CONTEXT[@]}"; do
    printf '\n\n===== %s =====\n\n' "$f"
    cat "$f"
  done
} > "$BUNDLE"

git rev-parse --verify "$BRANCH" >/dev/null 2>&1 \
  && git checkout "$BRANCH" \
  || git checkout -b "$BRANCH"

echo "Branch: $(git rev-parse --abbrev-ref HEAD)  Model: $MODEL  Log: $LOG"

set +e
ralph run \
  --model "$MODEL" \
  --max-iterations 120 \
  --timeout 10h \
  --promise "$PROMISE" \
  --system-prompt "$BUNDLE" \
  --system-prompt-mode append \
  "$PROMPT" 2>&1 | tee "$LOG"
EXIT="${PIPESTATUS[0]}"
set -e

if [[ $EXIT -eq 0 ]] && grep -q "<promise>${PROMISE}</promise>" "$LOG"; then
  echo "SUCCESS: <promise>${PROMISE}</promise>"
else
  echo "INCOMPLETE (exit=$EXIT). See $LOG and .ralph/usecase-progress.md"
fi

git status --short
exit "$EXIT"
