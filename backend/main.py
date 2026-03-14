from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import RedirectResponse
from fastapi.staticfiles import StaticFiles
from sqlalchemy import text

from .config import AUDIO_UPLOAD_DIR, SERVER_HOST, SERVER_PORT
from .database import Base, engine
from .routers.auth_router import router as auth_router
from .routers.capsules_router import router as capsules_router
from .routers.friends_router import router as friends_router
from .routers.layers_router import router as layers_router

@asynccontextmanager
async def lifespan(app: FastAPI):
    try:
        async with engine.begin() as conn:
            await conn.execute(text("CREATE EXTENSION IF NOT EXISTS postgis"))
            await conn.run_sync(Base.metadata.create_all)
    except Exception as e:
        import logging
        logging.warning(f"бд ошибка: {e}")
    yield
    await engine.dispose()

app = FastAPI(
    title="api",
    version="1.0.0",
    lifespan=lifespan,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.mount("/audio", StaticFiles(directory=AUDIO_UPLOAD_DIR), name="audio")

app.include_router(auth_router)
app.include_router(capsules_router)
app.include_router(friends_router)
app.include_router(layers_router)

@app.get("/")
async def root():
    return RedirectResponse(url="/docs")

@app.get("/health")
async def health():
    return {"status": "ok"}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host=SERVER_HOST, port=SERVER_PORT, reload=True)
