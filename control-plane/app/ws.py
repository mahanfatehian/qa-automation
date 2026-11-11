import logging
from fastapi import WebSocket

logger = logging.getLogger(__name__)


class ConnectionManager:
    def __init__(self) -> None:
        self.rooms: dict[str, set[WebSocket]] = {}

    async def connect(self, run_id: str, websocket: WebSocket) -> None:
        await websocket.accept()
        self.rooms.setdefault(run_id, set()).add(websocket)

    def disconnect(self, run_id: str, websocket: WebSocket) -> None:
        room = self.rooms.get(run_id)
        if not room:
            return
        room.discard(websocket)
        if not room:
            self.rooms.pop(run_id, None)

    async def broadcast(self, run_id: str, message: dict) -> None:
        dead: list[WebSocket] = []
        for websocket in list(self.rooms.get(run_id, set())):
            try:
                await websocket.send_json(message)
            except Exception:
                logger.debug("dropping closed socket for %s", run_id)
                dead.append(websocket)
        for websocket in dead:
            self.disconnect(run_id, websocket)


manager = ConnectionManager()
