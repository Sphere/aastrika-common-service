-- Competency certificate bookkeeping: one row per (user, competency).
-- Run manually per environment (spring.jpa.hibernate.ddl-auto is none — see TRIAGE T-002).
CREATE TABLE IF NOT EXISTS user_competency_certificate (
    user_id         varchar(64)  NOT NULL,
    competency_id   varchar(64)  NOT NULL,
    competency_name varchar(256),
    root_org_id     varchar(64)  NOT NULL,
    template_id     varchar(128),
    event_id        uuid         NOT NULL,
    status          varchar(16)  NOT NULL,
    certificate_url text,
    message         varchar(512),
    completed_at    timestamptz  NOT NULL,
    updated_at      timestamptz  NOT NULL,
    CONSTRAINT pk_user_competency_certificate PRIMARY KEY (user_id, competency_id),
    CONSTRAINT ck_user_competency_certificate_status CHECK (status IN ('PENDING', 'GENERATED', 'FAILED'))
);

-- Status/download filter by rootOrgId or competencyId alone; the PK only serves user_id lookups.
CREATE INDEX IF NOT EXISTS idx_user_competency_certificate_org ON user_competency_certificate (root_org_id);
CREATE INDEX IF NOT EXISTS idx_user_competency_certificate_competency ON user_competency_certificate (competency_id);