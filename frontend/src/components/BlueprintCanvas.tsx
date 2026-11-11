import {
  Background,
  BackgroundVariant,
  Controls,
  MiniMap,
  ReactFlow,
  addEdge,
  useReactFlow,
  type Connection,
  type Edge,
  type Node,
  type OnConnect,
  type OnEdgesChange,
  type OnNodesChange,
} from "@xyflow/react";
import { useCallback, useMemo } from "react";
import { CATALOG, CATALOG_BY_TYPE, pinKind } from "../catalog";
import type { BlueprintData } from "../types/blueprint";
import { BlueprintNode } from "./nodes/BlueprintNode";

const nodeTypes = Object.fromEntries(CATALOG.map((step) => [step.type, BlueprintNode]));

const stroke: Record<string, string> = {
  exec: "#f4efe6",
  element: "#c9d7b0",
  text: "#8eb8c9",
  bool: "#e0b15a",
};

type Props = {
  nodes: Node<BlueprintData>[];
  edges: Edge[];
  onNodesChange: OnNodesChange<Node<BlueprintData>>;
  onEdgesChange: OnEdgesChange<Edge>;
  setNodes: React.Dispatch<React.SetStateAction<Node<BlueprintData>[]>>;
  setEdges: React.Dispatch<React.SetStateAction<Edge[]>>;
  onSelect: (id: string | null) => void;
};

export function BlueprintCanvas({ nodes, edges, onNodesChange, onEdgesChange, setNodes, setEdges, onSelect }: Props) {
  const { screenToFlowPosition } = useReactFlow();
  const onConnect: OnConnect = useCallback((connection: Connection) => {
    const kind = pinKind(connection.sourceHandle);
    setEdges((current) => addEdge({
      ...connection,
      style: {
        stroke: stroke[kind] || "#f4f7fb",
        strokeWidth: 1.6,
        strokeDasharray: kind === "exec" ? undefined : "4 3",
      },
    }, current));
  }, [setEdges]);

  const isValidConnection = useCallback((connection: Connection | Edge) => {
    if (!connection.sourceHandle || !connection.targetHandle) return false;
    const sourceExec = connection.sourceHandle.startsWith("exec_");
    const targetExec = connection.targetHandle.startsWith("exec_");
    if (sourceExec !== targetExec) return false;
    if (sourceExec) return true;
    return pinKind(connection.sourceHandle) === pinKind(connection.targetHandle);
  }, []);

  const onDrop = useCallback((event: React.DragEvent) => {
    event.preventDefault();
    const type = event.dataTransfer.getData("application/blueprint-step");
    const def = CATALOG_BY_TYPE[type];
    if (!def) return;
    const position = screenToFlowPosition({ x: event.clientX, y: event.clientY });
    const id = `n_${crypto.randomUUID().slice(0, 8)}`;
    setNodes((current) => current.concat({
      id,
      type,
      position,
      data: { nodeType: type, title: def.title, ...def.defaults },
    }));
  }, [screenToFlowPosition, setNodes]);

  const miniColor = useMemo(() => (node: Node<BlueprintData>) => {
    const category = CATALOG_BY_TYPE[String(node.data?.nodeType)]?.category;
    if (category === "flow") return "#ff5d73";
    if (category === "search") return "#3ddc97";
    if (category === "assert") return "#ffb020";
    return "#49c2ff";
  }, []);

  return (
    <div className="relative min-w-0 flex-1" data-testid="blueprint-canvas" onDragOver={(event) => event.preventDefault()} onDrop={onDrop}>
      <ReactFlow
        nodes={nodes}
        edges={edges}
        onNodesChange={onNodesChange}
        onEdgesChange={onEdgesChange}
        onConnect={onConnect}
        isValidConnection={isValidConnection}
        nodeTypes={nodeTypes}
        onSelectionChange={({ nodes: selected }) => onSelect(selected[0]?.id ?? null)}
        fitView
        deleteKeyCode={["Backspace", "Delete"]}
        selectionOnDrag
        selectionKeyCode="Shift"
        multiSelectionKeyCode={["Meta", "Control"]}
        panOnScroll
        selectNodesOnDrag={false}
        proOptions={{ hideAttribution: true }}
      >
        <Background variant={BackgroundVariant.Dots} gap={22} size={1.2} color="#1e293b" />
        <Controls />
        <MiniMap pannable zoomable nodeColor={miniColor} />
      </ReactFlow>
      <p className="canvas-hint">Shift-drag selects · Ctrl+A selects all · drag a selected step to move the group</p>
    </div>
  );
}
