from uuid import UUID
from xml.sax.saxutils import escape

from fastapi import APIRouter, Depends, HTTPException, Query, Response
from sqlalchemy import delete, select
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import get_db
from app.models import NodeResult, TestRun
from app.schemas import NodeResultOut, RunOut

router = APIRouter(prefix="/api/v1/runs", tags=["runs"])


def to_result(row: NodeResult) -> NodeResultOut:
    return NodeResultOut(
        id=row.id,
        run_id=row.run_id,
        node_id=row.node_id,
        node_type=row.node_type,
        status=row.status,
        duration_ms=row.duration_ms,
        message=row.message or "",
        error_message=row.error_message or "",
        output_data=row.output_data or {},
        has_screenshot=bool(row.screenshot_base64),
        sequence=row.sequence,
        created_at=row.created_at,
    )


@router.get("", response_model=list[RunOut])
async def list_runs(
    workflow_id: UUID | None = None,
    limit: int = Query(40, ge=1, le=200),
    db: AsyncSession = Depends(get_db),
) -> list[TestRun]:
    stmt = select(TestRun).order_by(TestRun.created_at.desc()).limit(limit)
    if workflow_id is not None:
        stmt = stmt.where(TestRun.workflow_id == workflow_id)
    return list((await db.execute(stmt)).scalars().all())


@router.get("/{run_id}", response_model=RunOut)
async def get_run(run_id: UUID, db: AsyncSession = Depends(get_db)) -> TestRun:
    row = await db.get(TestRun, run_id)
    if row is None:
        raise HTTPException(status_code=404, detail="Run not found")
    return row


@router.get("/{run_id}/results", response_model=list[NodeResultOut])
async def list_results(run_id: UUID, db: AsyncSession = Depends(get_db)) -> list[NodeResultOut]:
    rows = (
        await db.execute(select(NodeResult).where(NodeResult.run_id == run_id).order_by(NodeResult.sequence))
    ).scalars().all()
    finished = {row.node_id for row in rows if row.status in {"PASSED", "FAILED", "ERROR"} and row.node_id}
    visible = [row for row in rows if not (row.status == "RUNNING" and row.node_id in finished)]
    return [to_result(row) for row in visible]


@router.get("/{run_id}/results/{result_id}/screenshot")
async def screenshot(run_id: UUID, result_id: UUID, db: AsyncSession = Depends(get_db)) -> Response:
    import base64

    row = await db.get(NodeResult, result_id)
    if row is None or row.run_id != run_id or not row.screenshot_base64:
        raise HTTPException(status_code=404, detail="Screenshot not found")
    try:
        raw = base64.b64decode(row.screenshot_base64)
    except Exception as exc:
        raise HTTPException(status_code=422, detail="Screenshot could not be decoded") from exc
    return Response(content=raw, media_type="image/png")


@router.delete("/{run_id}", status_code=204)
async def delete_run(run_id: UUID, db: AsyncSession = Depends(get_db)) -> None:
    row = await db.get(TestRun, run_id)
    if row is None:
        raise HTTPException(status_code=404, detail="Run not found")
    await db.execute(delete(NodeResult).where(NodeResult.run_id == run_id))
    await db.delete(row)
    await db.commit()


@router.get("/{run_id}/junit")
async def junit(run_id: UUID, db: AsyncSession = Depends(get_db)) -> Response:
    run = await db.get(TestRun, run_id)
    if run is None:
        raise HTTPException(status_code=404, detail="Run not found")
    rows = (
        await db.execute(select(NodeResult).where(NodeResult.run_id == run_id).order_by(NodeResult.sequence))
    ).scalars().all()
    seen: dict[str, NodeResult] = {}
    for row in rows:
        if row.node_id:
            seen[row.node_id] = row
    cases = []
    failures = 0
    for row in seen.values():
        seconds = f"{(row.duration_ms or 0) / 1000:.3f}"
        name = escape(f"{row.node_type} {row.node_id}")
        if row.status == "FAILED":
            failures += 1
            message = escape(row.message or "step failed")
            detail = escape(row.error_message or row.message or "")
            cases.append(
                f'<testcase name="{name}" time="{seconds}"><failure message="{message}">{detail}</failure></testcase>'
            )
        else:
            cases.append(f'<testcase name="{name}" time="{seconds}"/>')
    if not cases:
        failed = run.status in {"FAILED", "ERROR"}
        failures = 1 if failed else 0
        body = f'<failure message="{escape(run.error_message or run.status)}"/>' if failed else ""
        cases.append(f'<testcase name="blueprint" time="0">{body}</testcase>')
    xml = (
        '<?xml version="1.0" encoding="UTF-8"?>'
        f'<testsuite name="blueprintqa" tests="{len(cases)}" failures="{failures}">'
        + "".join(cases)
        + "</testsuite>"
    )
    return Response(content=xml, media_type="application/xml")
