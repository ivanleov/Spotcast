import asyncio
import sys
import asyncpg
import os

async def test_connection(password=None, **kwargs):
    try:
        defaults = {
            'host': 'localhost',
            'port': 5432,
            'user': 'postgres',
            'database': 'postgres'
        }
        defaults.update(kwargs)
        if password is not None:
            defaults['password'] = password

        print(f"коннект {defaults.get('user')}@{defaults.get('host')}:{defaults.get('port')}")
        conn = await asyncio.wait_for(asyncpg.connect(**defaults), timeout=5.0)
        await conn.close()
        print("ок")
        return True
    except Exception as e:
        print(f"ошибка: {e}")
        return False

async def setup_database():
    print("настройка бд")

    attempts = [{"password": ""}, {"password": "postgres"}, {}]
    conn = None
    for kwargs in attempts:
        pwd = kwargs.get('password', None)
        if await test_connection(password=pwd):
            conn = await asyncpg.connect(
                host='localhost', port=5432, user='postgres',
                database='postgres', **({"password": pwd} if pwd is not None else {})
            )
            break

    if not conn:
        print("нет коннекта")
        return False

    try:
        result = await conn.fetchval("SELECT 1 FROM pg_database WHERE datname = 'spotcast'")
        if not result:
            await conn.execute("DROP DATABASE IF EXISTS spotcast;")
            await conn.execute("CREATE DATABASE spotcast;")
        await conn.close()

        pwd = kwargs.get('password')
        conn = await asyncpg.connect(
            host='localhost', port=5432, user='postgres',
            database='spotcast', **({"password": pwd} if pwd is not None else {})
        )

        await conn.execute("CREATE EXTENSION IF NOT EXISTS postgis;")

        await conn.execute("""
            CREATE TABLE IF NOT EXISTS users (
                id            SERIAL       PRIMARY KEY,
                username      VARCHAR(100) UNIQUE NOT NULL,
                password_hash VARCHAR(255) NOT NULL,
                created_at    TIMESTAMPTZ  DEFAULT NOW()
            );
        """)

        await conn.execute("""
            CREATE TABLE IF NOT EXISTS capsules (
                id            SERIAL       PRIMARY KEY,
                user_id       INTEGER      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                location      GEOMETRY(Point, 4326) NOT NULL,
                radius        DOUBLE PRECISION DEFAULT 50.0,
                capsule_type  VARCHAR(10)  NOT NULL CHECK (capsule_type IN ('TEXT', 'AUDIO')),
                text_content  TEXT,
                audio_path    VARCHAR(500),
                layer         VARCHAR(50)  DEFAULT 'personal',
                ttl           TIMESTAMPTZ,
                is_active     BOOLEAN      DEFAULT TRUE,
                is_completed  BOOLEAN      DEFAULT FALSE,
                created_at    TIMESTAMPTZ  DEFAULT NOW()
            );
        """)

        await conn.execute("CREATE INDEX IF NOT EXISTS idx_capsules_location ON capsules USING GIST (location);")
        await conn.execute("CREATE INDEX IF NOT EXISTS idx_capsules_user ON capsules (user_id);")
        await conn.execute("CREATE INDEX IF NOT EXISTS idx_capsules_layer ON capsules (layer);")
        await conn.execute("CREATE INDEX IF NOT EXISTS idx_capsules_active ON capsules (is_active) WHERE is_active = true;")

        print("готово")
        return True

    except Exception as e:
        print(f"ошибка: {e}")
        return False
    finally:
        if conn and not conn.is_closed():
            await conn.close()

if __name__ == "__main__":
    success = asyncio.run(setup_database())
    sys.exit(0 if success else 1)
