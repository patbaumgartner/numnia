#!/usr/bin/env bash
# Run Ralph against the use-case overnight prompt.
#
# Features used (see https://github.com/patbaumgartner/copilot-ralph):
#   - Checkpoint + resume                (--checkpoint-file, ralph resume)
#   - Oracle reviewer (cross-family)     (--oracle-model, --oracle-every,
#                                         --oracle-on-verify-fail)
#   - Per-iteration verify gate          (--verify-cmd, --verify-timeout)
#   - Auto-commit + auto-tag             (--auto-commit, --auto-tag, --diff-stat)
#   - Runaway brakes                     (--stop-on-no-changes, --stop-on-error,
#                                         --stall-after, --iteration-timeout)
#   - Blocked-phrase escape hatch        (--blocked-phrase, --on-blocked)
#   - Lifecycle hooks                    (--on-complete)
#   - Pacing                             (--iteration-delay)
#   - AIUP integration                   (--plan-file, --specs)
#   - Structured observability           (--log-file, --json-output)
#
# Usage:
#   ./scripts/ralph-run-usecases.sh                 # foreground
#   nohup ./scripts/ralph-run-usecases.sh &         # background
#   RALPH_MODEL=claude-opus-4.7 \
#   RALPH_ORACLE_MODEL=gpt-5-codex \
#   RALPH_VERIFY_CMD='./scripts/verify.sh' \
#       ./scripts/ralph-run-usecases.sh
#
#   ./scripts/ralph-run-usecases.sh --reset         # discard checkpoint and start fresh
set -euo pipefail

BRANCH="ralph/usecases-overnight"
PROMPT=".ralph/usecases-overnight.md"
BUNDLE=".ralph/system-prompt.bundle.md"
CHECKPOINT=".ralph/checkpoint.json"
PLAN_FILE=".ralph/usecase-progress.md"
PROMISE="ALL_USE_CASES_DONE"
BLOCKED_PHRASE="${RALPH_BLOCKED_PHRASE:-NEEDS_HUMAN}"

# Primary driver model (Claude Opus by default — strong code synthesis).
MODEL="${RALPH_MODEL:-claude-opus-4.7}"
# Oracle / second-opinion model. Cross-family is the point: pick a GPT/Codex
# class model so the reviewer doesn't share Claude's blind spots.
ORACLE_MODEL="${RALPH_ORACLE_MODEL:-gpt-5-codex}"
ORACLE_EVERY="${RALPH_ORACLE_EVERY:-10}"

# Pacing & runaway brakes
STALL_AFTER="${RALPH_STALL_AFTER:-3}"          # halt after N identical responses
ITERATION_DELAY="${RALPH_ITERATION_DELAY:-2s}" # pause between iterations

# Log paths must be defined BEFORE VERIFY_CMD so the watchdog can reference them.
LOG="logs/ralph-$(date +%Y%m%d-%H%M%S).log"
EVENT_LOG="logs/ralph-events.log"
EVENT_JSONL="logs/ralph-events.jsonl"

# Verify command run after each iteration. Override via env for a faster gate.
# Default keeps backend + frontend in sync because UCs touch both.
#
# We also wrap the user's verify command in a *promise watchdog* belt-and-braces:
# Ralph >=0.2 stops the loop on <promise>, but if an older binary or a stall
# leaks promise text into a verify-only iteration, exit 99 forces termination
# via --stop-on-error 1.
USER_VERIFY_CMD='set -e
  (cd backend  && ./mvnw -q -B test) \
  && (cd frontend && pnpm -s test --run) \
  && (cd frontend && pnpm -s build)'
USER_VERIFY_CMD="${RALPH_VERIFY_CMD:-$USER_VERIFY_CMD}"
VERIFY_CMD="set -e
# Promise watchdog (belt-and-braces; Ralph >=0.2 already stops on <promise>).
# We only inspect AIResponseEvent text in the structured JSONL stream so the
# guard cannot trip on the prompt template, tool-call echoes or our own grep
# results that happen to mention the literal phrase.
if [ -f '$EVENT_JSONL' ] && command -v python3 >/dev/null 2>&1; then
  if python3 -c \"
import json, sys
needle = '<promise>${PROMISE}</promise>'
with open('$EVENT_JSONL') as fh:
    for line in fh:
        try:
            o = json.loads(line)
        except Exception:
            continue
        if o.get('type') != 'AIResponseEvent':
            continue
        ev = o.get('event') or {}
        for v in ev.values():
            if isinstance(v, str) and needle in v:
                print(needle); sys.exit(0)
sys.exit(1)
\" >/dev/null; then
    echo \"verify: promise <${PROMISE}> emitted in $EVENT_JSONL — terminating loop.\" >&2
    exit 99
  fi
fi
${USER_VERIFY_CMD}"
VERIFY_TIMEOUT="${RALPH_VERIFY_TIMEOUT:-20m}"
ITER_TIMEOUT="${RALPH_ITER_TIMEOUT:-45m}"
MAX_ITER="${RALPH_MAX_ITER:-120}"
TOTAL_TIMEOUT="${RALPH_TIMEOUT:-10h}"

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

# --- arg parsing -------------------------------------------------------------
RESET=0
for arg in "$@"; do
  case "$arg" in
    --reset) RESET=1 ;;
    -h|--help) grep -E '^# ' "$0" | sed 's/^# \{0,1\}//'; exit 0 ;;
    *) echo "unknown arg: $arg" >&2; exit 2 ;;
  esac
done

# --- preflight ---------------------------------------------------------------
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

# Ensure the dedicated branch exists / is checked out.
git rev-parse --verify "$BRANCH" >/dev/null 2>&1 \
  && git checkout "$BRANCH" \
  || git checkout -b "$BRANCH"

# Optional: reset stale checkpoint.
if [[ $RESET -eq 1 && -f $CHECKPOINT ]]; then
  ralph reset --force --checkpoint-file "$CHECKPOINT" || rm -f "$CHECKPOINT"
fi

# --- shared flags ------------------------------------------------------------
COMMON_FLAGS=(
  --model               "$MODEL"
  --oracle-model        "$ORACLE_MODEL"
  --oracle-every        "$ORACLE_EVERY"
  --oracle-on-verify-fail
  --max-iterations      "$MAX_ITER"
  --timeout             "$TOTAL_TIMEOUT"
  --iteration-timeout   "$ITER_TIMEOUT"
  --iteration-delay     "$ITERATION_DELAY"
  --promise             "$PROMISE"
  --blocked-phrase      "$BLOCKED_PHRASE"
  --system-prompt       "$BUNDLE"
  --system-prompt-mode  append
  --plan-file           "$PLAN_FILE"
  --specs               docs/use_cases
  --checkpoint-file     "$CHECKPOINT"
  --verify-cmd          "$VERIFY_CMD"
  --verify-timeout      "$VERIFY_TIMEOUT"
  --auto-commit
  --auto-commit-message "ralph(uc): iteration %d"
  --auto-commit-on-failure=false
  --auto-tag            "ralph/iter-%d"
  --diff-stat
  --stop-on-no-changes  3
  --stop-on-error       1
  --stall-after         "$STALL_AFTER"
  --carry-context       off
  --log-file            "$EVENT_LOG"
  --json-output         "$EVENT_JSONL"
  --on-complete         "echo '[ralph] loop complete after \$RALPH_ITERATIONS iterations (state=\$RALPH_STATE)'"
  --on-blocked          "echo '[ralph] BLOCKED at iteration \$RALPH_ITERATIONS — see $PLAN_FILE' >&2"
)

# Decide run vs resume based on checkpoint presence.
if [[ -f $CHECKPOINT ]]; then
  echo "Resuming from checkpoint: $CHECKPOINT"
  RALPH_CMD=(ralph resume "${COMMON_FLAGS[@]}")
else
  echo "Starting fresh run."
  RALPH_CMD=(ralph run "${COMMON_FLAGS[@]}" "$PROMPT")
fi

echo "Branch:        $(git rev-parse --abbrev-ref HEAD)"
echo "Model:         $MODEL"
echo "Oracle model:  $ORACLE_MODEL  (every ${ORACLE_EVERY} iters + on verify failure)"
echo "Verify cmd:    ${VERIFY_CMD%%$'\n'*} ..."
echo "Log:           $LOG"

# --- run ---------------------------------------------------------------------
set +e
"${RALPH_CMD[@]}" 2>&1 | tee "$LOG"
EXIT="${PIPESTATUS[0]}"
set -e

if [[ $EXIT -eq 0 ]] && grep -q "<promise>${PROMISE}</promise>" "$LOG"; then
  echo "SUCCESS: <promise>${PROMISE}</promise>"
else
  echo "INCOMPLETE (exit=$EXIT). See $LOG, $EVENT_LOG, $EVENT_JSONL and $PLAN_FILE"
  echo "Resume with: $0    (checkpoint: $CHECKPOINT)"
fi

git status --short
exit "$EXIT"
