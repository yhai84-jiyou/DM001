from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.config import get_settings
from app.api import auth, users, organizations, issues, dictionary, reports, changelog, attachments, ai_assistant
from app.core.changelog_hook import register_hooks

register_hooks()

settings = get_settings()


@asynccontextmanager
async def lifespan(app: FastAPI):
    from app.database import engine, Base
    from app.models import *  # noqa: F401,F403
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)

    from app.seed import seed_all
    try:
        await seed_all()
    except Exception as e:
        print(f"种子数据: {e}")

    yield


app = FastAPI(
    title=settings.APP_NAME,
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

app.include_router(auth.router, prefix="/api/v1/auth", tags=["认证"])
app.include_router(users.router, prefix="/api/v1/users", tags=["用户管理"])
app.include_router(organizations.router, prefix="/api/v1/organizations", tags=["组织管理"])
app.include_router(dictionary.router, prefix="/api/v1/dictionary", tags=["数据字典"])
app.include_router(issues.router, prefix="/api/v1/issues", tags=["问题管理"])
app.include_router(reports.router, prefix="/api/v1/reports", tags=["统计报表"])
app.include_router(changelog.router, prefix="/api/v1/changelog", tags=["变更日志"])
app.include_router(attachments.router, prefix="/api/v1/attachments", tags=["附件"])
app.include_router(ai_assistant.router, prefix="/api/v1/ai", tags=["AI助手"])


@app.get("/api/v1/health")
async def health_check():
    return {"status": "ok", "app": settings.APP_NAME}
