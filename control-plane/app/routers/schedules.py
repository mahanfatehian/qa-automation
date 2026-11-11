import asyncio
import secrets
from uuid import UUID

from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import SessionLocal, get_db
from app.models import TestRun, Workflow, WorkflowSchedule
from app.redis_bus import enqueue_run
from app.scheduler import add_job, remove_job, validate_schedule
from app.schemas import RunOut, ScheduleCreate, ScheduleOut, ScheduleUpdate
from app.validation import GraphError, validate_graph

router = APIRouter(tags=["schedules"])


@router.get("/api/v1/workflows/{workflow_id}/schedules", response_model=list[ScheduleOut])
async def list_schedules(workflow_id: UUID, db: AsyncSession = Depends(get_db)) -> list[WorkflowSchedule]:
    rows = (
        await db.execute(
            select(WorkflowSchedule)
            .where(WorkflowSchedule.workflow_id == workflow_id)
            .order_by(WorkflowSchedule.created_at.desc())
        )
    ).scalars().all()
    return list(rows)


@router.post("/api/v1/workflows/{workflow_id}/schedules", response_model=ScheduleOut, status_code=201)
async def create_schedule(workflow_id: UUID, body: ScheduleCreate, db: AsyncSession = Depends(get_db)) -> WorkflowSchedule:
    workflow = await db.get(Workflow, workflow_id)
    if workflow is None:
        raise HTTPException(status_code=404, detail="Blueprint not found")
    try:
        validate_schedule(body.schedule_type, body.cron_expression, body.interval_seconds)
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc
    row = WorkflowSchedule(
        workflow_id=workflow_id,
        name=body.name.strip() or "Schedule",
        schedule_type=body.schedule_type,
        cron_expression=body.cron_expression,
        interval_seconds=body.interval_seconds,
        enabled=body.enabled,
    )
    db.add(row)
    await db.commit()
    await db.refresh(row)
    add_job(row)
    return row


@router.patch("/api/v1/schedules/{schedule_id}", response_model=ScheduleOut)
async def update_schedule(schedule_id: UUID, body: ScheduleUpdate, db: AsyncSession = Depends(get_db)) -> WorkflowSchedule:
    row = await db.get(WorkflowSchedule, schedule_id)
    if row is None:
        raise HTTPException(status_code=404, detail="Schedule not found")
    schedule_type = body.schedule_type or row.schedule_type
    cron_expression = body.cron_expression if body.cron_expression is not None else row.cron_expression
    interval_seconds = body.interval_seconds if body.interval_seconds is not None else row.interval_seconds
    try:
        validate_schedule(schedule_type, cron_expression, interval_seconds)
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc
    if body.name is not None:
        row.name = body.name.strip() or row.name
    row.schedule_type = schedule_type
    row.cron_expression = cron_expression
    row.interval_seconds = interval_seconds
    if body.enabled is not None:
        row.enabled = body.enabled
    await db.commit()
    await db.refresh(row)
    add_job(row)
    return row


@router.delete("/api/v1/schedules/{schedule_id}", status_code=204)
async def delete_schedule(schedule_id: UUID, db: AsyncSession = Depends(get_db)) -> None:
    row = await db.get(WorkflowSchedule, schedule_id)
    if row is None:
        raise HTTPException(status_code=404, detail="Schedule not found")
    remove_job(str(row.id))
    await db.delete(row)
    await db.commit()


@router.post("/api/v1/webhooks/trigger/{workflow_id}", response_model=RunOut)
async def trigger_webhook(
    workflow_id: UUID,
    token: str = Query(...),
    wait: bool = Query(False),
    timeout: int = Query(180, ge=5, le=900),
    db: AsyncSession = Depends(get_db),
) -> TestRun:
    workflow = await db.get(Workflow, workflow_id)
    if workflow is None or not secrets.compare_digest(workflow.webhook_token, token):
        raise HTTPException(status_code=401, detail="Unknown blueprint or token")
    try:
        validate_graph(workflow.graph or {}, require_start=True)
    except GraphError as exc:
        raise HTTPException(status_code=400, detail={"message": str(exc), "errors": exc.errors}) from exc
    run = await enqueue_run(db, workflow, "WEBHOOK")
    if not wait:
        return run
    deadline = asyncio.get_running_loop().time() + timeout
    while asyncio.get_running_loop().time() < deadline:
        await asyncio.sleep(1)
        async with SessionLocal() as session:
            current = await session.get(TestRun, run.id)
            if current and current.status in {"PASSED", "FAILED", "ERROR"}:
                if current.status != "PASSED":
                    raise HTTPException(status_code=422, detail={"run_id": str(current.id), "status": current.status, "error": current.error_message})
                return current
    raise HTTPException(status_code=504, detail={"run_id": str(run.id), "status": "TIMEOUT"})
