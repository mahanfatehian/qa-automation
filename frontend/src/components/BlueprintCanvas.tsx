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
import { useCallback, useMemo, useState, type PointerEvent } from "react";
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
  const [box, setBox] = useState<{ x: number; y: number; w: number; h: number } | null>(null);

  const selectWithRightDrag = useCallback((event: PointerEvent<HTMLDivElement>) => {
    if (event.button !== 2) return;
    const target = event.target as HTMLElement;
    if (target.closest(".react-flow__node, .react-flow__handle, .react-flow__controls, .react-flow__minimap")) return;
    event.preventDefault();
    const host = event.currentTarget.getBoundingClientRect();
    const startX = event.clientX;
    const startY = event.clientY;
    const move = (pointer: globalThis.PointerEvent) => {
      const x = Math.min(startX, pointer.clientX) - host.left;
      const y = Math.min(startY, pointer.clientY) - host.top;
      setBox({ x, y, w: Math.abs(pointer.clientX - startX), h: Math.abs(pointer.clientY - startY) });
    };
    const up = (pointer: globalThis.PointerEvent) => {
      window.removeEventListener("pointermove", move);
      window.removeEventListener("pointerup", up);
      setBox(null);
      const x1 = Math.min(startX, pointer.clientX);
      const y1 = Math.min(startY, pointer.clientY);
      const x2 = Math.max(startX, pointer.clientX);
      const y2 = Math.max(startY, pointer.clientY);
      if (x2 - x1 < 4 && y2 - y1 < 4) return;
      const additive = pointer.ctrlKey || pointer.metaKey || pointer.shiftKey;
      setNodes((current) => current.map((node) => {
        const el = document.querySelector(`.react-flow__node[data-id="${node.id}"]`);
        if (!el) return additive ? node : { ...node, selected: false };
        const rect = el.getBoundingClientRect();
        const hit = rect.left < x2 && rect.right > x1 && rect.top < y2 && rect.bottom > y1;
        return { ...node, selected: hit || (additive && Boolean(node.selected)) };
      }));
    };
    window.addEventListener("pointermove", move);
    window.addEventListener("pointerup", up);
  }, [setNodes]);
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
    <div className="relative min-w-0 flex-1" data-testid="blueprint-canvas" onDragOver={(event) => event.preventDefault()} onDrop={onDrop} onPointerDown={selectWithRightDrag} onContextMenu={(event) => event.preventDefault()}>
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
        selectionOnDrag={false}
        panOnDrag={[1]}
        zoomOnScroll
        panOnScroll={false}
        multiSelectionKeyCode={["Meta", "Control"]}
        selectNodesOnDrag={false}
        proOptions={{ hideAttribution: true }}
      >
        <Background variant={BackgroundVariant.Lines} gap={28} size={1} color="rgba(244,248,252,0.08)" />
        <Controls />
        <MiniMap pannable zoomable nodeColor={miniColor} />
      </ReactFlow>
      <p className="canvas-hint">Right-drag selects · scroll-wheel button pans · wheel zooms · Ctrl+A selects all</p>
      {box && <div className="pointer-events-none absolute z-10 border border-[#f4f8fc] bg-[rgba(244,248,252,0.12)]" style={{ left: box.x, top: box.y, width: box.w, height: box.h }} />}
    </div>
  );
}
