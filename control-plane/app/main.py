import asyncio
import contextlib
import logging
import secrets
from contextlib import asynccontextmanager
from uuid import UUID

from pathlib import Path

from fastapi import FastAPI, WebSocket, WebSocketDisconnect
from fastapi.staticfiles import StaticFiles
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from sqlalchemy import func, select, text
import httpx

from app.config import settings
from app.database import SessionLocal, dispose_db, init_db
from app.demo import demo_graph
from app.models import NodeResult, TestRun, Workflow
from app.redis_bus import close as close_redis
from app.redis_bus import connect as connect_redis
from app.redis_bus import get_redis, listen, public_event
from app.routers import runs, schedules, tools, workflows
from app.scheduler import shutdown as shutdown_scheduler
from app.scheduler import start as start_scheduler
from app.validation import GraphError
from app.ws import manager

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s %(message)s")
logger = logging.getLogger("blueprintqa")


async def seed_demo() -> None:
    async with SessionLocal() as session:
        count = await session.scalar(select(func.count()).select_from(Workflow))
        if count:
            return
        session.add(
            Workflow(
                name="Quotes smoke test",
                description="Opens the local fixture, reads the intro, and checks the text.",
                graph=demo_graph(),
                webhook_token=secrets.token_urlsafe(24),
            )
        )
        await session.commit()


@asynccontextmanager
async def lifespan(_: FastAPI):
    await init_db()
    await seed_demo()
    await connect_redis()
    listener = asyncio.create_task(listen())
    await start_scheduler()
    yield
    shutdown_scheduler()
    listener.cancel()
    with contextlib.suppress(asyncio.CancelledError):
        await listener
    await close_redis()
    await dispose_db()


app = FastAPI(title="BlueprintQA", version="0.1.0", lifespan=lifespan)
origins = ["*"] if settings.cors_origins.strip() == "*" else [item.strip() for item in settings.cors_origins.split(",") if item.strip()]
app.add_middleware(
    CORSMiddleware,
    allow_origins=origins,
    allow_credentials=False,
    allow_methods=["*"],
    allow_headers=["*"],
)
app.include_router(workflows.router)
app.include_router(schedules.router)
app.include_router(runs.router)
app.include_router(tools.router)
fixtures = Path(__file__).resolve().parents[1] / "fixtures"
if fixtures.is_dir():
    app.mount("/fixtures", StaticFiles(directory=fixtures), name="fixtures")


@app.exception_handler(GraphError)
async def graph_error_handler(_, exc: GraphError) -> JSONResponse:
    return JSONResponse(status_code=400, content={"detail": str(exc), "errors": exc.errors})


@app.get("/health")
async def health() -> dict:
    return {"status": "ok", "service": "control-plane"}


@app.get("/api/v1/system/status")
async def system_status() -> dict:
    database = False
    redis_ok = False
    grid = {"ready": False, "node_count": 0, "message": "not checked"}
    try:
        async with SessionLocal() as session:
            await session.execute(text("SELECT 1"))
        database = True
    except Exception as exc:
        database = False
        logger.warning("database status failed: %s", exc)
    try:
        redis_ok = bool(await get_redis().ping())
    except Exception as exc:
        redis_ok = False
        logger.warning("redis status failed: %s", exc)
    try:
        async with httpx.AsyncClient(timeout=4) as client:
            response = await client.get(settings.selenium_grid_url.rstrip("/") + "/status")
            body = response.json()
            value = body.get("value", body)
            nodes = value.get("nodes") or []
            grid = {
                "ready": bool(value.get("ready")),
                "node_count": len(nodes),
                "message": value.get("message") or "hub reachable",
            }
    except Exception as exc:
        grid = {"ready": False, "node_count": 0, "message": str(exc)}
    return {"database": database, "redis": redis_ok, "grid": grid}


def result_to_event(row: NodeResult) -> dict:
    shot = row.screenshot_base64 or ""
    omitted = len(shot) > 900_000
    return public_event(
        {
            "run_id": str(row.run_id),
            "node_id": row.node_id,
            "node_type": row.node_type,
            "event_type": f"NODE_{row.status}",
            "duration_ms": row.duration_ms,
            "message": row.message or "",
            "error_stack": row.error_message or "",
            "output_data": row.output_data or {},
            "screenshot_base64": "" if omitted else shot,
            "sequence": row.sequence,
        }
    )


@app.websocket("/ws/runs/{run_id}")
async def run_socket(run_id: str, websocket: WebSocket) -> None:
    await manager.connect(run_id, websocket)
    try:
        try:
            rid = UUID(run_id)
        except ValueError:
            await websocket.close(code=4400)
            return
        async with SessionLocal() as session:
            run = await session.get(TestRun, rid)
            if run is not None:
                await websocket.send_json(
                    {
                        "event_type": "RUN_STATUS",
                        "run_id": run_id,
                        "status": run.status,
                        "message": run.error_message or "",
                    }
                )
            rows = (
                await session.execute(select(NodeResult).where(NodeResult.run_id == rid).order_by(NodeResult.sequence))
            ).scalars().all()
            for row in rows:
                await websocket.send_json(result_to_event(row))
        while True:
            await websocket.receive_text()
    except WebSocketDisconnect:
        pass
    finally:
        manager.disconnect(run_id, websocket)
