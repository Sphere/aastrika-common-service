# Changelog

All notable changes to this project are documented here. Newest first.

## [0.1.0] — 2026-08-10 · [Full Release Note](docs/release-notes/v0.1.0.md)

Removes three endpoints confirmed unused by the endpoint-usage audit and makes the rating
aggregation's silent failure mode visible. Redis is no longer a runtime dependency. Endpoint count
drops from 15 to 13; the four in-use rating endpoints and all persistence behaviour are unchanged.

- **Security:** consumer log lines exclude user ids (`SECURITY.md` 2.5); Redis starter dropped from `pom.xml`, Tomcat CVE override (T-001) untouched
- **Added:** rating consumer logs why an event was dropped instead of failing silently to the dead-letter topic
- **Added:** Postman `R10` documents the enrolment gate — a 200 upsert whose aggregation is dropped
- **Changed:** `ContentClient` is now read-only (6 constructor params → 3); `RatingServiceImpl` 8 → 5
- **Changed:** Postman rating requests retargeted at a verified-enrolled user/activity pair so the summary can actually populate
- **Removed:** `GET /v2/resources/{resourceId}/user/{userUUID}/cohorts/top-performers` — **breaking**, now 404
- **Removed:** `POST /ratings/meta/update` and `POST /update/v1/content/additionaltag` — **breaking**, now 404
- **Removed:** `client/TopPerformerDao.java`, `client/RatingTagRedisReader.java`, three `ContentClient` methods
- **Removed:** `spring-boot-starter-data-redis` and all Redis config — Redis no longer needs provisioning

---
