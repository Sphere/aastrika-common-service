-- Competency name (from the passbook's additionalParams.competencyName), returned by the status and
-- download APIs. Null when the passbook carries no name.
-- For tables created before the column was added to user_competency_certificate.sql.
-- Run manually per environment (spring.jpa.hibernate.ddl-auto is none — see TRIAGE T-002).
ALTER TABLE user_competency_certificate ADD COLUMN IF NOT EXISTS competency_name varchar(256);