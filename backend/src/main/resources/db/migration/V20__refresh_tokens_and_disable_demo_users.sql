-- Refresh token revocation store (jti)
CREATE TABLE refresh_tokens (
    jti            UUID PRIMARY KEY,
    user_id        UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    expires_at     TIMESTAMPTZ  NOT NULL,
    revoked_at     TIMESTAMPTZ,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens (expires_at);

-- Demo accounts from V4 must not remain usable on prod-like DBs.
-- Profile "dev" re-activates them via DevDataInitializer.
UPDATE users
SET actif = FALSE,
    updated_at = NOW()
WHERE email IN (
    'admin@ocp.ma',
    'responsable@ocp.ma',
    'technicien@ocp.ma'
);
