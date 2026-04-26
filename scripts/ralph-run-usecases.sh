#!/usr/bin/env bash

set -euo pipefail

# Run normally:
#   ./scripts/ralph-run-usecases.sh
#
# Run overnight in background:
#   nohup ./scripts/ralph-run-usecases.sh &
#
# Follow logs:
#   tail -f logs/ralph-usecases-*.log

BRANCH="ralph/usecases-overnight"
PROMPT_FILE=".ralph/usecases-overnight.md"
COMPLETION_PHRASE="ALL_USE_CASES_DONE"
MAX_ITERATIONS="120"
TIMEOUT="10h"
# Model can be overridden: RALPH_MODEL=gpt-5.4 ./scripts/ralph-run-usecases.sh
# Available (per local Copilot CLI): auto, claude-sonnet-4.6, claude-sonnet-4.5,
# claude-haiku-4.5, claude-opus-4.7, claude-sonnet-4, gpt-5.4, gpt-5.5,
# gpt-5.3-codex, gpt-5.2-codex, gpt-5.2, gpt-5.4-mini, gpt-5-mini, gpt-4.1.
MODEL="${RALPH_MODEL:-claude-opus-4.7}"

TIMESTAMP="$(date +%Y%m%d-%H%M%S)"
LOG_DIR="logs"
LOG_FILE="${LOG_DIR}/ralph-usecases-${TIMESTAMP}.log"

echo "== Ralph use-case overnight runner =="

command -v git >/dev/null || {
  echo "ERROR: git is not installed or not in PATH."
  exit 1
}

command -v ralph >/dev/null || {
  echo "ERROR: ralph is not installed or not in PATH."
  echo "Install it first, for example:"
  echo "  go install github.com/JanDeDobbeleer/copilot-ralph/cmd/ralph@latest"
  exit 1
}

if [[ ! -f "$PROMPT_FILE" ]]; then
  echo "ERROR: Prompt file not found: $PROMPT_FILE"
  exit 1
fi

if [[ -n "$(git status --porcelain)" ]]; then
  echo "ERROR: Working tree is not clean."
  echo "Commit or stash your changes before running Ralph."
  git status --short
  exit 1
fi

mkdir -p ".ralph" "$LOG_DIR"

CURRENT_BRANCH="$(git rev-parse --abbrev-ref HEAD)"

if git rev-parse --verify "$BRANCH" >/dev/null 2>&1; then
  echo "Using existing branch: $BRANCH"
  git checkout "$BRANCH"
else
  echo "Creating branch: $BRANCH"
  git checkout -b "$BRANCH"
fi

echo "Started at: $(date)"
echo "Original branch: $CURRENT_BRANCH"
echo "Working branch: $(git rev-parse --abbrev-ref HEAD)"
echo "Model: $MODEL"
echo "Prompt file: $PROMPT_FILE"
echo "Log file: $LOG_FILE"
echo

set +e

ralph run \
  --working-dir . \
  --model "$MODEL" \
  --max-iterations "$MAX_ITERATIONS" \
  --timeout "$TIMEOUT" \
  --promise "$COMPLETION_PHRASE" \
  --log-level info \
  --streaming=true \
  "$PROMPT_FILE" 2>&1 | tee "$LOG_FILE"

RALPH_EXIT_CODE="${PIPESTATUS[0]}"

set -e

echo
echo "Finished at: $(date)"
echo "Ralph exit code: $RALPH_EXIT_CODE"

if [[ $RALPH_EXIT_CODE -eq 0 ]] && grep -q "^${COMPLETION_PHRASE}\$\|^${COMPLETION_PHRASE}[[:space:]]" "$LOG_FILE"; then
  echo "SUCCESS: Ralph reported completion: $COMPLETION_PHRASE"
else
  echo "WARNING: Ralph did not complete successfully."
  echo "  Exit code: $RALPH_EXIT_CODE"
  echo "Check progress:"
  echo "  cat .ralph/usecase-progress.md"
  echo "  tail -100 $LOG_FILE"
fi

echo
echo "Git status:"
git status --short

exit "$RALPH_EXIT_CODE"
