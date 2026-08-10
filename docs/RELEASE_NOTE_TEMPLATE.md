# Release Note Template — {{PROJECT_NAME}}

> Copy this file for each release (or let `scripts/update-release-notes.sh` draft it for you).
> Fill in each section, remove sections that don't apply, and delete all instructional
> comments before publishing.

---

## [VERSION] — YYYY-MM-DD

### Summary
<!-- One or two sentences: what is this release and why does it matter? -->

---

### Security
<!-- CVE patches, dependency upgrades driven by vulnerabilities, auth/credential changes.
     Include CVE IDs where applicable. -->
- [ ] Item

---

### Added
<!-- New features, endpoints, config options, or capabilities introduced in this release. -->
- [ ] Item

---

### Changed
<!-- Modifications to existing behaviour, API contracts, config defaults, or dependencies.
     State what changed FROM and TO where relevant. -->
- [ ] Item

---

### Fixed
<!-- Bug fixes. Reference a TRIAGE.md item ID (e.g. T-003) where applicable. -->
- [ ] Item

---

### Removed
<!-- Deleted files, dropped columns, deprecated endpoints removed, dependencies removed. -->
- [ ] Item

---

### Known Issues
<!-- Open items NOT resolved in this release. Reference TRIAGE.md IDs. -->
- [ ] Item (see TRIAGE.md T-XXX)

---

### Migration Notes
<!-- Steps required when upgrading from the previous version. -->

#### Environment Variables
<!-- New, renamed, or removed env vars. -->
| Variable | Change | Action Required |
|---|---|---|
| `EXAMPLE_VAR` | Added | Set in deployment config |

#### Data / Schema
<!-- Migration scripts to run, if any. -->

#### Config Changes
<!-- Properties, flags, or settings added, renamed, or removed. -->

---

### Deployment Checklist
<!-- Tick each item before marking this release deployed. Adapt to your stack. -->
- [ ] Data / schema migrations executed
- [ ] Environment variables updated in deployment config
- [ ] Dangerous defaults overridden for production
- [ ] Application builds and passes a smoke test
- [ ] Dependency vulnerability scan is clean
- [ ] `docs/TRIAGE.md` updated — run `bash scripts/check-triage.sh --update`

---

### Contributors
- @your-handle
