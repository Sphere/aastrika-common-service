
CREATE TABLE IF NOT EXISTS user_fcm_tokens (
    device_id  TEXT PRIMARY KEY,
    fcm_token  TEXT NOT NULL UNIQUE,
    user_id    TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_user_fcm_tokens_user_id ON user_fcm_tokens (user_id);
