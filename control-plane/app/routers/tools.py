import httpx
from fastapi import APIRouter, HTTPException

from app.config import settings
from app.schemas import InspectHoverIn, InspectResolveIn, InspectStartIn, PickIn, ProbeIn
from app.selector import synthesize

router = APIRouter(prefix="/api/v1/tools", tags=["tools"])
_html_cache: dict[str, str] = {}
_runner = httpx.AsyncClient(timeout=httpx.Timeout(45.0, connect=2.0), limits=httpx.Limits(max_keepalive_connections=8))


@router.post("/inspect/start")
async def inspect_start(body: InspectStartIn) -> dict:
    payload = {
        "nodes": body.nodes,
        "edges": body.edges,
        "stopBefore": body.stop_before,
        "variables": body.variables,
    }
    try:
        response = await _runner.post(f"{settings.runner_url.rstrip('/')}/internal/inspect/start", json=payload)
        data = response.json()
    except httpx.HTTPError as exc:
        raise HTTPException(status_code=503, detail=f"Runner is not reachable: {exc}") from exc
    return {
        "ok": bool(data.get("ok")),
        "session_id": data.get("sessionId") or "",
        "current_url": data.get("currentUrl") or "",
        "title": data.get("title") or "",
        "screenshot_base64": data.get("screenshotBase64") or "",
        "screenshot_mime": data.get("screenshotMime") or "image/png",
        "viewport": data.get("viewport") or {},
        "error": data.get("error"),
    }


@router.post("/inspect/{session_id}/hover")
async def inspect_hover(session_id: str, body: InspectHoverIn) -> dict:
    try:
        response = await _runner.post(
            f"{settings.runner_url.rstrip('/')}/internal/inspect/{session_id}/hover",
            json={"x": body.x, "y": body.y},
            timeout=3,
        )
        data = response.json()
    except httpx.HTTPError as exc:
        raise HTTPException(status_code=503, detail=f"Selection lookup failed: {exc}") from exc
    return {"ok": bool(data.get("ok")), "element": data.get("element"), "error": data.get("error")}


@router.post("/inspect/{session_id}/resolve")
async def inspect_resolve(session_id: str, body: InspectResolveIn) -> dict:
    fingerprint = body.model_dump()
    selectors = synthesize("", fingerprint)
    stable = [item for item in selectors if int(item.get("rank") or 99) <= 6]
    if stable:
        return {"ok": True, "selectors": stable}
    if selectors:
        return {"ok": True, "selectors": selectors}
    html = await _cached_html(session_id, refresh=False)
    selectors = synthesize(html, fingerprint)
    if not selectors:
        html = await _cached_html(session_id, refresh=True)
        selectors = synthesize(html, fingerprint)
    return {"ok": bool(selectors), "selectors": selectors}


async def _cached_html(session_id: str, refresh: bool) -> str:
    if not refresh and session_id in _html_cache:
        return _html_cache[session_id]
    try:
        async with httpx.AsyncClient(timeout=20) as client:
            response = await client.post(
                f"{settings.runner_url.rstrip('/')}/internal/inspect/{session_id}/source",
                json={"refresh": refresh},
            )
            html = str(response.json().get("html") or "")
    except httpx.HTTPError:
        return _html_cache.get(session_id, "")
    if html:
        _html_cache[session_id] = html
    return html


@router.post("/inspect/{session_id}/close")
async def inspect_close(session_id: str) -> dict:
    try:
        async with httpx.AsyncClient(timeout=5) as client:
            await client.post(f"{settings.runner_url.rstrip('/')}/internal/inspect/{session_id}/close")
    except httpx.HTTPError:
        _html_cache.pop(session_id, None)
        return {"ok": False}
    _html_cache.pop(session_id, None)
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
