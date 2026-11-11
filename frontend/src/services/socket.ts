import type { TelemetryEvent } from "../types/blueprint";

export function openRunSocket(runId: string, onEvent: (event: TelemetryEvent) => void): WebSocket {
  const protocol = window.location.protocol === "https:" ? "wss" : "ws";
  const socket = new WebSocket(`${protocol}://${window.location.host}/ws/runs/${runId}`);
  socket.onmessage = (message) => {
    try {
      onEvent(JSON.parse(message.data) as TelemetryEvent);
    } catch {
      // ignore non-json keepalives
    }
  };
  const ping = window.setInterval(() => {
    if (socket.readyState === WebSocket.OPEN) socket.send("ping");
  }, 20000);
  socket.addEventListener("close", () => window.clearInterval(ping));
  return socket;
}
