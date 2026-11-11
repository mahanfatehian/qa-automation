import type { Workflow } from "../types/blueprint";

type Props = {
  workflows: Workflow[];
  activeId: string | null;
  onOpen: (workflow: Workflow) => void;
  onCreate: () => void;
  onDelete: (workflow: Workflow) => void;
  onRun: (workflow: Workflow) => void;
  onRename: (workflow: Workflow, name: string) => void;
};

export function WorkflowsDesk({ workflows, activeId, onOpen, onCreate, onDelete, onRun, onRename }: Props) {
  return (
    <section className="panel min-h-0 flex-1 overflow-auto p-4" data-testid="workflows-desk">
      <div className="mb-4 flex items-center justify-between">
        <div>
          <h2 className="text-lg">Blueprints</h2>
          <p className="text-sm text-[#999]">Every saved test. Open one to edit it, or start a new one.</p>
        </div>
        <button className="btn btn-run" onClick={onCreate}>New blueprint</button>
      </div>
      <table className="w-full text-left text-sm">
        <thead className="text-[#999]">
          <tr>
            <th className="py-2">Name</th>
            <th>Steps</th>
            <th>Updated</th>
            <th />
          </tr>
        </thead>
        <tbody>
          {workflows.map((workflow) => (
            <tr key={workflow.id} className="border-t border-line">
              <td className="py-2">
                <input
                  className="btn w-56"
                  defaultValue={workflow.name}
                  key={`${workflow.id}-${workflow.name}`}
                  onBlur={(event) => {
                    const next = event.target.value.trim();
                    if (next && next !== workflow.name) onRename(workflow, next);
                  }}
                  onKeyDown={(event) => {
                    if (event.key === "Enter") event.currentTarget.blur();
                  }}
                />
                {workflow.id === activeId && <span className="ml-2 text-[10px] uppercase text-[#7f6df2]">open</span>}
              </td>
              <td className="text-[#999]">{workflow.graph?.nodes?.length || 0}</td>
              <td className="text-[#999]">{when(workflow.updated_at)}</td>
              <td className="flex justify-end gap-2 py-2">
                <button className="btn" onClick={() => onOpen(workflow)}>Open</button>
                <button className="btn" onClick={() => onRun(workflow)}>Run</button>
                <button className="btn btn-danger" onClick={() => onDelete(workflow)}>Delete</button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      {workflows.length === 0 && <p className="mt-4 text-sm text-[#999]">No blueprints yet.</p>}
    </section>
  );
}

function when(value: string) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString([], { month: "short", day: "numeric", hour: "2-digit", minute: "2-digit" });
}
