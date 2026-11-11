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
  exec: "#dcddde",
  element: "#7d9a6a",
  text: "#7f6df2",
  bool: "#d4a017",
};

type Props = {
  nodes: Node<BlueprintData>[];
  edges: Edge[];
  onNodesChange: OnNodesChange<Node<BlueprintData>>;
  onEdgesChange: OnEdgesChange<Edge>;
  setNodes: React.Dispatch<React.SetStateAction<Node<BlueprintData>[]>>;
  setEdges: React.Dispatch<React.SetStateAction<Edge[]>>;
  onSelect: (id: string | null) => void;
  onGestureStart: () => void;
};

export function BlueprintCanvas({ nodes, edges, onNodesChange, onEdgesChange, setNodes, setEdges, onSelect, onGestureStart }: Props) {
  const { screenToFlowPosition, fitView } = useReactFlow();
  const [box, setBox] = useState<{ x: number; y: number; w: number; h: number } | null>(null);
  const [menu, setMenu] = useState<{ x: number; y: number; nodeId: string | null } | null>(null);

  const selectWithLeftDrag = useCallback((event: PointerEvent<HTMLDivElement>) => {
    if (event.button !== 0) return;
    const target = event.target as HTMLElement;
    if (target.closest(".react-flow__node, .react-flow__handle, .react-flow__controls, .react-flow__minimap, .react-flow__panel")) return;
    event.preventDefault();
    event.stopPropagation();
    const host = event.currentTarget;
    const bounds = host.getBoundingClientRect();
    if (host.setPointerCapture) host.setPointerCapture(event.pointerId);
    const startX = event.clientX;
    const startY = event.clientY;
    const move = (pointer: globalThis.PointerEvent) => {
      const x = Math.min(startX, pointer.clientX) - bounds.left;
      const y = Math.min(startY, pointer.clientY) - bounds.top;
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
    <div
      className="relative min-w-0 flex-1"
      data-testid="blueprint-canvas"
      onDragOver={(event) => event.preventDefault()}
      onDrop={onDrop}
      onPointerDownCapture={selectWithLeftDrag}
      onContextMenu={(event) => {
        event.preventDefault();
        const node = (event.target as HTMLElement).closest(".react-flow__node");
        const host = event.currentTarget.getBoundingClientRect();
        const nodeId = node?.getAttribute("data-id") || null;
        if (nodeId) onSelect(nodeId);
        setMenu({ x: event.clientX - host.left, y: event.clientY - host.top, nodeId });
      }}
    >
      <ReactFlow
        nodes={nodes}
        edges={edges}
        onNodesChange={onNodesChange}
        onEdgesChange={onEdgesChange}
        onConnect={onConnect}
        isValidConnection={isValidConnection}
        nodeTypes={nodeTypes}
        onNodeDragStart={onGestureStart}
        onSelectionChange={({ nodes: selected }) => onSelect(selected[0]?.id ?? null)}
        onInit={(instance) => instance.fitView({ padding: 0.2 })}
        deleteKeyCode={["Backspace", "Delete"]}
        selectionOnDrag={false}
        panOnDrag={[1]}
        zoomOnScroll
        panOnScroll={false}
        multiSelectionKeyCode={["Meta", "Control"]}
        selectNodesOnDrag={false}
        proOptions={{ hideAttribution: true }}
      >
        <Background variant={BackgroundVariant.Dots} gap={22} size={1} color="#333" />
        <Controls />
        <MiniMap pannable zoomable nodeColor={miniColor} />
      </ReactFlow>
      {menu && (
        <div className="absolute z-20 min-w-40 border border-[#3f3f3f] bg-[#1e1e1e] py-1 text-sm shadow-lg" style={{ left: menu.x, top: menu.y }} onMouseLeave={() => setMenu(null)}>
          {menu.nodeId ? (
            <>
              <button className="block w-full px-3 py-1 text-left hover:bg-[#2a2a2a]" onClick={() => { onSelect(menu.nodeId); setMenu(null); }}>Properties</button>
              <button className="block w-full px-3 py-1 text-left hover:bg-[#2a2a2a]" onClick={() => {
                const source = nodes.find((node) => node.id === menu.nodeId);
                if (!source) return;
                const id = `n_${crypto.randomUUID().slice(0, 8)}`;
                setNodes((current) => current.concat({ ...source, id, selected: true, position: { x: source.position.x + 40, y: source.position.y + 40 } }));
                setMenu(null);
              }}>Duplicate</button>
              <button className="block w-full px-3 py-1 text-left text-[#d46a6a] hover:bg-[#2a2a2a]" onClick={() => {
                setNodes((current) => current.filter((node) => node.id !== menu.nodeId));
                setEdges((current) => current.filter((edge) => edge.source !== menu.nodeId && edge.target !== menu.nodeId));
                setMenu(null);
              }}>Delete</button>
            </>
          ) : (
            <>
              <button className="block w-full px-3 py-1 text-left hover:bg-[#2a2a2a]" onClick={() => { setNodes((current) => current.map((node) => ({ ...node, selected: true }))); setMenu(null); }}>Select all</button>
              <button className="block w-full px-3 py-1 text-left hover:bg-[#2a2a2a]" onClick={() => { fitView({ padding: 0.2 }); setMenu(null); }}>Fit view</button>
            </>
          )}
        </div>
      )}
      {box && <div className="pointer-events-none absolute z-10 border border-[#7f6df2] bg-[rgba(127,109,242,0.12)]" style={{ left: box.x, top: box.y, width: box.w, height: box.h }} />}
    </div>
  );
}
