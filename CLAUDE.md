# Claude Code Rules — Aastrika Common Service

## Meta Rules
- Read `docs/DECISIONS.md` before implementing any code change.
- Before any security-related work — dependency changes, vulnerability assessment, logging,
  error handling, schema changes — read `SECURITY.md` for scope, rules, and current triage status.
- Before any major change — new feature, dependency upgrade, schema change, API contract change —
  check `docs/TRIAGE.md` for open items tagged `[security]` or matching the area being changed.
  Flag any relevant open items to the user before implementing.
- At the end of a session where significant code was changed, ask the user:
  "Should I draft a CHANGELOG entry?" If yes, run `bash scripts/update-release-notes.sh <version>`
  using `docs/RELEASE_NOTE_TEMPLATE.md` as the structure. Do not apply automatically; show the draft first.
- If any rule in this file conflicts with the actual code, stop and flag the conflict to the user
  before proceeding.
- **HARD RULE — No git operations:** Never run `git commit`, `git push`, `git tag`, `git merge`,
  `git rebase`, `git reset`, or any command that writes to the repository. Provide the exact command
  as a suggestion for the user to run themselves. This rule has no exceptions.

---

## Project Overview

Spring Boot common service exposing several cross-cutting capabilities for the Aastrika platform:
- **Passbook** — read/update a user's competency passbook records.
- **Ratings** — upsert emits a Kafka event; an in-service consumer aggregates ratings (replacing an
  external Flink job); plus summary and paginated-review reads.
- **Cohorts** — active-users and auto-enrollment batches.
- **Assessment** — assessment submission with external user/content validation.
- **Public search** — unauthenticated course search over the composite-search Elasticsearch index
  (`POST /publicSearch/getcourse`), migrated from the course-recommendation service.
- **User migration** — admin org migration (`PATCH /user/v1/migrate`): learner-service migration,
  then a `profiledetails` rewrite, PUBLIC role assignment and a data-sync. Not atomic — the
  learner-service step is irreversible once it succeeds.

Persists to **PostgreSQL** (JPA) and **Cassandra** (Spring Data), and produces/consumes events over
**Kafka**. Integrates with external content, user, and course services over HTTP.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.4.2 |
| Build | Maven |
| ORM | Hibernate 6 / Spring Data JPA |
| Data stores | PostgreSQL · Cassandra |
| Messaging | Apache Kafka (spring-kafka) |
| Mapping | MapStruct 1.6.3 (on classpath) + Lombok |
| API docs | springdoc-openapi-starter-webmvc-ui 2.8.4 |
| Server | Tomcat 10.1.36 (CVE override in `pom.xml`) |

---

## Package / Project Structure

Base package: `org.aastrika`

```
org.aastrika
├── controller/     # REST controllers — thin, no business logic
├── service/        # service interfaces
│   └── impl/       # service implementations
├── repository/     # Spring Data JPA repositories
├── dao/            # data-access helpers
│   └── impl/
├── entity/         # JPA entities (PostgreSQL)
├── model/          # (currently empty — see TRIAGE)
├── dto/
│   ├── request/    # inbound DTOs
│   ├── response/   # outbound DTOs (AppResponse<T> wrapper lives here)
│   └── event/      # Kafka event payloads
├── messaging/      # Kafka producer + rating aggregation consumer
├── client/         # HTTP clients for external services
├── config/         # Spring config beans
├── exception/      # custom exceptions + handler
├── common/         # constants (Constants.X_AUTH_USER_ID, …)
└── util/           # stateless utilities
```

---

## Code Generation Rules

- Use `jakarta.*` imports — **never** `javax.*` (Spring Boot 3.4.2 / Jakarta EE baseline).
- Controllers are thin: `@RestController`, **constructor-inject the service interface** (not the impl),
  return `ResponseEntity<AppResponse<T>>`, and contain no business logic.
  **One documented exception:** `PublicSearchController` (`POST /publicSearch/getcourse`) returns the
  raw `CourseSearchResponse` — it is a drop-in replacement for the recommendation service's endpoint,
  so its body must stay byte-compatible for existing callers. Do not add the envelope there; do not
  copy the exception to new endpoints.
- User identity arrives as the `x-authenticated-userid` request header (`Constants.X_AUTH_USER_ID`).
  Admin/token-forwarding endpoints instead use `x-authenticated-user-token` + `Authorization`
  (`Constants.X_AUTH_TOKEN` / `AUTH_TOKEN`).
  Authentication/authorisation is handled upstream (gateway) — this service trusts the header.
- Validate request bodies with `jakarta.validation` (`@Valid` on `@RequestBody`).
- Lombok is used across DTOs/entities. **MapStruct is on the classpath but currently unused**
  (no `@Mapper` in the codebase) — do not assume mappers exist; if you add one, the annotation
  processor is already configured in `pom.xml`.
- Persistence: JPA entities in `org.aastrika.entity` → PostgreSQL. Cassandra via Spring Data
  (the `CqlSession` connects at startup — the keyspace must already exist).
- Search: both search paths point at the same **OpenSearch** cluster — `composite-search.url` for
  the public course search and `sb.lern.les.host.list` for user autocomplete (they share a default of
  `localhost:9201`; override both per environment). Autocomplete goes through the OpenSearch
  `RestHighLevelClient` built in `OpenSearchConfig`. `CompositeSearchClient` instead sends raw JSON
  over the shared `RestTemplate`, reading only `_source` and a hit count; its `readTotal` accepts both
  `hits.total` shapes — the bare number of Elasticsearch 6.x and the `{value, relation}` object of
  Elasticsearch 7+/OpenSearch — so it is version-agnostic. Keep that if you touch it.
  (An earlier version of this bullet claimed the cluster was Elasticsearch 6.8. That was inherited
  from the course-recommendation service this endpoint was migrated from and is wrong — do not
  re-add it.)
- Messaging: the rating consumer is single-threaded over a single-partition topic by design.

---

## Do NOT

- **Do not** use `javax.*` imports — while on Spring Boot 3.x the baseline is `jakarta.*`.
- **Do not** put business logic in controllers, and **do not** add `@Transactional` to controllers.
- **Do not** change the rating consumer's `spring.kafka.consumer.auto-offset-reset=latest`, nor raise
  `spring.kafka.listener.concurrency` above `1` — the aggregation is delta-based / non-idempotent over
  a single partition; replaying the backlog or parallelising it double-counts. (Condition: while the
  aggregation remains delta-based.)
- **Do not** default `spring.jpa.hibernate.ddl-auto` to `update` in shared/production environments —
  it currently defaults to `none`; keep schema changes in explicit migrations.
- **Do not** ship real secrets as property defaults — `sb.api-key` currently defaults to `apiKey`,
  which is a placeholder only and must be overridden per environment (see TRIAGE T-004).
