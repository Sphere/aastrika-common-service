#!/usr/bin/env bash
# check-triage.sh — generic, data-driven triage checker.
#
# Reads config/triage-checks.conf (a list of `check` calls) and verifies each
# item against the actual repository. Optionally flips completed items in the
# Action Priority table of docs/TRIAGE.md.
#
# FRAMEWORK FILE — language-agnostic. You never edit this script; you edit
# config/triage-checks.conf. Works for any project (Java, Node, Python, Go, ...).
#
# Usage:
#   bash scripts/check-triage.sh            # report only
#   bash scripts/check-triage.sh --update   # report + mark completed rows in docs/TRIAGE.md
#
# Config lines (config/triage-checks.conf), one check each:
#   check <ID> <type> <target> <pattern> <note>
#
# Types:
#   grep_present  ✅ DONE if <pattern> (extended regex, grep -E) is found in <target>
#   grep_absent   ✅ DONE if <pattern> is NOT found in <target>   (missing file = DONE)
#   file_present  ✅ DONE if the <target> glob matches ≥1 file
#   file_absent   ✅ DONE if the <target> glob matches 0 files
#   manual        ⚠ SKIP always — a human must verify
#
# <target>  path relative to repo root. For file_* checks: a bare name
#           (e.g. Foo.java) is searched anywhere; a value with "/" is a glob
#           where "*"/"**" cross directories (find -path semantics).
# <pattern> POSIX extended regex (grep -E). Use '' when the type needs none.
# <note>    short human description shown in the report.

set -uo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TRIAGE="$ROOT/docs/TRIAGE.md"
CONFIG="$ROOT/config/triage-checks.conf"
UPDATE=false
[[ "${1:-}" == "--update" ]] && UPDATE=true

GREEN="\033[0;32m"; RED="\033[0;31m"; YELLOW="\033[0;33m"; RESET="\033[0m"; BOLD="\033[1m"
pass() { echo -e "  ${GREEN}✅ DONE${RESET}  $1"; }
fail() { echo -e "  ${RED}⬜ OPEN${RESET}  $1"; }
skip() { echo -e "  ${YELLOW}⚠  SKIP${RESET}  $1"; }

declare -A STATUSES
ORDER=()

# Portable in-place sed (GNU vs BSD/macOS).
_sed_inplace() {
  if sed --version >/dev/null 2>&1; then sed -i "$@"; else sed -i '' "$@"; fi
}

# True if the glob matches at least one file under ROOT (ignores .git).
_glob_matches() {
  local glob="$1" hit
  if [[ "$glob" == */* ]]; then
    local norm="${glob//\*\*/\*}"
    hit=$( cd "$ROOT" && find . -path "./$norm" -not -path '*/.git/*' 2>/dev/null | head -1 )
  else
    hit=$( cd "$ROOT" && find . -name "$glob" -not -path '*/.git/*' 2>/dev/null | head -1 )
  fi
  [[ -n "$hit" ]]
}

# The function the config file calls once per triage item.
check() {
  local id="$1" type="$2" target="${3:-}" pattern="${4:-}" note="${5:-}"
  ORDER+=("$id")
  echo -e "${BOLD}${id}${RESET} ${note}"
  case "$type" in
    grep_present)
      if [[ -z "$pattern" ]]; then
        skip "no pattern given — treated as manual"; STATUSES["$id"]="skip"
      elif [[ -f "$ROOT/$target" ]] && grep -qE "$pattern" "$ROOT/$target" 2>/dev/null; then
        pass "found in $target"; STATUSES["$id"]="done"
      else
        fail "not found in $target"; STATUSES["$id"]="open"
      fi ;;
    grep_absent)
      if [[ -z "$pattern" ]]; then
        skip "no pattern given — treated as manual"; STATUSES["$id"]="skip"
      elif [[ -f "$ROOT/$target" ]] && grep -qE "$pattern" "$ROOT/$target" 2>/dev/null; then
        fail "still present in $target"; STATUSES["$id"]="open"
      else
        pass "absent from $target"; STATUSES["$id"]="done"
      fi ;;
    file_present)
      if _glob_matches "$target"; then
        pass "file matches $target"; STATUSES["$id"]="done"
      else
        fail "no file matches $target"; STATUSES["$id"]="open"
      fi ;;
    file_absent)
      if _glob_matches "$target"; then
        fail "file still matches $target"; STATUSES["$id"]="open"
      else
        pass "no file matches $target"; STATUSES["$id"]="done"
      fi ;;
    manual)
      skip "${note:-needs manual check}"; STATUSES["$id"]="skip" ;;
    *)
      skip "unknown check type '$type' — treated as manual"; STATUSES["$id"]="skip" ;;
  esac
}

# ── Run ─────────────────────────────────────────────────────────────────────
if [[ ! -f "$CONFIG" ]]; then
  echo "Error: config not found at $CONFIG"
  echo "Copy claude-kit/config/triage-checks.conf into config/ and add your checks."
  exit 1
fi

echo ""
echo -e "${BOLD}=== Triage Status Check — $(date '+%Y-%m-%d') ===${RESET}"
echo ""

# shellcheck disable=SC1090
source "$CONFIG"

# ── Summary ──────────────────────────────────────────────────────────────────
echo ""
echo -e "${BOLD}=== Summary ===${RESET}"
DONE_COUNT=0; OPEN_COUNT=0; SKIP_COUNT=0
for id in "${ORDER[@]:-}"; do
  [[ -z "$id" ]] && continue
  case "${STATUSES[$id]}" in
    done) DONE_COUNT=$((DONE_COUNT + 1)) ;;
    open) OPEN_COUNT=$((OPEN_COUNT + 1)) ;;
    skip) SKIP_COUNT=$((SKIP_COUNT + 1)) ;;
  esac
done
echo -e "  ${GREEN}Completed: $DONE_COUNT${RESET} | ${RED}Open: $OPEN_COUNT${RESET} | ${YELLOW}Needs manual check: $SKIP_COUNT${RESET}"
echo ""

# ── Optional TRIAGE.md update ────────────────────────────────────────────────
if [[ "$UPDATE" == true ]]; then
  if [[ ! -f "$TRIAGE" ]]; then
    echo "⚠  $TRIAGE not found — nothing to update."
    exit 0
  fi
  echo -e "${BOLD}Updating docs/TRIAGE.md Action Priority table...${RESET}"
  TODAY=$(date '+%Y-%m-%d')
  for id in "${ORDER[@]:-}"; do
    [[ -z "$id" ]] && continue
    if [[ "${STATUSES[$id]}" == "done" ]]; then
      # On the table row containing this ID, replace "| Open |" with a completed date.
      _sed_inplace "/| $id /s/| Open |/| ✅ Completed $TODAY |/" "$TRIAGE"
    fi
    # "open" and "skip" rows are left unchanged.
  done
  echo -e "  ${GREEN}docs/TRIAGE.md updated.${RESET}"
  echo ""
fi
