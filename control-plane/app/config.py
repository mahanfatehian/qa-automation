from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

    database_url: str = "postgresql+asyncpg://blueprint:blueprint@postgres:5432/blueprintqa"
    redis_url: str = "redis://redis:6379/0"
    runner_url: str = "http://runner-engine:8081"
    selenium_grid_url: str = "http://selenium-hub:4444"
    cors_origins: str = "*"
    max_steps: int = 500
    queue_name: str = "queue:workflow_jobs"
    telemetry_channel: str = "telemetry:workflow_events"


settings = Settings()
