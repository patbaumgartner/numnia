-- UC-001: IAM module tables
-- Applied by Flyway against PostgreSQL 18+ (deferred: used in production).
-- For unit/integration tests, H2 create-drop is used (application-test.yaml).
-- See .ralph/usecase-progress.md for follow-up note on Postgres Testcontainers.

CREATE TABLE parent_accounts (
    id               UUID         NOT NULL PRIMARY KEY,
    email            VARCHAR(320) NOT NULL UNIQUE,
    hashed_password  VARCHAR(255) NOT NULL,
    first_name       VARCHAR(100) NOT NULL,
    salutation       VARCHAR(50)  NOT NULL,
    status           VARCHAR(30)  NOT NULL DEFAULT 'NOT_VERIFIED',
    privacy_consented BOOLEAN     NOT NULL DEFAULT FALSE,
    terms_accepted    BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE child_profiles (
    id                UUID        NOT NULL PRIMARY KEY,
    pseudonym         VARCHAR(100) NOT NULL,
    year_of_birth     INT          NOT NULL,
    avatar_base_model VARCHAR(100) NOT NULL,
    parent_id         UUID         NOT NULL REFERENCES parent_accounts(id),
    status            VARCHAR(30)  NOT NULL DEFAULT 'PENDING_CONFIRM',
    multiplayer_enabled BOOLEAN    NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE verification_tokens (
    id               UUID        NOT NULL PRIMARY KEY,
    parent_id        UUID        NOT NULL REFERENCES parent_accounts(id),
    child_profile_id UUID        REFERENCES child_profiles(id),
    purpose          VARCHAR(30) NOT NULL,
    expires_at       TIMESTAMPTZ NOT NULL,
    consumed_at      TIMESTAMPTZ,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE audit_log_entries (
    id          BIGSERIAL    NOT NULL PRIMARY KEY,
    timestamp   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    action      VARCHAR(60)  NOT NULL,
    parent_ref  VARCHAR(100) NOT NULL,
    child_ref   VARCHAR(100),
    details     TEXT
);

-- Indexes
CREATE INDEX idx_parent_accounts_email ON parent_accounts(email);
CREATE INDEX idx_verification_tokens_parent ON verification_tokens(parent_id, purpose)
    WHERE consumed_at IS NULL;
CREATE INDEX idx_audit_log_parent_ref ON audit_log_entries(parent_ref);
