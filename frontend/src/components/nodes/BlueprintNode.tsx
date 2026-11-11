import { Handle, Position, type Node, type NodeProps } from "@xyflow/react";
import { CATALOG_BY_TYPE, CLAUSE, CLAUSE_COLOR } from "../../catalog";
import { StepIcon } from "../StepIcon";
import type { BlueprintData, PinKind } from "../../types/blueprint";

export type FlowNode = Node<BlueprintData, string>;

const clauseLabel: Record<string, string> = {
  given: "Given",
  when: "When",
  then: "Then",
  flow: "And",
};

const pinColor: Record<PinKind, string> = {
  exec: "#dcddde",
  element: "#7d9a6a",
  text: "#7f6df2",
  bool: "#d4a017",
};

export function BlueprintNode({ data, selected }: NodeProps<FlowNode>) {
  const def = CATALOG_BY_TYPE[String(data.nodeType || "")];
  const inputs = def?.pins.filter((pin) => pin.direction === "in") ?? [];
  const outputs = def?.pins.filter((pin) => pin.direction === "out") ?? [];
  const rows = Math.max(inputs.length, outputs.length, 1);
  const status = String(data.runStatus || "idle");
  const clause = CLAUSE[String(data.nodeType || "")] || "when";
  return (
    <div className={`bp-node status-${status} ${selected ? "is-selected" : ""}`} style={{ minHeight: 34 + rows * 22 }}>
      <div className="bp-head" style={{ borderLeft: `3px solid ${CLAUSE_COLOR[clause]}` }}>
        <span className="mr-1 inline-flex text-[#dcddde]"><StepIcon type={String(data.nodeType || "")} /></span>
        <span className="mr-1 font-mono text-[9px] uppercase opacity-70">{clauseLabel[clause]}</span>
        {String(data.title || def?.title || data.nodeType)}
      </div>
      {inputs.map((pin, index) => (
        <Handle
          key={pin.id}
          id={pin.id}
          type="target"
          position={Position.Left}
          className={pin.kind === "exec" ? "handle-exec" : "handle-data"}
          style={{ top: 40 + index * 22, background: pinColor[pin.kind] }}
        />
      ))}
      {outputs.map((pin, index) => (
        <Handle
          key={pin.id}
          id={pin.id}
          type="source"
          position={Position.Right}
          className={pin.kind === "exec" ? "handle-exec" : "handle-data"}
          title={pin.kind === "exec" ? "Each wire runs on its own browser" : pin.label}
          style={{ top: 40 + index * 22, background: pinColor[pin.kind] }}
        />
      ))}
      <div className="bp-pins">
        <div>
          {inputs.map((pin) => (
            <div key={pin.id} className="pin-label">{pin.label}</div>
          ))}
        </div>
        <div className="text-right">
          {outputs.map((pin) => (
            <div key={pin.id} className="pin-label">{pin.label}</div>
          ))}
        </div>
      </div>
    </div>
  );
}
