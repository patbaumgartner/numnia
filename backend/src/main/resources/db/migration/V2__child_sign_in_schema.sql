-- UC-002: child sign-in PIN, lockout and server-side session schema.
-- Complements the UC-001 IAM baseline without changing already-applied V1.

ALTER TABLE child_profiles
    ADD COLUMN pin_hash VARCHAR(255),
    ADD COLUMN failed_sign_in_count INT NOT NULL DEFAULT 0,
    ADD COLUMN locked_at TIMESTAMPTZ,
    ADD COLUMN locked_reason VARCHAR(100);

CREATE TABLE child_sessions (
    id         UUID        NOT NULL PRIMARY KEY,
    child_id   UUID        NOT NULL REFERENCES child_profiles(id),
    parent_id  UUID        NOT NULL REFERENCES parent_accounts(id),
    role       VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ
);

CREATE INDEX idx_child_sessions_child_id ON child_sessions(child_id);
CREATE INDEX idx_child_sessions_parent_id ON child_sessions(parent_id);
CREATE INDEX idx_child_sessions_active
    ON child_sessions(child_id, expires_at)
    WHERE revoked_at IS NULL;
