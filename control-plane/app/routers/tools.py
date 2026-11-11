import httpx
from fastapi import APIRouter, HTTPException

from app.config import settings
from app.schemas import InspectHoverIn, InspectStartIn, PickIn, ProbeIn

router = APIRouter(prefix="/api/v1/tools", tags=["tools"])


@router.post("/inspect/start")
async def inspect_start(body: InspectStartIn) -> dict:
    payload = {
        "nodes": body.nodes,
        "edges": body.edges,
        "stopBefore": body.stop_before,
        "variables": body.variables,
    }
    try:
        async with httpx.AsyncClient(timeout=60) as client:
            response = await client.post(f"{settings.runner_url.rstrip('/')}/internal/inspect/start", json=payload)
            data = response.json()
    except httpx.HTTPError as exc:
        raise HTTPException(status_code=503, detail=f"Runner is not reachable: {exc}") from exc
    return {
        "ok": bool(data.get("ok")),
        "session_id": data.get("sessionId") or "",
        "current_url": data.get("currentUrl") or "",
        "title": data.get("title") or "",
        "screenshot_base64": data.get("screenshotBase64") or "",
        "viewport": data.get("viewport") or {},
        "error": data.get("error"),
    }


@router.post("/inspect/{session_id}/hover")
async def inspect_hover(session_id: str, body: InspectHoverIn) -> dict:
    try:
        async with httpx.AsyncClient(timeout=3) as client:
            response = await client.post(
                f"{settings.runner_url.rstrip('/')}/internal/inspect/{session_id}/hover",
                json={"x": body.x, "y": body.y},
            )
            data = response.json()
    except httpx.HTTPError as exc:
        raise HTTPException(status_code=503, detail=f"Selection lookup failed: {exc}") from exc
    return {"ok": bool(data.get("ok")), "element": data.get("element"), "error": data.get("error")}


@router.post("/inspect/{session_id}/close")
async def inspect_close(session_id: str) -> dict:
    try:
        async with httpx.AsyncClient(timeout=5) as client:
            await client.post(f"{settings.runner_url.rstrip('/')}/internal/inspect/{session_id}/close")
    except httpx.HTTPError:
        return {"ok": False}
    return {"ok": True}


@router.post("/pick")
async def pick(body: PickIn) -> dict:
    payload = {
        "url": body.url,
        "browser": body.browser,
        "headless": body.headless,
    }
    try:
        async with httpx.AsyncClient(timeout=90) as client:
            response = await client.post(f"{settings.runner_url.rstrip('/')}/internal/pick", json=payload)
    except httpx.HTTPError as exc:
        raise HTTPException(status_code=503, detail=f"Runner is not reachable: {exc}") from exc
    data = response.json()
    page = data.get("page") or {}
    return {
        "ok": bool(data.get("ok")),
        "current_url": data.get("currentUrl") or "",
        "title": data.get("title") or "",
        "screenshot_base64": data.get("screenshotBase64") or "",
        "viewport": page.get("viewport") or {},
        "elements": page.get("elements") or [],
        "error": data.get("error"),
    }


@router.post("/probe")
async def probe(body: ProbeIn) -> dict:
    payload = {
        "url": body.url,
        "strategy": body.strategy,
        "query": body.query,
        "timeoutSeconds": body.timeout_seconds,
        "browser": body.browser,
        "headless": body.headless,
    }
    try:
        async with httpx.AsyncClient(timeout=90) as client:
            response = await client.post(f"{settings.runner_url.rstrip('/')}/internal/probe", json=payload)
    except httpx.HTTPError as exc:
        raise HTTPException(status_code=503, detail=f"Runner is not reachable: {exc}") from exc
    try:
        data = response.json()
    except Exception as exc:
        raise HTTPException(status_code=502, detail="Runner returned a non-JSON probe result") from exc
    if response.status_code >= 500:
        raise HTTPException(status_code=502, detail=data.get("error") or "Probe failed")
    return {
        "ok": bool(data.get("ok")),
        "count": data.get("count") or 0,
        "samples": data.get("samples") or [],
        "current_url": data.get("currentUrl") or "",
        "title": data.get("title") or "",
        "screenshot_base64": data.get("screenshotBase64") or "",
        "error": data.get("error"),
    }
