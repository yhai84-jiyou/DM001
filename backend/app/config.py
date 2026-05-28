from pydantic_settings import BaseSettings
from functools import lru_cache


class Settings(BaseSettings):
    APP_NAME: str = "项目问题管理系统"
    DEBUG: bool = False

    DATABASE_URL: str = "postgresql+asyncpg://pm_admin:changeme@localhost:5432/pm_system"
    REDIS_URL: str = "redis://localhost:6379/0"

    JWT_SECRET: str = "change-this-to-a-random-secret-key"
    JWT_ALGORITHM: str = "HS256"
    ACCESS_TOKEN_EXPIRE_MINUTES: int = 120
    REFRESH_TOKEN_EXPIRE_DAYS: int = 7

    AI_DEFAULT_PROVIDER: str = "deepseek"
    AI_DEEPSEEK_API_KEY: str = ""
    AI_DEEPSEEK_BASE_URL: str = "https://api.deepseek.com"
    AI_DEEPSEEK_MODEL: str = "deepseek-chat"
    AI_QWEN_API_KEY: str = ""
    AI_QWEN_BASE_URL: str = "https://dashscope.aliyuncs.com/compatible-mode/v1"
    AI_QWEN_MODEL: str = "qwen-max"

    UPLOAD_DIR: str = "/data/uploads"
    MAX_UPLOAD_SIZE: int = 20 * 1024 * 1024

    model_config = {"env_file": ".env", "env_file_encoding": "utf-8"}


@lru_cache
def get_settings() -> Settings:
    return Settings()
