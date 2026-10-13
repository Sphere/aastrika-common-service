-- Failure reason for competency certificates, as "CODE: text" (e.g. "RC_UNAVAILABLE: certificate
-- service not reachable"); set when status is FAILED, cleared on success. Holds fixed messages only,
-- never raw exception text, since the status API returns it (SECURITY.md 2.6).
-- For tables created before the column was added to user_competency_certificate.sql.
-- Run manually per environment (spring.jpa.hibernate.ddl-auto is none — see TRIAGE T-002).
ALTER TABLE user_competency_certificate ADD COLUMN IF NOT EXISTS message varchar(512);