import { CATALOG, CLAUSE } from "../catalog";
import { StepIcon } from "./StepIcon";

const groups = [
  { id: "given", label: "Given", note: "setup" },
  { id: "when", label: "When", note: "act" },
  { id: "then", label: "Then", note: "check" },
  { id: "flow", label: "And", note: "branch" },
] as const;

export function NodePalette() {
  return (
    <aside className="panel w-56 shrink-0 overflow-auto border-r border-line p-3">
      <p className="mark mb-3 text-[#999]">Steps</p>
      {groups.map((group) => (
        <div key={group.id} className="mb-4">
          <p className="mb-1 text-[11px] text-[#dcddde]">{group.label} <span className="text-[#999]">{group.note}</span></p>
          {CATALOG.filter((step) => (CLAUSE[step.type] || "when") === group.id).map((step) => (
            <button
              key={step.type}
              draggable
              onDragStart={(event) => {
                event.dataTransfer.setData("application/blueprint-step", step.type);
                event.dataTransfer.effectAllowed = "move";
              }}
              className="mb-1 flex w-full items-center gap-2 border border-line bg-transparent px-2 py-1.5 text-left text-sm hover:border-[#7f6df2]"
            >
              <StepIcon type={step.type} />
              {step.title}
            </button>
          ))}
        </div>
      ))}
    </aside>
  );
}
