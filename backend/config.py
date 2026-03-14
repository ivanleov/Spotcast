import os
from pathlib import Path

_env_path = Path(__file__).resolve().parent.parent / ".env"
if _env_path.exists():
    with open(_env_path) as _f:
        for _line in _f:
            _line = _line.strip()
            if _line and not _line.startswith("#") and "=" in _line:
                _k, _v = _line.split("=", 1)
                os.environ.setdefault(_k.strip(), _v.strip())

DATABASE_URL: str = os.getenv(
    "DATABASE_URL",
    "postgresql+asyncpg://<username>:<password>@<db-host>:<db-port>/<db-name>",
)

JWT_SECRET: str = os.getenv("JWT_SECRET", "change-me-in-production-please")
JWT_ALGORITHM: str = "HS256"
JWT_EXPIRATION_MINUTES: int = 60 * 24

SERVER_HOST: str = "0.0.0.0"
SERVER_PORT: int = 8080

AUDIO_UPLOAD_DIR: str = os.getenv("AUDIO_UPLOAD_DIR", "./uploads/audio")
os.makedirs(AUDIO_UPLOAD_DIR, exist_ok=True)

DEFAULT_RADIUS_METERS: float = 50.0
DEFAULT_NEARBY_RADIUS_METERS: float = 500.0
