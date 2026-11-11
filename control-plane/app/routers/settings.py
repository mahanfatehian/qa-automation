import json

import httpx
from fastapi import APIRouter
from pydantic import BaseModel, Field

from app.config import settings
from app.grid_scale import scale
from app.redis_bus import get_redis

router = APIRouter(prefix="/api/v1/settings", tags=["settings"])
KEY = "studio:settings"
DEFAULTS = {"browser_slots": 0, "ready_ms": 800, "window_width": 1280, "window_height": 800}


class StudioSettings(BaseModel):
    browser_slots: int = Field(default=0, ge=0, le=16)
    ready_ms: int = Field(default=800, ge=0, le=5000)
    window_width: int = Field(default=1280, ge=320, le=1920)
    window_height: int = Field(default=800, ge=240, le=1200)


@router.get("")
async def read_settings() -> dict:
    stored = await _stored()
    return {**stored, "grid": await _grid()}


@router.put("")
async def write_settings(body: StudioSettings) -> dict:
    payload = body.model_dump()
    await get_redis().set(KEY, json.dumps(payload))
    note = scale(body.browser_slots)
    return {**payload, "grid": await _grid(), "scale_note": note}


async def _stored() -> dict:
    raw = await get_redis().get(KEY)
    if not raw:
        return dict(DEFAULTS)
    try:
        data = json.loads(raw)
    except json.JSONDecodeError:
        return dict(DEFAULTS)
    return {**DEFAULTS, **data}


async def _grid() -> dict:
    try:
        async with httpx.AsyncClient(timeout=4) as client:
            response = await client.get(settings.selenium_grid_url.rstrip("/") + "/status")
            value = response.json().get("value", {})
            nodes = value.get("nodes") or []
            slots = 0
            for node in nodes:
                slots += int(node.get("maxSessions") or len(node.get("slots") or []) or 0)
            return {"ready": bool(value.get("ready")), "nodes": len(nodes), "slots": slots}
    except httpx.HTTPError:
        return {"ready": False, "nodes": 0, "slots": 0}
