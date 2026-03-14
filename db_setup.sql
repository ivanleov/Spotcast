-- расширение
CREATE EXTENSION IF NOT EXISTS postgis;

-- юзеры
CREATE TABLE IF NOT EXISTS users (
    id              SERIAL PRIMARY KEY,
    username        VARCHAR(100) UNIQUE NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    created_at      TIMESTAMPTZ DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);

-- друзья
CREATE TABLE IF NOT EXISTS friendships (
    id          SERIAL PRIMARY KEY,
    user_id     INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    friend_id   INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status      VARCHAR(20) DEFAULT 'pending',
    created_at  TIMESTAMPTZ DEFAULT now(),
    CONSTRAINT uq_friendship UNIQUE (user_id, friend_id)
);
CREATE INDEX IF NOT EXISTS idx_friendships_user ON friendships(user_id);
CREATE INDEX IF NOT EXISTS idx_friendships_friend ON friendships(friend_id);

-- капсулы
CREATE TABLE IF NOT EXISTS capsules (
    id              SERIAL PRIMARY KEY,
    user_id         INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    recipient_id    INTEGER REFERENCES users(id) ON DELETE SET NULL,
    location        geometry(Point, 4326) NOT NULL,
    radius          DOUBLE PRECISION DEFAULT 50.0,
    capsule_type    VARCHAR(10) NOT NULL CHECK (capsule_type IN ('TEXT', 'AUDIO')),
    text_content    TEXT,
    audio_path      VARCHAR(500),
    layer           VARCHAR(50) DEFAULT 'personal',
    ttl             TIMESTAMPTZ,
    is_active       BOOLEAN DEFAULT true,
    is_completed    BOOLEAN DEFAULT false,
    created_at      TIMESTAMPTZ DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_capsules_user     ON capsules(user_id);
CREATE INDEX IF NOT EXISTS idx_capsules_recipient ON capsules(recipient_id);
CREATE INDEX IF NOT EXISTS idx_capsules_layer    ON capsules(layer);
CREATE INDEX IF NOT EXISTS idx_capsules_location ON capsules USING GIST(location);
CREATE INDEX IF NOT EXISTS idx_capsules_active   ON capsules(is_active) WHERE is_active = true;

-- готово
SELECT 'таблицы созданы' AS result;
