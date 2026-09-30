# Triage Report — Aastrika Common Service
**Branch:** filtered-api-mig
**Stack:** Java 21 · Spring Boot 3.4.2 · Maven · PostgreSQL / Cassandra · Kafka
**Last reviewed:** 2026-08-04

---

## Summary

| Severity | Open | Completed |
|---|---|---|
| Critical | 0 | 1 |
| High | 1 | 1 |
| Medium | 1 | 0 |
| Low | 1 | 1 |

Items tagged `[security]` are security-relevant and referenced from `SECURITY.md`.
"Completed" here means a guardrail whose automated check currently passes (see `config/triage-checks.conf`).

---

## Critical

### T-001 — Tomcat CVEs patched via version override `[security]`
**Severity:** Critical
**Area:** Security / Infrastructure

`pom.xml` overrides the Tomcat version to patch CVE-2025-24813, CVE-2025-31651, CVE-2025-55754.

**File:** `pom.xml:33` — `<tomcat.version>10.1.36</tomcat.version>`
**Guardrail:** keep the override present; check `T-001` fails if it is removed or downgraded below 10.x.

---

## High

### T-002 — `ddl-auto` must not auto-alter schema `[security]`
**Severity:** High
**Area:** Database Safety

`spring.jpa.hibernate.ddl-auto` defaults to `none`, so Hibernate does not auto-alter the schema on
startup. Schema changes must go through explicit migrations.

**File:** `src/main/resources/application.properties:30`
**Guardrail:** check `T-002` fails if the default becomes `update`.

### T-004 — `sb.api-key` ships a weak default `apiKey` `[security]`
**Severity:** High
**Area:** Secrets / Config

`sb.api-key` falls back to the literal `apiKey` when `SB_API_KEY` is unset. This key is sent as the
`Authorization` header to the course/LMS service — a weak default risks an unauthenticated-looking
call succeeding in a misconfigured environment. The weak default appears in **two** places; both must
be cleaned:

**Files:**
- `src/main/resources/application.properties:82` — `sb.api-key=${SB_API_KEY:apiKey}`
- `src/main/java/org/aastrika/client/CourseClient.java:47` — `@Value("${sb.api-key:apiKey}")` (embedded fallback; used as the raw `Authorization` header at `CourseClient.java:181`)

**Fix:** remove the `:apiKey` fallback in both spots (`${SB_API_KEY}` / `@Value("${sb.api-key}")`), or
fail fast when the key is blank in non-local environments.

**Note:** the automated check (`config/triage-checks.conf` T-004) only greps `application.properties`,
so the `CourseClient.java` occurrence must be cleaned manually — the check can pass while it remains.

---

## Medium

### T-005 — Cassandra connects without credentials `[security]`
**Severity:** Medium
**Area:** Security / Config

`spring.cassandra.username` / `password` are commented out, so the service connects unauthenticated
unless the cluster enforces auth. Silent misconfiguration.

**File:** `src/main/resources/application.properties:100-101`
**Fix:** enforce non-empty Cassandra credentials in staging/production via deployment config.

---

## Low

### T-003 — SQL logging off by default
**Severity:** Low
**Area:** Observability / Performance

`spring.jpa.show-sql` defaults to `false`, so SQL is not logged by default.

**File:** `src/main/resources/application.properties:31`
**Guardrail:** check `T-003` fails if `show-sql=true` is hardcoded.

### T-006 — DB credentials default to `postgres:postgres` `[security]`
**Severity:** Low
**Area:** Security / Config

`spring.datasource.username/password` default to `postgres:postgres` — acceptable locally, must be
overridden everywhere else.

**File:** `src/main/resources/application.properties:19-20`
**Fix:** ensure production/staging set `DATABASE_USERNAME` / `DATABASE_PASSWORD`.

---

## Action Priority

<!-- `scripts/check-triage.sh --update` rewrites the Status cell of a row when that row's matching
     check in config/triage-checks.conf passes. Keep each "| T-00X |" id and the "| Open |" text. -->

> T-001, T-002, and T-003 are already satisfied — run `bash scripts/check-triage.sh --update` to
> flip their Status to "✅ Completed". T-004 stays Open (real fix needed); T-005/T-006 need a human
> (deployment config), so they stay Open.

| # | Item | Owner | Priority | Status |
|---|---|---|---|---|
| T-001 | Keep Tomcat CVE override in pom.xml | Backend / DevOps | Guardrail | ✅ Completed 2026-08-04 |
| T-002 | Keep ddl-auto off `update` default | Backend | Guardrail | ✅ Completed 2026-08-04 |
| T-003 | Keep SQL logging off by default | Backend | Guardrail | ✅ Completed 2026-08-04 |
| T-004 | Remove weak `sb.api-key` default | Backend / Infra | This sprint | Open |
| T-005 | Enforce Cassandra credentials in prod | Backend / Infra | Backlog | Open |
| T-006 | Ensure prod overrides DB credentials | Backend / Infra | Backlog | Open |
