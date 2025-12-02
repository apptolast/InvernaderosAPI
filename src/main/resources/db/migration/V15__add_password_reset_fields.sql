-- =====================================================
-- V15: Add password reset fields to users table
-- =====================================================
ALTER TABLE metadata.users
ADD COLUMN IF NOT EXISTS reset_password_token VARCHAR(255),
    ADD COLUMN IF NOT EXISTS reset_password_token_expiry TIMESTAMPTZ;
COMMENT ON COLUMN metadata.users.reset_password_token IS 'Token for password reset flow';
COMMENT ON COLUMN metadata.users.reset_password_token_expiry IS 'Expiration time for the password reset token';