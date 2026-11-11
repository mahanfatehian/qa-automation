import { Handle, Position, type Node, type NodeProps } from "@xyflow/react";
import { CATALOG_BY_TYPE } from "../../catalog";
import type { BlueprintData, PinKind } from "../../types/blueprint";

export type FlowNode = Node<BlueprintData, string>;

const headerColor: Record<string, string> = {
  flow: "#ff5d73",
  action: "#49c2ff",
  search: "#3ddc97",
  assert: "#ffb020",
};

const pinColor: Record<PinKind, string> = {
  exec: "#f4f7fb",
  element: "#3ddc97",
  text: "#49c2ff",
  bool: "#ffb020",
};

export function BlueprintNode({ data, selected }: NodeProps<FlowNode>) {
  const def = CATALOG_BY_TYPE[String(data.nodeType || "")];
  const inputs = def?.pins.filter((pin) => pin.direction === "in") ?? [];
  const outputs = def?.pins.filter((pin) => pin.direction === "out") ?? [];
  const rows = Math.max(inputs.length, outputs.length, 1);
  const status = String(data.runStatus || "idle");
  return (
    <div className={`bp-node status-${status} ${selected ? "is-selected" : ""}`} style={{ minHeight: 34 + rows * 22 }}>
      <div className="bp-head" style={{ background: headerColor[def?.category || "action"] }}>
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
