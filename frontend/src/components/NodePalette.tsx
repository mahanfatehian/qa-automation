import { CATALOG } from "../catalog";

const groups = [
  { id: "flow", label: "Flow" },
  { id: "action", label: "Browser" },
  { id: "search", label: "Find / read" },
  { id: "assert", label: "Check" },
] as const;

export function NodePalette() {
  return (
    <aside className="panel w-56 shrink-0 overflow-auto border-r border-line p-3">
      <p className="mb-3 font-mono text-[10px] uppercase tracking-[0.18em] text-[#8ea6bb]">Blueprint steps</p>
      {groups.map((group) => (
        <div key={group.id} className="mb-4">
          <p className="mb-1 text-[11px] text-[#8ea6bb]">{group.label}</p>
          {CATALOG.filter((step) => step.category === group.id).map((step) => (
            <button
              key={step.type}
              draggable
              onDragStart={(event) => {
                event.dataTransfer.setData("application/blueprint-step", step.type);
                event.dataTransfer.effectAllowed = "move";
              }}
              className="mb-1 w-full rounded border border-line bg-[#101c2b] px-2 py-1.5 text-left text-sm hover:border-[#49c2ff]"
            >
              {step.title}
            </button>
          ))}
        </div>
      ))}
      <p className="font-mono text-[10px] leading-4 text-[#8ea6bb]">White pins order steps. Colored pins pass an element or text.</p>
    </aside>
  );
}
