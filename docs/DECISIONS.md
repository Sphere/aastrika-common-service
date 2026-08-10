# Decisions — {{PROJECT_NAME}}

Rules and lessons learned from real mistakes. Read these before implementing anything.

These first seven are universal lessons about keeping `CLAUDE.md` truthful and useful — they apply
to any project. Keep them, and append your own project-specific lessons as `L-008`, `L-009`, …

---

## L-001 — Verify CLAUDE.md against the actual code before enforcing any rule

**Mistake:** `CLAUDE.md` described an older state of the project (an old framework version, an old
import style, a since-changed convention). The code had already moved on, but the stale rules were
enforced anyway — producing output that did not compile or did not match the codebase.

**Rule:** Before writing new code or reviewing existing code, grep the actual files to confirm what
is in use. Never trust a rule in `CLAUDE.md` without checking the code first. If a rule conflicts
with what the code does, stop and flag the conflict — do not silently pick one.

---

## L-002 — When a migration is executed, update CLAUDE.md in the same change

**Mistake:** A migration was completed in the code (dependency swapped, config changed) but
`CLAUDE.md` still called it "pending" and kept the old rules. Every later change then used the wrong
baseline.

**Rule:** `CLAUDE.md` is only useful if it reflects the current state. After any significant change —
dependency upgrade, migration, breaking refactor — update the rules, the tech-stack table, and the
pending-work list in the same change.

---

## L-003 — "Do NOT" rules must be pruned when their condition no longer applies

**Mistake:** A "Do NOT use X" rule was correct for an old framework version and wrong after an
upgrade. It was never removed, so it kept steering work in the wrong direction long after it stopped
being true.

**Rule:** Every "Do NOT" rule must state its condition. When the condition changes (e.g. a version
upgrade), revisit all "Do NOT" entries and remove or rewrite any that no longer apply.

---

## L-004 — "Removed" in the notes does not mean removed if the code says otherwise

**Mistake:** The notes said a field had been removed from a model. The field was still present in the
source. The note was treated as truth without checking the file.

**Rule:** Release notes and changelogs describe intent, not ground truth. Always read the actual
source to confirm a stated change was applied. If the note and the code disagree, the code wins.

---

## L-005 — Dead rules about deleted files cause confusion

**Mistake:** A rule guarded a file ("do not delete X until confirmed unused") that had already been
deleted. The rule lingered and created false caution about a file that no longer existed.

**Rule:** When a file is deleted, remove every rule that references it. A rule guarding a file that
does not exist cannot be acted on and only misleads future sessions.

---

## L-006 — Docs need a reference in CLAUDE.md to be AI-enforced

**Learning:** Only `CLAUDE.md` is loaded automatically. Companion documents like `SECURITY.md` and
`docs/TRIAGE.md` are not read unless `CLAUDE.md` explicitly tells the agent to read them — otherwise
they are human-facing only.

**Rule:** Any document whose rules the agent should enforce must be referenced from `CLAUDE.md` with
a clear instruction, e.g. "Before security-related work, read `SECURITY.md` for scope and policy."

---

## L-007 — Flag conflicts before implementing, never resolve them silently

**Learning:** When `CLAUDE.md` contradicts the code, silently picking one side produces incorrect
output and erodes trust.

**Rule:** If a rule in `CLAUDE.md` conflicts with the actual state of the code, stop and tell the
user before writing any code. State what the rule says, what the code shows, and which is likely
correct. Let the user decide.
