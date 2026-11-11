import { useEffect, useState } from "react";
import { api } from "../services/api";
import type { Schedule } from "../types/blueprint";

type Props = {
  open: boolean;
  workflowId: string | null;
  webhookToken: string;
  onClose: () => void;
};

export function ScheduleModal({ open, workflowId, webhookToken, onClose }: Props) {
  const [rows, setRows] = useState<Schedule[]>([]);
  const [scheduleType, setScheduleType] = useState<"CRON" | "INTERVAL">("CRON");
  const [cron, setCron] = useState("0 */6 * * *");
  const [interval, setIntervalSeconds] = useState(900);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!open || !workflowId) return;
    api.schedules(workflowId).then(setRows).catch((err: Error) => setError(err.message));
  }, [open, workflowId]);

  if (!open || !workflowId) return null;
  const hook = `${window.location.origin}/api/v1/webhooks/trigger/${workflowId}?token=${webhookToken}&wait=true`;

  async function add() {
    setError("");
    try {
      const created = await api.createSchedule(workflowId!, {
        name: scheduleType === "CRON" ? `Cron ${cron}` : `Every ${interval}s`,
        schedule_type: scheduleType,
        cron_expression: scheduleType === "CRON" ? cron : null,
        interval_seconds: scheduleType === "INTERVAL" ? interval : null,
        enabled: true,
      });
      setRows((current) => [created, ...current]);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not save schedule");
    }
  }

  return (
    <div className="fixed inset-0 z-20 grid place-items-center bg-black/60 p-6" onClick={onClose}>
      <div className="panel w-full max-w-xl rounded-lg border border-line p-5" onClick={(event) => event.stopPropagation()}>
        <div className="mb-4 flex items-center justify-between">
          <h2 className="text-lg">Automatic runs</h2>
          <button className="btn" onClick={onClose}>Close</button>
        </div>
        <p className="mb-3 text-sm text-[#8ea6bb]">This blueprint can run on a cron, a fixed interval, or a CI webhook. It is still the same node graph.</p>
        <div className="mb-3 flex gap-2">
          <select className="btn" value={scheduleType} onChange={(event) => setScheduleType(event.target.value as "CRON" | "INTERVAL")}>
            <option value="CRON">Cron</option>
            <option value="INTERVAL">Interval</option>
          </select>
          {scheduleType === "CRON" ? (
            <input className="btn flex-1" value={cron} onChange={(event) => setCron(event.target.value)} />
          ) : (
            <input className="btn w-32" type="number" min={15} value={interval} onChange={(event) => setIntervalSeconds(Number(event.target.value))} />
          )}
          <button className="btn btn-run" onClick={add}>Add</button>
        </div>
        {error && <p className="mb-2 text-sm text-[#ff8fa3]">{error}</p>}
        <ul className="mb-4 max-h-40 space-y-1 overflow-auto text-sm">
          {rows.map((row) => (
            <li key={row.id} className="flex items-center justify-between rounded border border-line px-2 py-1">
              <span>{row.enabled ? "on" : "off"} · {row.schedule_type === "CRON" ? row.cron_expression : `${row.interval_seconds}s`}</span>
              <span className="flex gap-2">
                <button className="btn" onClick={() => api.updateSchedule(row.id, { enabled: !row.enabled }).then((next) => setRows((current) => current.map((item) => item.id === next.id ? next : item)))}>Toggle</button>
                <button className="btn btn-danger" onClick={() => api.removeSchedule(row.id).then(() => setRows((current) => current.filter((item) => item.id !== row.id)))}>Delete</button>
              </span>
            </li>
          ))}
          {rows.length === 0 && <li className="text-[#8ea6bb]">No automatic runs yet.</li>}
        </ul>
        <p className="mb-1 font-mono text-[10px] uppercase tracking-[0.14em] text-[#8ea6bb]">Webhook</p>
        <textarea className="field w-full" readOnly value={`curl -X POST "${hook}"`} />
      </div>
    </div>
  );
}
