-- =============================================================================
-- V39 — Refresh tokens with rotation + reuse-detection family tracking
-- =============================================================================
-- Stores opaque refresh tokens issued by /api/v1/auth/login, /register and
-- rotated by /api/v1/auth/refresh. Implements the OAuth-style rotation +
-- reuse-detection pattern:
--
--   * token_hash       = SHA-256 hex (lowercase, 64 chars) of the opaque token.
--                        The plaintext token is NEVER persisted; only the hash.
--   * family_id        = UUID grouping the rotation chain of a single login.
--                        On rotation the new row inherits the parent's family_id.
--                        If a row whose revoked_at is already set is presented
--                        again, the whole family is revoked (reuse → robbery
--                        signal) and a SECURITY WARN log is emitted.
--   * rotated_from_id  = self-reference to the parent token in the rotation
--                        chain. NULL for the first token in a family (issued
--                        by login/register).
--   * expires_at       = absolute expiration timestamp (default refresh TTL
--                        is 30d, configurable via JWT_REFRESH_EXPIRATION env).
--   * revoked_at       = NULL while the token is active. Set by /refresh
--                        rotation, /logout, or family-wide reuse detection.
--
-- The unique index on token_hash enforces single-use semantics. Hot lookup
-- path is "find by hash WHERE revoked_at IS NULL"; the partial index
-- idx_refresh_tokens_user_active accelerates per-user revocation on logout.
--
-- Idempotent (uses IF NOT EXISTS) so repeated runs against dev/prod databases
-- are safe. Foreign key on user_id cascades on user deletion (matches the
-- existing pattern of metadata.push_tokens, V38).
-- =============================================================================

CREATE TABLE IF NOT EXISTS metadata.refresh_tokens (
    id              BIGSERIAL    PRIMARY KEY,
    user_id         BIGINT       NOT NULL REFERENCES metadata.users(id)          ON DELETE CASCADE,
    token_hash      VARCHAR(64)  NOT NULL UNIQUE,
    family_id       UUID         NOT NULL,
    rotated_from_id BIGINT       NULL     REFERENCES metadata.refresh_tokens(id) ON DELETE SET NULL,
    expires_at      TIMESTAMPTZ  NOT NULL,
    revoked_at      TIMESTAMPTZ  NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user        ON metadata.refresh_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_family      ON metadata.refresh_tokens(family_id);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_expires     ON metadata.refresh_tokens(expires_at);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_active
    ON metadata.refresh_tokens(user_id) WHERE revoked_at IS NULL;

COMMENT ON TABLE metadata.refresh_tokens IS
    'Opaque refresh tokens with rotation. Stores SHA-256 hash only. family_id groups the rotation chain; reuse of a revoked token triggers full-family revocation.';
COMMENT ON COLUMN metadata.refresh_tokens.token_hash      IS 'SHA-256 hex (lowercase) of the plaintext opaque token. Plaintext is never stored.';
COMMENT ON COLUMN metadata.refresh_tokens.family_id       IS 'Groups all rotations that descend from the same login.';
COMMENT ON COLUMN metadata.refresh_tokens.rotated_from_id IS 'Parent token in the rotation chain. NULL for the first token of a family.';
COMMENT ON COLUMN metadata.refresh_tokens.revoked_at      IS 'NULL while active. Set on rotation, logout, or family-wide reuse detection.';
