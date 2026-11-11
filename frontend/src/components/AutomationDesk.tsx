import { useEffect, useState } from "react";
import { api } from "../services/api";
import type { Schedule } from "../types/blueprint";

type Props = {
  workflowId: string | null;
  workflowName: string;
  webhookToken: string;
  runs: Array<{ id: string; status: string; trigger: string; created_at: string; variation_label?: string | null; error_message?: string | null }>;
};

const presets = [
  { label: "Every 15 minutes", type: "CRON" as const, cron: "*/15 * * * *", interval: null },
  { label: "Every hour", type: "CRON" as const, cron: "0 * * * *", interval: null },
  { label: "Every day at 09:00 UTC", type: "CRON" as const, cron: "0 9 * * *", interval: null },
  { label: "Every 15 minutes from now", type: "INTERVAL" as const, cron: null, interval: 900 },
];

export function AutomationDesk({ workflowId, workflowName, webhookToken, runs }: Props) {
  const [rows, setRows] = useState<Schedule[]>([]);
  const [cron, setCron] = useState("0 9 * * *");
  const [error, setError] = useState("");
  const [logs, setLogs] = useState<Array<{ id: string; level: string; message: string; created_at: string }>>([]);

  useEffect(() => {
    if (!workflowId) return;
    api.schedules(workflowId).then(setRows).catch((err: Error) => setError(err.message));
    api.automationLogs(workflowId).then(setLogs).catch(() => undefined);
  }, [workflowId]);

  if (!workflowId) {
    return <section className="panel min-h-0 flex-1 p-4 text-sm text-[#999]">Save a blueprint before scheduling it.</section>;
  }
  const id = workflowId;
  const hook = `${window.location.origin}/api/v1/webhooks/trigger/${id}?token=${webhookToken}&wait=true`;

  async function add(type: "CRON" | "INTERVAL", expression: string | null, seconds: number | null, name: string) {
    setError("");
    try {
      const created = await api.createSchedule(id, {
        name,
        schedule_type: type,
        cron_expression: expression,
        interval_seconds: seconds,
        enabled: true,
      });
      setRows((current) => [created, ...current]);
    } catch (err) {
      setError(err instanceof Error && err.message === "Failed to fetch" ? "The API did not answer. Wait a second and try again." : err instanceof Error ? err.message : "Could not save schedule");
    }
  }

  return (
    <section className="panel min-h-0 flex-1 overflow-auto p-4" data-testid="automation-desk">
      <h2 className="text-lg">Automation</h2>
      <p className="mb-4 text-sm text-[#999]">The first three buttons follow the clock. The last one starts 15 minutes after you add it, then repeats. A finished run writes a log a notifier can read later.</p>
      <div className="mb-4 flex flex-wrap gap-2">
        {presets.map((preset) => (
          <button key={preset.label} className="btn" onClick={() => add(preset.type, preset.cron, preset.interval, preset.label)}>{preset.label}</button>
        ))}
      </div>
      <div className="mb-4 flex gap-2">
        <input className="btn flex-1" value={cron} onChange={(event) => setCron(event.target.value)} aria-label="Cron expression" />
        <button className="btn btn-run" onClick={() => add("CRON", cron, null, `Cron ${cron}`)}>Add cron</button>
      </div>
      {error && <p className="mb-3 text-sm text-[#d46a6a]">{error}</p>}
      <ul className="space-y-2 text-sm">
        {rows.map((row) => (
          <li key={row.id} className="flex items-center justify-between border border-line px-3 py-2">
            <span>{row.enabled ? "On" : "Off"} · {row.name} · {row.schedule_type === "CRON" ? row.cron_expression : `every ${row.interval_seconds}s`}{row.last_run_at ? ` · last ${row.last_run_at.slice(0, 16).replace("T", " ")}` : " · never fired"}</span>
            <span className="flex gap-2">
              <button className="btn" onClick={() => api.updateSchedule(row.id, { enabled: !row.enabled }).then((next) => setRows((current) => current.map((item) => item.id === next.id ? next : item)))}>{row.enabled ? "Pause" : "Resume"}</button>
              <button className="btn btn-danger" onClick={() => api.removeSchedule(row.id).then(() => setRows((current) => current.filter((item) => item.id !== row.id)))}>Delete</button>
            </span>
          </li>
        ))}
        {rows.length === 0 && <li className="text-[#999]">No clock yet.</li>}
      </ul>
      <p className="mb-2 mt-6 font-mono text-[10px] uppercase tracking-[0.14em] text-[#999]">Runs of {workflowName || "this blueprint"}</p>
      <ul className="mb-4 space-y-1 text-sm">
        {runs.slice(0, 12).map((run) => (
          <li key={run.id} className="border-b border-line py-1">
            <span className={run.status === "PASSED" ? "text-[#7d9a6a]" : run.status === "FAILED" ? "text-[#d46a6a]" : "text-[#7f6df2]"}>{run.status.toLowerCase()}</span>
            <span className="ml-2">{workflowName}</span>
            <span className="ml-2 text-[#999]">{run.trigger.toLowerCase()}</span>
            <span className="ml-2 text-[#dcddde]">{run.variation_label || "single run"}</span>
          </li>
        ))}
        {runs.length === 0 && <li className="text-[#999]">No runs of this blueprint yet.</li>}
      </ul>
      <p className="mb-2 mt-6 font-mono text-[10px] uppercase tracking-[0.14em] text-[#999]">Automation log</p>
      <ul className="mb-4 space-y-1 text-sm">
        {logs.map((item) => (
          <li key={item.id} className={item.level === "error" ? "text-[#d46a6a]" : item.level === "warn" ? "text-[#d4a017]" : "text-[#dcddde]"}>
            {item.created_at.slice(11, 16)} · {item.message}
          </li>
        ))}
        {logs.length === 0 && <li className="text-[#999]">No automatic run has finished yet.</li>}
      </ul>
      <p className="mb-1 mt-6 font-mono text-[10px] uppercase tracking-[0.14em] text-[#999]">CI webhook</p>
      <textarea className="field w-full" readOnly value={`curl -X POST "${hook}"`} />
    </section>
  );
}
