import json
import logging
from uuid import UUID

import redis.asyncio as redis
from sqlalchemy import func, select
from sqlalchemy.ext.asyncio import AsyncSession

from app.config import settings
from app.database import SessionLocal
from app.models import NodeResult, ScheduleLog, TestRun, Workflow, utcnow
from app.ws import manager

logger = logging.getLogger(__name__)

_redis: redis.Redis | None = None


class BusyError(RuntimeError):
    pass


def get_redis() -> redis.Redis:
    if _redis is None:
        raise RuntimeError("Redis is not connected")
    return _redis


async def connect() -> None:
    global _redis
    _redis = redis.from_url(settings.redis_url, decode_responses=True)
    await _redis.ping()


async def close() -> None:
    global _redis
    if _redis is not None:
        await _redis.aclose()
        _redis = None


def clean_node(node: dict) -> dict:
    data = dict(node.get("data") or {})
    data.pop("runStatus", None)
    kind = data.get("nodeType") or node.get("type")
    data["nodeType"] = kind
    return {
        "id": node.get("id"),
        "type": kind,
        "data": data,
        "position": node.get("position") or {"x": 0, "y": 0},
    }


def clean_edge(edge: dict) -> dict:
    return {
        "id": edge.get("id"),
        "source": edge.get("source"),
        "target": edge.get("target"),
        "sourceHandle": edge.get("sourceHandle") or "exec_out",
        "targetHandle": edge.get("targetHandle") or "exec_in",
    }


def build_payload(run: TestRun, workflow: Workflow, row: dict | None = None, row_index: int | None = None) -> dict:
    graph = workflow.graph or {}
    try:
        max_steps = int(graph.get("maxSteps") or settings.max_steps)
    except (TypeError, ValueError):
        max_steps = settings.max_steps
    variables = dict(graph.get("variables") or {})
    if row:
        variables.update({str(key): value for key, value in row.items()})
    name = workflow.name
    if row_index is not None:
        name = f"{name} [example {row_index + 1}]"
    return {
        "runId": str(run.id),
        "workflowId": str(workflow.id),
        "workflowName": name,
        "maxSteps": max(1, min(max_steps, 5000)),
        "nodes": [clean_node(node) for node in graph.get("nodes") or []],
        "edges": [clean_edge(edge) for edge in graph.get("edges") or []],
        "variables": variables,
    }


def example_rows(graph: dict) -> list[dict]:
    raw = graph.get("examples") or []
    if not isinstance(raw, list):
        return []
    rows = [row for row in raw if isinstance(row, dict) and row]
    return rows[:25]


def planned_rows(graph: dict) -> list[dict]:
    import random
    import re

    uses: dict[str, dict] = {}
    for node in graph.get("nodes") or []:
        data = (node or {}).get("data") or {}
        specs = data.get("inputUses") if isinstance(data.get("inputUses"), dict) else {}
        blob = " ".join(str(value) for value in data.values() if isinstance(value, str))
        for name in re.findall(r"\{\{\s*([\w.-]+)\s*\}\}", blob):
            spec = specs.get(name)
            uses[name] = spec if isinstance(spec, dict) else {"mode": "one"}
    if not uses:
        return []
    expanding: list[tuple[str, list[str]]] = []
    fixed: dict[str, str] = {}
    for name, spec in uses.items():
        pool = _pool(graph, name)
        mode = str(spec.get("mode") or "one")
        chosen = [str(value) for value in (spec.get("values") or []) if str(value)]
        if mode == "all":
            expanding.append((name, pool or [""]))
        elif mode == "selected":
            expanding.append((name, chosen or [""]))
        elif mode == "random-selected":
            pick_from = chosen or pool
            fixed[name] = random.choice(pick_from) if pick_from else ""
        elif mode == "random":
            fixed[name] = random.choice(pool) if pool else ""
        else:
            fixed[name] = chosen[0] if chosen else (pool[0] if pool else "")
    if not expanding:
        return [fixed]
    rows = [dict(fixed)]
    for name, values in expanding:
        rows = [{**row, name: value} for row in rows for value in values]
    return rows[:25]


def _pool(graph: dict, name: str) -> list[str]:
    found: list[str] = []
    for row in example_rows(graph):
        value = row.get(name)
        if value not in (None, "") and str(value) not in found:
            found.append(str(value))
    for column in graph.get("dataColumns") or []:
        if isinstance(column, dict) and column.get("name") == name:
            for option in column.get("options") or []:
                if option and str(option) not in found:
                    found.append(str(option))
    return found


async def enqueue_run(session: AsyncSession, workflow: Workflow, trigger: str) -> TestRun:
    if trigger in {"CRON", "INTERVAL"}:
        active = await session.scalar(
            select(func.count())
            .select_from(TestRun)
            .where(TestRun.workflow_id == workflow.id, TestRun.status.in_(["QUEUED", "RUNNING"]))
        )
        if active:
            raise BusyError("A run for this blueprint is already queued or running")
    rows = planned_rows(workflow.graph or {})
    queued: list[tuple[TestRun, dict | None, int | None]] = []
    if not rows:
        run = TestRun(workflow_id=workflow.id, status="QUEUED", trigger=trigger)
        session.add(run)
        queued.append((run, None, None))
    else:
        total = len(rows)
        for index, row in enumerate(rows):
            bits = ", ".join(f"{key}={value}" for key, value in row.items())
            label = f"{index + 1} of {total}" + (f" · {bits}" if bits else "")
            run = TestRun(workflow_id=workflow.id, status="QUEUED", trigger=trigger, variation_label=label)
            session.add(run)
            queued.append((run, row, index))
    await session.commit()
    first = queued[0][0]
    await session.refresh(first)
    try:
        for run, row, index in queued:
            await session.refresh(run)
            await get_redis().rpush(settings.queue_name, json.dumps(build_payload(run, workflow, row, index)))
    except Exception as exc:
        first.status = "ERROR"
        first.error_message = f"Could not queue blueprint: {exc}"
        first.finished_at = utcnow()
        await session.commit()
        raise
    return first


def normalize(raw: dict) -> dict:
    output = raw.get("outputData") or raw.get("output_data") or {}
    if not isinstance(output, dict):
        output = {"value": str(output)}
    return {
        "run_id": raw.get("runId") or raw.get("run_id") or "",
        "workflow_id": raw.get("workflowId") or raw.get("workflow_id") or "",
        "node_id": raw.get("nodeId") or raw.get("node_id") or "",
        "node_type": raw.get("nodeType") or raw.get("node_type") or "",
        "event_type": raw.get("eventType") or raw.get("event_type") or "",
        "timestamp": raw.get("timestamp") or "",
        "duration_ms": int(raw.get("durationMs") or raw.get("duration_ms") or 0),
        "message": raw.get("message") or "",
        "error_stack": raw.get("errorStack") or raw.get("error_stack") or "",
        "output_data": output,
        "screenshot_base64": raw.get("screenshotBase64") or raw.get("screenshot_base64") or "",
    }


def public_event(event: dict) -> dict:
    shot = event.get("screenshot_base64") or ""
    omitted = len(shot) > 900_000
    return {
        **event,
        "screenshot_base64": "" if omitted else shot,
        "screenshot_omitted": omitted,
    }


async def handle_event(raw: dict) -> None:
    event = normalize(raw)
    run_id = event["run_id"]
    if not run_id:
        return
    try:
        rid = UUID(run_id)
    except ValueError:
        return
    async with SessionLocal() as session:
        run = await session.get(TestRun, rid)
        if run is None:
            return
        kind = event["event_type"]
        now = utcnow()
        if kind == "RUN_STARTED":
            run.status = "RUNNING"
            run.started_at = run.started_at or now
        elif kind == "RUN_COMPLETED":
            run.status = "PASSED"
            run.finished_at = now
            await note_scheduled(session, run, "info")
        elif kind == "RUN_FAILED":
            run.status = "FAILED"
            run.finished_at = now
            run.error_message = (event.get("message") or "Blueprint failed")[:4000]
            await note_scheduled(session, run, "error")
        elif kind in {"NODE_RUNNING", "NODE_PASSED", "NODE_FAILED"}:
            count = await session.scalar(
                select(func.count()).select_from(NodeResult).where(NodeResult.run_id == rid)
            )
            shot = event.get("screenshot_base64") or None
            session.add(
                NodeResult(
                    run_id=rid,
                    node_id=event.get("node_id") or "",
                    node_type=event.get("node_type") or "",
                    status=kind.removeprefix("NODE_"),
                    duration_ms=int(event.get("duration_ms") or 0),
                    message=(event.get("message") or "")[:2000],
                    error_message=(event.get("error_stack") or "")[:8000],
                    output_data=event.get("output_data") or {},
                    screenshot_base64=shot,
                    sequence=int(count or 0) + 1,
                )
            )
            if kind == "NODE_FAILED" and not run.error_message:
                run.error_message = (event.get("message") or "A step failed")[:4000]
        else:
            return
        await session.commit()
    await manager.broadcast(run_id, public_event(event))


async def note_scheduled(session, run: TestRun, level: str) -> None:
    if run.trigger not in {"CRON", "INTERVAL"}:
        return
    failed = await session.scalar(
        select(NodeResult).where(NodeResult.run_id == run.id, NodeResult.status == "FAILED").order_by(NodeResult.sequence.desc())
    )
    workflow = await session.get(Workflow, run.workflow_id)
    name = workflow.name if workflow else "Blueprint"
    detail = {"status": "PASSED" if level == "info" else "FAILED", "trigger": run.trigger}
    if failed:
        output = failed.output_data or {}
        detail.update({
            "step": failed.node_type,
            "url": output.get("url") or "",
            "selector": output.get("selector") or "",
            "message": failed.message or run.error_message or "",
        })
        label = run.variation_label or "single run"
        detail["variation"] = label
        text = f"{name} · {label} failed at {failed.node_type}: {detail['message']}"
        if detail["url"]:
            text += f" Page: {detail['url']}."
        if detail["selector"]:
            text += f" Selector: {detail['selector']}."
    else:
        label = getattr(run, "variation_label", None) or "single run"
        text = f"{name} · {label} passed." if level == "info" else f"{name} · {label} failed: {run.error_message or 'unknown error'}"
    session.add(ScheduleLog(
        workflow_id=run.workflow_id,
        run_id=run.id,
        level="error" if level != "info" else "info",
        message=text[:2000],
        detail=detail,
    ))


async def listen() -> None:
    client = get_redis()
    pubsub = client.pubsub()
    await pubsub.subscribe(settings.telemetry_channel)
    try:
        async for message in pubsub.listen():
            if message.get("type") != "message":
                continue
            try:
                await handle_event(json.loads(message["data"]))
            except Exception:
                logger.exception("failed to handle telemetry")
    finally:
        await pubsub.unsubscribe(settings.telemetry_channel)
        await pubsub.aclose()
