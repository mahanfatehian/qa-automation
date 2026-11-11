import { useEffect, useMemo, useRef, useState } from "react";
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

const ROW = 48;
const LANE = 16;
const COLORS = ["#9aa0a6", "#7f6df2", "#49c2ff", "#7d9a6a", "#d4a017", "#d46a6a", "#c084fc", "#2dd4bf", "#fb923c", "#f472b6", "#a3e635", "#38bdf8"];

type Row = {
  event: TelemetryEvent;
  key: string;
  branch: string;
  lane: number;
  fork: number;
};

export function LiveRunOverlay({ run, events, shot, runs, height, onPick, onOpenRun }: Props) {
  const well = useRef<HTMLElement | null>(null);
  const picked = useRef<string | null>(null);
  const onPickRef = useRef(onPick);
  const lastAuto = useRef("");
  onPickRef.current = onPick;
  const [logWidth, setLogWidth] = useState(620);
  const [selected, setSelected] = useState("");
  const rows = useMemo(() => buildRows(events), [events]);
  const failed = rows.find((row) => row.event.event_type.endsWith("FAILED"));
  const summary = cleanFail(failed?.event.message || run?.error_message || "");
  const lanes = rows.reduce((max, row) => Math.max(max, row.lane), 0);
  const graphWidth = (lanes + 1) * LANE + 18;

  useEffect(() => {
    picked.current = null;
    lastAuto.current = "";
    setSelected("");
  }, [run?.id]);

  useEffect(() => {
    const last = rows.at(-1);
    if (!last || picked.current || lastAuto.current === last.key) return;
    lastAuto.current = last.key;
    setSelected(last.key);
    onPickRef.current(last.event);
  }, [rows]);

  function choose(row: Row) {
    picked.current = row.key;
    setSelected(row.key);
    onPick(row.event);
  }

  return (
    <section ref={well} className="panel flex shrink-0 overflow-hidden border-t border-line" style={{ height }} data-testid="live-well">
      <div className="flex min-w-0 shrink-0 overflow-hidden" style={{ width: logWidth }}>
        <div className="flex w-[168px] shrink-0 flex-col overflow-hidden border-r border-line">
          <p className="mark px-2 pb-1 pt-2 text-[#999]" data-testid="run-status">Runs</p>
          <div className="min-h-0 flex-1 overflow-auto px-1 pb-2">
            {runs.slice(0, 12).map((item) => (
              <button key={item.id} className={`run-row ${run?.id === item.id ? "is-on" : ""}`} data-testid="past-run" onClick={() => onOpenRun(item)}>
                <span className="text-[#dcddde]">{clock(item.created_at)}</span>
                <span className={item.status === "PASSED" ? "text-[#7d9a6a]" : item.status === "FAILED" ? "text-[#d46a6a]" : "text-[#7f6df2]"}>{word(item.status)}</span>
              </button>
            ))}
            {runs.length === 0 && <p className="px-1 text-xs text-[#999]">No runs yet.</p>}
          </div>
        </div>
        <div className="min-w-0 flex-1 overflow-auto" data-testid="run-steps">
          {summary && <p className="border-b border-line px-3 py-2 text-sm text-[#e7a090]">{summary}</p>}
          {rows.length === 0 && <p className="px-3 pt-3 text-sm text-[#999]">Each path lands here as its own line. Click a dot to see that page.</p>}
          {rows.length > 0 && (
            <div className="flex min-w-full" style={{ minWidth: graphWidth + 280 }}>
              <Graph rows={rows} width={graphWidth} selected={selected} />
              <div className="min-w-0 flex-1">
                {rows.map((row) => (
                  <button key={row.key} className={`tree-row ${selected === row.key ? "is-on" : ""}`} data-testid="run-step" onClick={() => choose(row)}>
                    <span className={tone(row.event.event_type)}>{word(row.event.event_type.replace("NODE_", ""))}</span>
                    <span className="truncate">{(row.event.node_type && CATALOG_BY_TYPE[row.event.node_type]?.title) || row.event.node_type || "Step"}</span>
                    <span className="text-[#999]">{row.event.duration_ms ? `${row.event.duration_ms}ms` : ""}</span>
                    <span className="step-note truncate text-[#dcddde]">{stepNote(row.event)}</span>
                  </button>
                ))}
              </div>
            </div>
          )}
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
            setLogWidth(Math.min(bounds.width - 180, Math.max(280, pointer.clientX - bounds.left)));
          };
          const up = () => {
            window.removeEventListener("pointermove", move);
            window.removeEventListener("pointerup", up);
          };
          window.addEventListener("pointermove", move);
          window.addEventListener("pointerup", up);
        }}
      />
      <div className="relative min-h-0 min-w-0 flex-1 overflow-hidden bg-[#161616]" data-testid="live-shot">
        {shot ? <img alt="Step screenshot" className="absolute inset-0 h-full w-full object-contain" src={`data:image/png;base64,${shot}`} /> : <p className="px-4 pt-6 text-center text-sm text-[#999]">The page stays in this frame. It does not move the canvas.</p>}
      </div>
    </section>
  );
}

function Graph({ rows, width, selected }: { rows: Row[]; width: number; selected: string }) {
  const height = rows.length * ROW;
  const xOf = (lane: number) => 10 + lane * LANE;
  const yOf = (index: number) => index * ROW + ROW / 2;
  const first = new Map<string, number>();
  const last = new Map<string, number>();
  rows.forEach((row, index) => {
    if (!first.has(row.branch)) first.set(row.branch, index);
    last.set(row.branch, index);
  });
  const forks = [...first.entries()].filter(([branch]) => branch).map(([branch, index]) => ({
    branch,
    from: forkSource(rows, branch),
    to: index,
  }));
  return (
    <svg width={width} height={height} className="shrink-0" aria-hidden="true">
      {[...first.entries()].map(([branch, start]) => (
        <line
          key={`lane-${branch}`}
          x1={xOf(rows[start].lane)}
          y1={yOf(start)}
          x2={xOf(rows[start].lane)}
          y2={yOf(last.get(branch) ?? start)}
          stroke={color(branch)}
          strokeWidth={2}
        />
      ))}
      {forks.map((fork) => {
        const x1 = xOf(rows[fork.from].lane);
        const y1 = yOf(fork.from);
        const x2 = xOf(rows[fork.to].lane);
        const y2 = yOf(fork.to);
        const mid = y1 + Math.max(10, (y2 - y1) / 2);
        return (
          <path
            key={`fork-${fork.branch}`}
            d={`M ${x1} ${y1} C ${x1} ${mid}, ${x2} ${mid}, ${x2} ${y2}`}
            fill="none"
            stroke={color(fork.branch)}
            strokeWidth={2}
          />
        );
      })}
      {rows.map((row, index) => (
        <circle
          key={row.key}
          cx={xOf(row.lane)}
          cy={yOf(index)}
          r={selected === row.key ? 5 : 3.5}
          fill={color(row.branch)}
          stroke={selected === row.key ? "#f4efe6" : "#1e1e1e"}
          strokeWidth={1.5}
        />
      ))}
    </svg>
  );
}

function buildRows(events: TelemetryEvent[]): Row[] {
  const collapsed = collapse(events);
  const laneOf = new Map<string, number>();
  laneOf.set("", 0);
  let next = 1;
  return collapsed.map((event, index) => {
    const branch = branchOf(event);
    if (!laneOf.has(branch)) laneOf.set(branch, branch ? next++ : 0);
    return {
      event,
      key: `${branch}:${event.node_id}:${index}`,
      branch,
      lane: laneOf.get(branch) ?? 0,
      fork: forkCount(event),
    };
  });
}

function collapse(events: TelemetryEvent[]) {
  const rank = (kind: string) => kind.endsWith("FAILED") ? 3 : kind.endsWith("PASSED") ? 2 : 1;
  const rows: TelemetryEvent[] = [];
  const open = new Map<string, number>();
  const bare = new Map<string, number>();
  for (const event of events) {
    if (!event.event_type?.startsWith("NODE_") || !event.node_id) continue;
    const branch = branchOf(event);
    const id = `${branch}:${event.node_id}`;
    const terminal = !event.event_type.endsWith("RUNNING");
    if (terminal && !branch && bare.has(event.node_id)) {
      const at = bare.get(event.node_id) ?? 0;
      if (rank(event.event_type) >= rank(rows[at].event_type)) rows[at] = event;
      bare.delete(event.node_id);
      open.set(id, at);
      continue;
    }
    if (terminal && branch && bare.has(event.node_id)) {
      const at = bare.get(event.node_id) ?? 0;
      if (rows[at]?.event_type.endsWith("RUNNING")) {
        rows[at] = event;
        bare.delete(event.node_id);
        open.set(id, at);
        continue;
      }
    }
    if (event.event_type.endsWith("RUNNING") || !open.has(id)) {
      open.set(id, rows.length);
      if (!branch) bare.set(event.node_id, rows.length);
      rows.push(event);
      continue;
    }
    const at = open.get(id) ?? rows.length - 1;
    if (rank(event.event_type) >= rank(rows[at].event_type)) rows[at] = event;
    if (terminal) {
      open.delete(id);
      bare.delete(event.node_id);
    }
  }
  return rows.filter((event, index, all) => {
    if (!event.event_type.endsWith("RUNNING")) return true;
    return !all.some((other, otherIndex) => otherIndex !== index && other.node_id === event.node_id && !other.event_type.endsWith("RUNNING"));
  });
}

function branchOf(event: TelemetryEvent) {
  const raw = String(event.output_data?.branch || "");
  if (raw) return raw.replace(/^branch\s+/i, "");
  const match = String(event.message || "").match(/^branch\s+([\d.]+)/i);
  return match?.[1] || "";
}

function forkCount(event: TelemetryEvent) {
  const match = String(event.message || "").match(/forked\s+(\d+)\s+browsers/i);
  return match ? Number(match[1]) : 0;
}

function parentBranch(branch: string) {
  const cut = branch.lastIndexOf(".");
  return cut < 0 ? "" : branch.slice(0, cut);
}

function forkSource(rows: Row[], branch: string) {
  const parent = parentBranch(branch);
  const first = rows.findIndex((row) => row.branch === branch);
  for (let index = first - 1; index >= 0; index -= 1) {
    if (rows[index].branch === parent && rows[index].fork > 1) return index;
  }
  for (let index = first - 1; index >= 0; index -= 1) {
    if (rows[index].branch === parent) return index;
  }
  return Math.max(0, first - 1);
}

function color(branch: string) {
  if (!branch) return COLORS[0];
  let hash = 0;
  for (const char of branch) hash = (hash * 33 + char.charCodeAt(0)) >>> 0;
  return COLORS[(hash % (COLORS.length - 1)) + 1];
}

function stepNote(event: TelemetryEvent) {
  const message = plain(event.message).replace(/^branch\s+[\d.]+\s*·\s*/i, "");
  const url = shortUrl(String(event.output_data?.url || ""));
  return [message, url].filter(Boolean).join(" · ");
}

function shortUrl(url: string) {
  try {
    const parsed = new URL(url);
    const path = parsed.pathname === "/" ? "" : parsed.pathname;
    const text = parsed.host + path;
    return text.length > 42 ? `${text.slice(0, 42)}…` : text;
  } catch {
    return url.length > 42 ? `${url.slice(0, 42)}…` : url;
  }
}

function cleanFail(message?: string | null) {
  const text = plain(message).replace(/^branch\s+[\d.]+\s*·\s*/i, "");
  return text.length > 140 ? `${text.slice(0, 140)}…` : text;
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
  if (eventType.endsWith("PASSED")) return "text-[#7d9a6a]";
  if (eventType.endsWith("FAILED")) return "text-[#d46a6a]";
  return "text-[#7f6df2]";
}

function clock(value: string) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "";
  return date.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });
}
