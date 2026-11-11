import secrets
from uuid import UUID

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import get_db
from app.demo import starter_graph
from app.models import Workflow, utcnow
from app.redis_bus import enqueue_run
from app.schemas import RunOut, WorkflowCreate, WorkflowOut, WorkflowUpdate
from app.validation import GraphError, validate_graph

router = APIRouter(prefix="/api/v1/workflows", tags=["blueprints"])


def _graph_or_default(graph: dict | None) -> dict:
    if graph is None:
        return starter_graph()
    validate_graph(graph, require_start=True)
    return graph


@router.get("", response_model=list[WorkflowOut])
async def list_workflows(db: AsyncSession = Depends(get_db)) -> list[Workflow]:
    rows = (await db.execute(select(Workflow).order_by(Workflow.updated_at.desc()))).scalars().all()
    return list(rows)


@router.post("", response_model=WorkflowOut, status_code=201)
async def create_workflow(body: WorkflowCreate, db: AsyncSession = Depends(get_db)) -> Workflow:
    try:
        graph = _graph_or_default(body.graph)
    except GraphError as exc:
        raise HTTPException(status_code=400, detail={"message": str(exc), "errors": exc.errors}) from exc
    row = Workflow(
        name=body.name.strip() or "Untitled test",
        description=body.description or "",
        graph=graph,
        webhook_token=secrets.token_urlsafe(24),
    )
    db.add(row)
    await db.commit()
    await db.refresh(row)
    return row


@router.get("/{workflow_id}", response_model=WorkflowOut)
async def get_workflow(workflow_id: UUID, db: AsyncSession = Depends(get_db)) -> Workflow:
    row = await db.get(Workflow, workflow_id)
    if row is None:
        raise HTTPException(status_code=404, detail="Blueprint not found")
    return row


@router.put("/{workflow_id}", response_model=WorkflowOut)
async def update_workflow(workflow_id: UUID, body: WorkflowUpdate, db: AsyncSession = Depends(get_db)) -> Workflow:
    row = await db.get(Workflow, workflow_id)
    if row is None:
        raise HTTPException(status_code=404, detail="Blueprint not found")
    if body.name is not None:
        row.name = body.name.strip() or row.name
    if body.description is not None:
        row.description = body.description
    if body.graph is not None:
        try:
            validate_graph(body.graph, require_start=True)
        except GraphError as exc:
            raise HTTPException(status_code=400, detail={"message": str(exc), "errors": exc.errors}) from exc
        row.graph = body.graph
    row.updated_at = utcnow()
    await db.commit()
    await db.refresh(row)
    return row


@router.delete("/{workflow_id}", status_code=204)
async def delete_workflow(workflow_id: UUID, db: AsyncSession = Depends(get_db)) -> None:
    from sqlalchemy import delete

    from app.models import NodeResult, TestRun, WorkflowSchedule

    row = await db.get(Workflow, workflow_id)
    if row is None:
        raise HTTPException(status_code=404, detail="Blueprint not found")
    run_ids = select(TestRun.id).where(TestRun.workflow_id == workflow_id)
    await db.execute(delete(NodeResult).where(NodeResult.run_id.in_(run_ids)))
    await db.execute(delete(TestRun).where(TestRun.workflow_id == workflow_id))
    await db.execute(delete(WorkflowSchedule).where(WorkflowSchedule.workflow_id == workflow_id))
    await db.delete(row)
    await db.commit()


@router.post("/{workflow_id}/run", response_model=RunOut, status_code=202)
async def run_workflow(workflow_id: UUID, db: AsyncSession = Depends(get_db)) -> object:
    row = await db.get(Workflow, workflow_id)
    if row is None:
        raise HTTPException(status_code=404, detail="Blueprint not found")
    try:
        validate_graph(row.graph or {}, require_start=True)
    except GraphError as exc:
        raise HTTPException(status_code=400, detail={"message": str(exc), "errors": exc.errors}) from exc
    try:
        return await enqueue_run(db, row, "MANUAL")
    except Exception as exc:
        raise HTTPException(status_code=503, detail=f"Could not queue blueprint: {exc}") from exc


@router.post("/{workflow_id}/webhook/rotate", response_model=WorkflowOut)
async def rotate_webhook(workflow_id: UUID, db: AsyncSession = Depends(get_db)) -> Workflow:
    row = await db.get(Workflow, workflow_id)
    if row is None:
        raise HTTPException(status_code=404, detail="Blueprint not found")
    row.webhook_token = secrets.token_urlsafe(24)
    row.updated_at = utcnow()
    await db.commit()
    await db.refresh(row)
    return row
