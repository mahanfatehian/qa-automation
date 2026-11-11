import { useRef, useState } from "react";
import { CATALOG_BY_TYPE } from "../catalog";
import type { TelemetryEvent, TestRun } from "../types/blueprint";

type Props = {
  run: TestRun | null;
  events: TelemetryEvent[];
  shot: string;
  runs: TestRun[];
  height: number;
  onPick: (event: TelemetryEvent) => void;
  onOpenRun: (run: TestRun) => void;
};

export function LiveRunOverlay({ run, events, shot, runs, height, onPick, onOpenRun }: Props) {
  const well = useRef<HTMLElement | null>(null);
  const [logWidth, setLogWidth] = useState(460);
  const steps = latestSteps(events);
  const failed = steps.find((event) => event.event_type.endsWith("FAILED"));
  const summary = failed ? plain(failed.message || run?.error_message || "") : "";
  return (
    <section ref={well} className="panel flex shrink-0 border-t border-line" style={{ height }} data-testid="live-well">
      <div className="flex min-w-0 shrink-0 overflow-hidden" style={{ width: logWidth }}>
      <div className="w-[148px] shrink-0 overflow-auto border-r border-line p-3">
        <p className="mark text-[#8f857a]" data-testid="run-status">
          {run ? `${word(run.status)} · ${run.trigger.toLowerCase()}` : "Idle"}
        </p>
        {summary && <p className="mt-2 text-sm leading-5 text-[#f4efe6]">{summary}</p>}
        <p className="mark mb-2 mt-4 text-[#8f857a]">Earlier runs</p>
        {runs.slice(0, 8).map((item) => (
          <button key={item.id} className="mb-1 flex w-full items-baseline justify-between gap-2 text-left text-xs" data-testid="past-run" onClick={() => onOpenRun(item)}>
            <span className="text-[#cbbba8]">{clock(item.created_at)}</span>
            <span className={item.status === "PASSED" ? "text-[#c9d7b0]" : item.status === "FAILED" ? "text-[#e7a090]" : "text-[#e0b15a]"}>{word(item.status)}</span>
          </button>
        ))}
      </div>
      <div className="min-w-0 flex-1 overflow-auto px-3 py-2" data-testid="run-steps">
        {steps.length === 0 && <p className="pt-3 text-sm text-[#8f857a]">Each step lands here once: what it was, how long it took, and what it said.</p>}
        {steps.map((event) => (
          <button key={event.node_id} className="step-row" data-testid="run-step" onClick={() => onPick(event)}>
            <span className={tone(event.event_type)}>{word(event.event_type.replace("NODE_", ""))}</span>
            <span>{(event.node_type && CATALOG_BY_TYPE[event.node_type]?.title) || event.node_type || "Step"}</span>
            <span className="text-[#8f857a]">{event.duration_ms ? `${event.duration_ms}ms` : ""}</span>
            <span className="step-note truncate text-[#cbbba8]">{plain(event.message)}</span>
          </button>
        ))}
      </div>
      </div>
      <div
        className="log-rail"
        data-testid="log-rail"
        onPointerDown={(event) => {
          event.preventDefault();
          const bounds = well.current?.getBoundingClientRect();
          if (!bounds) return;
          const move = (pointer: PointerEvent) => {
            setLogWidth(Math.min(bounds.width - 180, Math.max(220, pointer.clientX - bounds.left)));
          };
          const up = () => {
            window.removeEventListener("pointermove", move);
            window.removeEventListener("pointerup", up);
          };
          window.addEventListener("pointermove", move);
          window.addEventListener("pointerup", up);
        }}
      />
      <div className="grid min-h-0 min-w-0 flex-1 place-items-center bg-[#16130f] p-2" data-testid="live-shot">
        {shot ? <img alt="Step screenshot" className="max-h-full max-w-full object-contain" src={`data:image/png;base64,${shot}`} /> : <p className="px-4 text-center text-sm text-[#8f857a]">Drag the side bar to give this page more room, or less.</p>}
      </div>
    </section>
  );
}

function latestSteps(events: TelemetryEvent[]) {
  const rank = (kind: string) => kind.endsWith("FAILED") ? 3 : kind.endsWith("PASSED") ? 2 : 1;
  const byNode = new Map<string, TelemetryEvent>();
  for (const event of events) {
    if (!event.event_type?.startsWith("NODE_") || !event.node_id) continue;
    const prev = byNode.get(event.node_id);
    if (!prev || rank(event.event_type) >= rank(prev.event_type)) byNode.set(event.node_id, event);
  }
  return [...byNode.values()];
}

function plain(message?: string | null) {
  if (!message) return "";
  return message
    .replace(/(https?:\/\/\S+\s*[—-]\s*)+/g, "")
    .replace("This step needs a selector or search text", "No selector on this step. Use Select on page.")
    .replace("Find needs a selector. Use Select on page, or type a CSS, XPath, or text query.", "No selector on this step. Use Select on page.")
    .trim();
}

function word(status: string) {
  if (status === "PASSED") return "Passed";
  if (status === "FAILED") return "Failed";
  if (status === "RUNNING") return "Running";
  if (status === "QUEUED") return "Queued";
  return status.charAt(0) + status.slice(1).toLowerCase();
}

function tone(eventType: string) {
  if (eventType.endsWith("PASSED")) return "text-[#c9d7b0]";
  if (eventType.endsWith("FAILED")) return "text-[#e7a090]";
  return "text-[#e0b15a]";
}

function clock(value: string) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "";
  return date.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });
}
