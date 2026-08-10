#!/usr/bin/env bash
# guard-git-writes.sh — PreToolUse hook for Claude Code.
# Blocks any Bash tool call containing git write commands.
# Exit 2 causes Claude Code to reject the tool call entirely.
#
# Runs only when the agent attempts a Bash tool call — not on every message.
# Performance: ~5-10ms per Bash call (shell startup + one grep).
#
# FRAMEWORK FILE — language-agnostic. Copy verbatim; do not add project values.

INPUT=$(cat)
COMMAND=$(echo "$INPUT" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('command',''))" 2>/dev/null || \
          echo "$INPUT" | grep -o '"command":"[^"]*"' | sed 's/"command":"//;s/"$//')

GIT_WRITE_PATTERN='git (commit|push|tag|merge|rebase|reset|branch -[dD]|rm |mv |restore|switch -c|checkout -b)'

if echo "$COMMAND" | grep -qE "$GIT_WRITE_PATTERN"; then
  echo "" >&2
  echo "╔════════════════════════════════════════════════════════╗" >&2
  echo "║  BLOCKED — no AI agent should run git write commands   ║" >&2
  echo "╚════════════════════════════════════════════════════════╝" >&2
  echo "" >&2
  echo "  Run this yourself:" >&2
  echo "  $COMMAND" >&2
  echo "" >&2
  exit 2
fi
