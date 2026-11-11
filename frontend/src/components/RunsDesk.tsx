import { CATALOG_BY_TYPE } from "../catalog";
import type { NodeResult, TestRun } from "../types/blueprint";

type Props = {
  runs: TestRun[];
  results: NodeResult[];
  onRun: () => void;
  onOpen: (run: TestRun) => void;
  onDelete: (run: TestRun) => void;
};

export function RunsDesk({ runs, results, onRun, onOpen, onDelete }: Props) {
  return (
    <section className="panel min-h-0 flex-1 overflow-auto p-4" data-testid="runs-desk">
      <div className="mb-4 flex items-center justify-between">
        <div>
          <h2 className="text-lg">Runs</h2>
          <p className="text-sm text-[#999]">Start a run of this blueprint, or open one that already finished.</p>
        </div>
        <button className="btn btn-run" onClick={onRun}>Run now</button>
      </div>
      <table className="w-full text-left text-sm">
        <thead className="text-[#999]">
          <tr>
            <th className="py-2">When</th>
            <th>How</th>
            <th>Status</th>
            <th>Note</th>
            <th />
          </tr>
        </thead>
        <tbody>
          {runs.map((run) => (
            <tr key={run.id} className="border-t border-line">
              <td className="py-2">{when(run.created_at)}</td>
              <td>{run.trigger.toLowerCase()}</td>
              <td className={run.status === "PASSED" ? "text-[#7d9a6a]" : run.status === "FAILED" ? "text-[#d46a6a]" : "text-[#7f6df2]"}>{run.status.toLowerCase()}</td>
              <td className="max-w-md truncate text-[#999]">{run.variation_label || run.error_message || "single run"}</td>
              <td className="flex justify-end gap-2 py-2">
                <button className="btn" onClick={() => onOpen(run)}>Open</button>
                <button className="btn btn-danger" onClick={() => onDelete(run)}>Delete</button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      {runs.length === 0 && <p className="mt-4 text-sm text-[#999]">No runs yet. Run now starts one.</p>}
      {results.length > 0 && (
        <ul className="mt-6 border-t border-line pt-3 text-sm">
          {results.map((row) => (
            <li key={row.id} className="border-b border-line py-2">
              <span className={row.status === "PASSED" ? "text-[#7d9a6a]" : row.status === "FAILED" ? "text-[#d46a6a]" : "text-[#999]"}>{row.status.toLowerCase()}</span>
              <span className="ml-3">{CATALOG_BY_TYPE[row.node_type]?.title || row.node_type}</span>
              <span className="ml-3 text-[#999]">{row.duration_ms}ms</span>
              <span className="mt-1 block text-[#999]">{row.message || row.error_message}</span>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}

function when(value: string) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString([], { month: "short", day: "numeric", hour: "2-digit", minute: "2-digit" });
}
