# Security Policy — Aastrika Common Service

**Last updated:** 2026-08-04
**Service:** Spring Boot common service — passbook, ratings aggregation, cohorts, assessment
**Stack:** Java 21 · Spring Boot 3.4.2 · Maven · PostgreSQL / Cassandra · Kafka

---

## 1. Scope

### In Scope
- All REST API endpoints exposed by this service (passbook, ratings, cohorts, assessment).
- Kafka producer (rating events) and the rating-aggregation consumer.
- Data-access layers: PostgreSQL (JPA), Cassandra (Spring Data).
- Outbound HTTP integrations (content, user, and course services).
- All runtime dependencies declared in `pom.xml`.
- Configuration and secrets handling (`application.properties`, environment variables).
- Logging output — what is logged and at what level.

### Out of Scope
- Cluster/infra for PostgreSQL, Cassandra, and Kafka (managed by infra).
- Network perimeter, load balancer, API gateway.
- **Authentication / authorisation** — not implemented here; the `X-Auth-User-Id` header is assumed
  to be set by a trusted upstream gateway.
- The external content/user/course services this service calls.

### Environments Covered
| Environment | Notes |
|---|---|
| Production | All rules apply strictly |
| Staging | Apply the same rules as production |
| Development / Local | Relaxed only where explicitly noted (default local credentials) |

---

## 2. Security Rules

### 2.1 Dependency Vulnerabilities
- Versions are managed by the Spring Boot 3.4.2 parent BOM; pin explicit versions only where needed.
- Tomcat CVE override `<tomcat.version>10.1.36</tomcat.version>` is active in `pom.xml` (CVE-2025-24813,
  CVE-2025-31651, CVE-2025-55754) — do not remove or downgrade without checking the patched version.
- Review dependencies on every PR that touches `pom.xml`.

### 2.2 Secrets Management
- No credentials in source or in `application.properties` defaults. All sensitive values come from
  environment variables: `DATABASE_*`, `DATA_LAKE_DB_*`, `SB_API_KEY`, `KAFKA_BOOTSTRAP_SERVERS`,
  `CASSANDRA_*`.
- Local defaults (`postgres:postgres`, `sb.api-key=apiKey`) are for development only — production
  **must** override all of them. `sb.api-key`'s `apiKey` default is a placeholder (TRIAGE T-004).
- Never commit `.env` files or real credentials.

### 2.3 Input Validation
- All request-body DTOs validated with `jakarta.validation` (`@Valid` on `@RequestBody`).
- The `X-Auth-User-Id` header is required on user-scoped endpoints; treat a missing/blank value as an
  authorization failure, not a NullPointerException.

### 2.4 Data Layer Safety
- `spring.jpa.hibernate.ddl-auto` defaults to `none` — keep it `none`/`validate` outside local; do
  schema changes via explicit migrations, never Hibernate auto-DDL.
- Cassandra `CqlSession` connects at startup; the keyspace must exist. Enforce credentials in prod
  (TRIAGE T-005).
- Use parameterised binding for all native/CQL queries — no string concatenation into queries.

### 2.5 Logging Policy
- Never log PII (user IDs that map to individuals, emails) or secrets/tokens at any level.
- `spring.jpa.show-sql` defaults to `false` — keep it `false` in production.
- Do not hardcode `DEBUG` for data/search/kafka packages in `application.properties`.

### 2.6 Error Handling
- All exceptions go through the exception handler in `org.aastrika.exception` — never let raw stack
  traces or internal messages (SQL, file paths, host names) reach an API response.
- Error responses use the `AppResponse` wrapper.

---

## 3. Security Triage

All issues — security and non-security — are tracked in `docs/TRIAGE.md` as the single source of
truth. Security-relevant items are tagged `[security]` there; status and fixes are updated only in
`docs/TRIAGE.md`.

**Current security items in TRIAGE.md:**

| TRIAGE ID | Severity | Summary |
|---|---|---|
| T-001 | Critical | Tomcat CVE override present in `pom.xml` |
| T-002 | High | `ddl-auto` does not default to `update` |
| T-004 | High | `sb.api-key` ships a weak default `apiKey` |
| T-005 | Medium | Cassandra credentials commented out — unauthenticated connect |
| T-006 | Low | DB credentials default to `postgres:postgres` |

---

## 4. Security Release Notes

Append an entry here each time a security item is resolved or introduced.

### 2026-08-04
- **Added:** This `SECURITY.md` as the authoritative security policy for Aastrika Common Service.
- **Identified:** T-004 (weak `sb.api-key` default), T-005 (Cassandra unauthenticated), T-006 (local
  DB credential defaults) — see `docs/TRIAGE.md`.
