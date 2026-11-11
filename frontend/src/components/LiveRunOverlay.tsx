import type { TelemetryEvent, TestRun } from "../types/blueprint";

type Props = {
  run: TestRun | null;
  events: TelemetryEvent[];
  shot: string;
  onPick: (event: TelemetryEvent) => void;
};

export function LiveRunOverlay({ run, events, shot, onPick }: Props) {
  const steps = events.filter((event) => event.event_type?.startsWith("NODE_"));
  return (
    <section className="panel grid h-52 shrink-0 grid-cols-[280px_1fr_280px] border-t border-line">
      <div className="overflow-auto border-r border-line p-3">
        <p className="mb-2 font-mono text-[10px] uppercase tracking-[0.16em] text-[#8ea6bb]">
          Run {run ? run.status : "idle"} {run ? `· ${run.trigger}` : ""}
        </p>
        {steps.length === 0 && <p className="text-sm text-[#8ea6bb]">Step feedback shows up here: status, duration, text, and the screenshot taken at that pin.</p>}
        {steps.map((event, index) => (
          <button key={`${event.node_id}-${event.event_type}-${index}`} className="mb-1 block w-full rounded px-2 py-1 text-left text-xs hover:bg-[#122033]" onClick={() => onPick(event)}>
            <span className={tone(event.event_type)}>{event.event_type.replace("NODE_", "")}</span>
            <span className="ml-2 text-[#d5e4f0]">{event.node_type}</span>
            <span className="ml-2 text-[#8ea6bb]">{event.duration_ms ? `${event.duration_ms}ms` : ""}</span>
            {event.message && <span className="mt-0.5 block truncate text-[#8ea6bb]">{event.message}</span>}
          </button>
        ))}
      </div>
      <div className="grid place-items-center bg-[#070d14]">
        {shot ? <img alt="Step screenshot" className="max-h-48 max-w-full object-contain" src={`data:image/png;base64,${shot}`} /> : <p className="text-sm text-[#8ea6bb]">No screenshot yet</p>}
      </div>
      <div className="overflow-auto border-l border-line p-3 text-xs text-[#d5e4f0]">
        <p className="mb-2 font-mono text-[10px] uppercase tracking-[0.16em] text-[#8ea6bb]">Output</p>
        <pre className="whitespace-pre-wrap font-mono text-[11px] text-[#9ad8ff]">{JSON.stringify(steps.at(-1)?.output_data || {}, null, 2)}</pre>
        {run?.error_message && <p className="mt-2 text-[#ff8fa3]">{run.error_message}</p>}
      </div>
    </section>
  );
}

function tone(eventType: string) {
  if (eventType.endsWith("PASSED")) return "text-[#3dff9a]";
  if (eventType.endsWith("FAILED")) return "text-[#ff4d6a]";
  return "text-[#ffe14a]";
}
