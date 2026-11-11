from datetime import datetime
from uuid import UUID

from pydantic import BaseModel, ConfigDict, Field


class WorkflowCreate(BaseModel):
    name: str = "Untitled test"
    description: str = ""
    graph: dict | None = None


class WorkflowUpdate(BaseModel):
    name: str | None = None
    description: str | None = None
    graph: dict | None = None


class WorkflowOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: UUID
    name: str
    description: str
    graph: dict
    webhook_token: str
    created_at: datetime
    updated_at: datetime


class ScheduleCreate(BaseModel):
    name: str = "Schedule"
    schedule_type: str
    cron_expression: str | None = None
    interval_seconds: int | None = None
    enabled: bool = True


class ScheduleUpdate(BaseModel):
    name: str | None = None
    schedule_type: str | None = None
    cron_expression: str | None = None
    interval_seconds: int | None = None
    enabled: bool | None = None


class ScheduleOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: UUID
    workflow_id: UUID
    name: str
    schedule_type: str
    cron_expression: str | None
    interval_seconds: int | None
    enabled: bool
    last_run_at: datetime | None
    created_at: datetime


class RunOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: UUID
    workflow_id: UUID
    status: str
    trigger: str
    error_message: str | None = None
    started_at: datetime | None = None
    finished_at: datetime | None = None
    created_at: datetime


class NodeResultOut(BaseModel):
    id: UUID
    run_id: UUID
    node_id: str
    node_type: str
    status: str
    duration_ms: int
    message: str
    error_message: str
    output_data: dict
    has_screenshot: bool
    sequence: int
    created_at: datetime


class PickIn(BaseModel):
    url: str
    browser: str = "chrome"
    headless: bool = True


class ProbeIn(BaseModel):
    url: str
    strategy: str = "XPATH"
    query: str
    timeout_seconds: int = Field(default=10, ge=1, le=60)
    browser: str = "chrome"
    headless: bool = True
