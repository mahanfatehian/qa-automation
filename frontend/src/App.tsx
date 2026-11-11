import { useCallback, useEffect, useRef, useState } from "react";
import { useEdgesState, useNodesState, type Edge, type Node } from "@xyflow/react";
import { AutomationDesk } from "./components/AutomationDesk";
import { BlueprintCanvas } from "./components/BlueprintCanvas";
import { RunsDesk } from "./components/RunsDesk";
import { SettingsDesk } from "./components/SettingsDesk";
import { TestDataDesk, type DataColumn } from "./components/TestDataDesk";
import { WorkflowsDesk } from "./components/WorkflowsDesk";
import { LiveRunOverlay } from "./components/LiveRunOverlay";
import { NodeInspector } from "./components/NodeInspector";
import { NodePalette } from "./components/NodePalette";
import { InspectModal } from "./components/InspectModal";
import { ExamplesModal } from "./components/ExamplesModal";
import { ScheduleModal } from "./components/ScheduleModal";
import { starterNodes } from "./demo";
import { api, type PickResult } from "./services/api";
import { openRunSocket } from "./services/socket";
import type { BlueprintData, NodeResult, TelemetryEvent, TestRun, Workflow } from "./types/blueprint";

const DESK_PATH: Record<string, string> = {
  library: "/library",
  canvas: "/canvas",
  runs: "/runs",
  automation: "/automation",
  data: "/inputs",
  settings: "/settings",
};

function deskFromPath(path: string): "library" | "canvas" | "runs" | "automation" | "data" | "settings" {
  const name = path.replace(/^\/+/, "").split("/")[0];
  if (name === "inputs" || name === "data") return "data";
  if (name === "canvas" || name === "runs" || name === "automation" || name === "settings" || name === "library") return name;
  return "library";
}

function writeDesk(next: string, blueprintId: string | null, replace = false) {
  const path = DESK_PATH[next] || "/library";
  const url = blueprintId ? `${path}?blueprint=${blueprintId}` : path;
  if (`${window.location.pathname}${window.location.search}` === url) return;
  if (replace) window.history.replaceState({ desk: next }, "", url);
  else window.history.pushState({ desk: next }, "", url);
}

export function App() {
  const [workflows, setWorkflows] = useState<Workflow[]>([]);
  const [activeId, setActiveId] = useState<string | null>(null);
  const [name, setName] = useState("Untitled test");
  const [nodes, setNodes, onNodesChange] = useNodesState<Node<BlueprintData>>([]);
  const [edges, setEdges, onEdgesChange] = useEdgesState<Edge>([]);
  const past = useRef<Array<{ nodes: Node<BlueprintData>[]; edges: Edge[] }>>([]);
  const future = useRef<Array<{ nodes: Node<BlueprintData>[]; edges: Edge[] }>>([]);
  const graphRef = useRef({ nodes, edges });
  graphRef.current = { nodes, edges };

  function remember() {
    past.current.push(JSON.parse(JSON.stringify(graphRef.current)));
    if (past.current.length > 100) past.current.shift();
    future.current = [];
  }

  function restore(next: { nodes: Node<BlueprintData>[]; edges: Edge[] }) {
    setNodes(next.nodes);
    setEdges(next.edges);
    setDirty(true);
  }

  function undo() {
    const prev = past.current.pop();
    if (!prev) return;
    future.current.push(JSON.parse(JSON.stringify(graphRef.current)));
    restore(prev);
  }

  function redo() {
    const next = future.current.pop();
    if (!next) return;
    past.current.push(JSON.parse(JSON.stringify(graphRef.current)));
    restore(next);
  }
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [dirty, setDirty] = useState(false);
  const [grid, setGrid] = useState("checking");
  const [run, setRun] = useState<TestRun | null>(null);
  const [events, setEvents] = useState<TelemetryEvent[]>([]);
  const [shot, setShot] = useState("");
  const [error, setError] = useState("");
  const [scheduleOpen, setScheduleOpen] = useState(false);
  const [examplesOpen, setExamplesOpen] = useState(false);
  const [examples, setExamples] = useState<Array<Record<string, string>>>([]);
  const [dataColumns, setDataColumns] = useState<DataColumn[]>([]);
  const [probing, setProbing] = useState(false);
  const [probeMessage, setProbeMessage] = useState("");
  const [runs, setRuns] = useState<TestRun[]>([]);
  const [pickOpen, setPickOpen] = useState(false);
  const [pickBusy, setPickBusy] = useState(false);
  const [pickResult, setPickResult] = useState<PickResult | null>(null);
  const [pickError, setPickError] = useState("");
  const [pickNodeId, setPickNodeId] = useState<string | null>(null);
  const [well, setWell] = useState(220);
  const [desk, setDesk] = useState<"library" | "canvas" | "runs" | "automation" | "data" | "settings">(() => deskFromPath(window.location.pathname));
  const [deskResults, setDeskResults] = useState<NodeResult[]>([]);

  const active = workflows.find((item) => item.id === activeId) || null;
  const selected = nodes.find((node) => node.id === selectedId) || null;

  const loadList = useCallback(async () => {
    const rows = await api.workflows();
    setWorkflows(rows);
    return rows;
  }, []);

  function applyGraph(workflow: Workflow) {
    const graph = workflow.graph || { nodes: [], edges: [] };
    past.current = [];
    future.current = [];
    setName(workflow.name);
    setNodes(((graph.nodes || []) as Node<BlueprintData>[]).map((node, index) => ({
      ...node,
      position: node.position ?? { x: 80 + index * 40, y: 120 },
    })));
    setEdges((graph.edges || []) as Edge[]);
    setExamples(Array.isArray(graph.examples) ? graph.examples : []);
    setDataColumns(Array.isArray(graph.dataColumns) ? graph.dataColumns.map((column) => ({ name: column.name, type: column.type, options: column.options || [] })) : []);
    setSelectedId(null);
    setDirty(false);
    setRun(null);
    setEvents([]);
    setShot("");
    api.runs(workflow.id).then(setRuns).catch(() => setRuns([]));
  }

  useEffect(() => {
    const wanted = new URLSearchParams(window.location.search).get("blueprint");
    const initialDesk = deskFromPath(window.location.pathname);
    loadList().then((rows) => {
      const match = rows.find((row) => row.id === wanted) || rows[0];
      if (match) {
        setActiveId(match.id);
        applyGraph(match);
        writeDesk(initialDesk, match.id, true);
      }
    }).catch((err: Error) => setError(err.message));
    const timer = window.setInterval(() => {
      api.status().then((status) => {
        setGrid(status.grid.ready ? `${status.grid.node_count} browsers` : "grid down");
      }).catch(() => setGrid("api down"));
    }, 8000);
    const onPop = () => setDesk(deskFromPath(window.location.pathname));
    window.addEventListener("popstate", onPop);
    return () => {
      window.clearInterval(timer);
      window.removeEventListener("popstate", onPop);
    };
  }, [loadList]);

  useEffect(() => {
    if (!activeId) return;
    const tick = () => {
      api.runs(activeId).then((rows) => {
        setRuns(rows);
        const live = rows.find((row) => row.status === "QUEUED" || row.status === "RUNNING");
        if (live) setRun((current) => current && current.id === live.id ? { ...current, status: live.status, error_message: live.error_message } : current);
      }).catch(() => undefined);
    };
    const timer = window.setInterval(tick, 4000);
    return () => window.clearInterval(timer);
  }, [activeId]);

  function go(next: "library" | "canvas" | "runs" | "automation" | "data" | "settings") {
    setDesk(next);
    writeDesk(next, activeId);
    if (next === "library") loadList().catch((err: Error) => setError(err.message));
    if ((next === "runs" || next === "automation") && activeId) api.runs(activeId).then(setRuns).catch(() => undefined);
  }

  function graphBody() {
    return {
      name,
      graph: {
        maxSteps: 500,
        nodes: nodes.map((node) => ({
          ...node,
          data: { ...node.data, runStatus: undefined },
        })),
        edges,
        examples,
        dataColumns,
      },
    };
  }

  async function save() {
    if (!activeId) return;
    const saved = await api.saveWorkflow(activeId, graphBody());
    setWorkflows((current) => current.map((item) => item.id === saved.id ? saved : item));
    setDirty(false);
  }

  async function runBlueprint() {
    setError("");
    if (!activeId) return;
    if (dirty) await save();
    const started = await api.run(activeId);
    setRun(started);
    setRuns((current) => [started, ...current.filter((item) => item.id !== started.id)]);
    setEvents([]);
    setShot("");
    setNodes((current) => current.map((node) => ({ ...node, data: { ...node.data, runStatus: "idle" } })));
    const socket = openRunSocket(started.id, (event) => {
      setEvents((current) => current.concat(event));
      if (event.screenshot_base64) setShot(event.screenshot_base64);
      if (event.node_id && event.event_type?.startsWith("NODE_")) {
        const status = event.event_type.replace("NODE_", "").toLowerCase();
        setNodes((current) => current.map((node) => node.id === event.node_id ? { ...node, data: { ...node.data, runStatus: status } } : node));
      }
      if (event.event_type === "RUN_COMPLETED" || event.event_type === "RUN_FAILED" || event.event_type === "RUN_STATUS") {
        const status = event.status || (event.event_type === "RUN_COMPLETED" ? "PASSED" : event.event_type === "RUN_FAILED" ? "FAILED" : started.status);
        setRun((current) => current ? { ...current, status, error_message: event.message || current.error_message } : current);
        setRuns((current) => current.map((item) => item.id === started.id ? { ...item, status, error_message: event.message || item.error_message } : item));
      }
    });
    window.setTimeout(() => socket.close(), 15 * 60 * 1000);
  }

  async function createTest() {
    const created = await api.createWorkflow({ name: "Untitled test", graph: { nodes: starterNodes() as unknown as Workflow["graph"]["nodes"], edges: [], maxSteps: 500 } });
    setWorkflows((current) => [created, ...current]);
    setActiveId(created.id);
    applyGraph(created);
  }

  function confirmDirty() {
    if (!dirty) return true;
    return window.confirm("This blueprint has unsaved edits. Continue and drop them?");
  }

  async function openPast(item: TestRun) {
    setRun(item);
    const results = await api.results(item.id);
    setDeskResults(results);
    setEvents(results.map((row) => ({
      event_type: `NODE_${row.status}`,
      run_id: item.id,
      node_id: row.node_id,
      node_type: row.node_type,
      duration_ms: row.duration_ms,
      message: row.message,
      error_stack: row.error_message,
      output_data: row.output_data,
      result_id: row.id,
    })));
    const withShot = [...results].reverse().find((row) => row.has_screenshot);
    if (withShot) {
      const response = await fetch(`/api/v1/runs/${item.id}/results/${withShot.id}/screenshot`);
      if (response.ok) {
        const blob = await response.blob();
        const reader = new FileReader();
        reader.onload = () => setShot(String(reader.result).split(",")[1] || "");
        reader.readAsDataURL(blob);
      }
    }
  }

  useEffect(() => {
    function onKey(event: KeyboardEvent) {
      if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === "s") {
        event.preventDefault();
        save().catch((err: Error) => setError(err.message));
      }
      if ((event.ctrlKey || event.metaKey) && event.key === "Enter") {
        event.preventDefault();
        runBlueprint().catch((err: Error) => setError(err.message));
      }
      const typing = (event.target as HTMLElement | null)?.closest("input, textarea, select, [contenteditable='true']");
      if ((event.ctrlKey || event.metaKey) && !typing && event.key.toLowerCase() === "z") {
        event.preventDefault();
        if (event.shiftKey) redo();
        else undo();
        return;
      }
      if ((event.ctrlKey || event.metaKey) && !typing && event.key.toLowerCase() === "y") {
        event.preventDefault();
        redo();
        return;
      }
      if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === "a") {
        const target = event.target as HTMLElement | null;
        if (target?.closest("input, textarea, select, [contenteditable='true']")) return;
        event.preventDefault();
        setNodes((current) => current.map((node) => ({ ...node, selected: true })));
      }
    }
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  });

  return (
    <div className="flex h-screen flex-col">
      <header className="flex h-14 shrink-0 items-center gap-3 border-b border-line bg-[#161616] px-3" data-testid="studio-header">
        <div className="flex items-center gap-2">
          <img src="/favicon.svg" alt="" width={28} height={28} />
          <div>
            <p className="font-mono text-[9px] uppercase tracking-[0.16em] text-[#999]">BQA-01</p>
            <p className="text-sm font-semibold leading-4">BlueprintQA</p>
          </div>
        </div>
        <select className="btn max-w-56" value={activeId || ""} onChange={(event) => {
          const next = workflows.find((item) => item.id === event.target.value);
          if (!next) return;
          setActiveId(next.id);
          applyGraph(next);
          writeDesk(desk, next.id, true);
        }}>
          {workflows.map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}
        </select>
        <input className="btn w-48" value={name} onChange={(event) => { setName(event.target.value); setDirty(true); }} />
        <button className="btn" onClick={() => { if (!confirmDirty()) return; createTest().catch((err: Error) => setError(err.message)); }}>New</button>

        <button className="btn btn-danger" onClick={() => {
          if (!activeId || !window.confirm("Delete this blueprint and its runs?")) return;
          api.removeWorkflow(activeId).then(() => loadList()).then((rows) => {
            if (rows && rows[0]) { setActiveId(rows[0].id); applyGraph(rows[0]); }
            else { setActiveId(null); setNodes([]); setEdges([]); }
          }).catch((err: Error) => setError(err.message));
        }}>Delete</button>
        <button className="btn" onClick={() => save().catch((err: Error) => setError(err.message))}>{dirty ? "Save" : "Saved"}</button>
        <button className="btn btn-run" onClick={() => runBlueprint().catch((err: Error) => setError(err.message))}>Run blueprint</button>
        <button className={`btn ${desk === "data" ? "btn-run" : ""}`} onClick={() => go("data")}>Inputs</button>
        <button className={`btn ${desk === "library" ? "btn-run" : ""}`} onClick={() => go("library")}>Blueprints</button>
        <button className={`btn ${desk === "canvas" ? "btn-run" : ""}`} onClick={() => go("canvas")}>Canvas</button>
        <button className={`btn ${desk === "runs" ? "btn-run" : ""}`} onClick={() => go("runs")}>Runs</button>
        <button className={`btn ${desk === "automation" ? "btn-run" : ""}`} onClick={() => go("automation")}>Automation</button>
        <button className={`btn ${desk === "settings" ? "btn-run" : ""}`} onClick={() => go("settings")}>Settings</button>
        <a className="btn" href="http://localhost:7900/?autoconnect=1&resize=scale" target="_blank" rel="noreferrer">Watch browser</a>
        <span className="ml-auto font-mono text-[11px] text-[#999]">{grid}</span>
        {run && <a className="btn" href={`/api/v1/runs/${run.id}/junit`}>JUnit</a>}
      </header>
      {error && <p className="bg-[#3a241c] px-3 py-1 text-sm text-[#e7a090]">{error}</p>}
      <div className="flex min-h-0 flex-1 flex-col">
      <div className="flex min-h-[280px] min-w-0 flex-1 overflow-hidden">
        {desk === "library" && <WorkflowsDesk workflows={workflows} activeId={activeId} onCreate={() => { if (!confirmDirty()) return; createTest().then(() => setDesk("canvas")).catch((err: Error) => setError(err.message)); }} onOpen={(workflow) => { if (!confirmDirty()) return; setActiveId(workflow.id); applyGraph(workflow); setDesk("canvas"); }} onRun={(workflow) => { setActiveId(workflow.id); applyGraph(workflow); setDesk("canvas"); window.setTimeout(() => runBlueprint().catch((err: Error) => setError(err.message)), 0); }} onDelete={(workflow) => { if (!window.confirm(`Delete ${workflow.name}?`)) return; api.removeWorkflow(workflow.id).then(() => loadList()).then((rows) => { if (workflow.id === activeId) { if (rows && rows[0]) { setActiveId(rows[0].id); applyGraph(rows[0]); } else { setActiveId(null); setNodes([]); setEdges([]); } } }).catch((err: Error) => setError(err.message)); }} onRename={(workflow, next) => { api.saveWorkflow(workflow.id, { name: next }).then((saved) => { setWorkflows((current) => current.map((item) => item.id === saved.id ? saved : item)); if (saved.id === activeId) setName(saved.name); }).catch((err: Error) => setError(err.message)); }} />}
        {desk === "runs" && <RunsDesk runs={runs} results={deskResults} activeRunId={run?.id} onRun={() => runBlueprint().catch((err: Error) => setError(err.message))} onOpen={(item) => openPast(item).catch((err: Error) => setError(err.message))} onDelete={(item) => api.removeRun(item.id).then(() => { setRuns((current) => current.filter((run) => run.id !== item.id)); setDeskResults([]); }).catch((err: Error) => setError(err.message))} />}
        {desk === "automation" && <AutomationDesk workflowId={activeId} workflowName={name} webhookToken={active?.webhook_token || ""} runs={runs} activeRunId={run?.id} onOpen={(item) => openPast(item as TestRun).catch((err: Error) => setError(err.message))} />}
        {desk === "data" && <TestDataDesk columns={dataColumns} rows={examples} onChange={(columns, rows) => { setDataColumns(columns); setExamples(rows); setDirty(true); }} />}
        {desk === "settings" && <SettingsDesk />}
        {desk === "canvas" && <NodePalette />}
        {desk === "canvas" && <BlueprintCanvas
          nodes={nodes}
          edges={edges}
          onNodesChange={(changes) => {
            if (changes.some((change) => change.type === "remove" || change.type === "add" || change.type === "replace")) remember();
            if (changes.some((change) => change.type !== "select" && change.type !== "dimensions")) setDirty(true);
            onNodesChange(changes);
          }}
          onEdgesChange={(changes) => {
            if (changes.some((change) => change.type !== "select")) remember();
            setDirty(true);
            onEdgesChange(changes);
          }}
          onGestureStart={remember}
          setNodes={(value) => { remember(); setDirty(true); setNodes(value); }}
          setEdges={(value) => { remember(); setDirty(true); setEdges(value); }}
          onSelect={setSelectedId}
        />}
        {desk === "canvas" && <NodeInspector
          node={selected}
          dataColumns={dataColumns}
          rows={examples}
          probing={probing}
          probeMessage={probeMessage}
          onPick={(node) => {
            const opener = nodes.find((item) => item.data.nodeType === "NAVIGATE");
            if (!String(opener?.data.url || "")) {
              setPickError("Add an Open page step with a URL first.");
              setPickOpen(true);
              return;
            }
            setPickNodeId(node.id);
            setPickError("");
            setPickOpen(true);
          }}
          onChange={(id, patch) => {
            remember();
            setDirty(true);
            setNodes((current) => current.map((node) => node.id === id ? { ...node, data: { ...node.data, ...patch } } : node));
          }}
          onProbe={async (node) => {
            const opener = nodes.find((item) => item.data.nodeType === "NAVIGATE");
            const url = String(opener?.data.url || "");
            if (!url) {
              setProbeMessage("Add an Open page step with a URL first.");
              return;
            }
            setProbing(true);
            try {
              const result = await api.probe({ url, strategy: String(node.data.strategy || "XPATH"), query: String(node.data.query || "") });
              setProbeMessage(result.ok ? `Matched ${result.count}. ${result.samples[0] || ""}` : result.error || "No match");
              if (result.screenshot_base64) setShot(result.screenshot_base64);
            } catch (err) {
              setProbeMessage(err instanceof Error ? err.message : "Probe failed");
            } finally {
              setProbing(false);
            }
          }}
        />}
      </div>
      <div className="well-rail" data-testid="well-rail" onPointerDown={(event) => {
        event.preventDefault();
        const startY = event.clientY;
        const start = well;
        const move = (pointer: PointerEvent) => setWell(Math.min(Math.round(window.innerHeight * 0.38), Math.max(160, start - (pointer.clientY - startY))));
        const up = () => {
          window.removeEventListener("pointermove", move);
          window.removeEventListener("pointerup", up);
        };
        window.addEventListener("pointermove", move);
        window.addEventListener("pointerup", up);
      }} />
      <LiveRunOverlay height={well} run={run} events={events} shot={shot} runs={runs} onPick={(event) => {
        if (event.screenshot_base64) {
          setShot(event.screenshot_base64);
          return;
        }
        if (!run || !event.result_id) return;
        fetch(`/api/v1/runs/${run.id}/results/${event.result_id}/screenshot`).then(async (response) => {
          if (!response.ok) return;
          const blob = await response.blob();
          const reader = new FileReader();
          reader.onload = () => setShot(String(reader.result).split(",")[1] || "");
          reader.readAsDataURL(blob);
        }).catch(() => undefined);
      }} onOpenRun={(item) => openPast(item).catch((err: Error) => setError(err.message))} />
      </div>
      <ScheduleModal open={scheduleOpen} workflowId={activeId} webhookToken={active?.webhook_token || ""} onClose={() => setScheduleOpen(false)} />

      <InspectModal
        open={pickOpen}
        nodes={nodes}
        edges={edges}
        stopBefore={pickNodeId}
        onClose={() => setPickOpen(false)}
        onUse={(stepType, strategy, query, selectors) => {
          if (!pickNodeId) return;
          setDirty(true);
          const picked = { strategy, query, xpath: query, selectors };
          const current = nodes.find((node) => node.id === pickNodeId);
          if (!current) return;
          const holdsSelector = ["SEARCH_ELEMENT", "CLICK", "TYPE_TEXT", "EXTRACT_CONTENT", "HOVER", "SCROLL", "SELECT_OPTION", "UPLOAD", "ENTER_FRAME", "VERIFY", "SET_CHECKBOX", "DOUBLE_CLICK", "RIGHT_CLICK"].includes(String(current.data.nodeType));
          if (current.data.nodeType === stepType) {
            remember();
            setNodes((items) => items.map((node) => node.id === pickNodeId ? { ...node, data: { ...node.data, ...picked } } : node));
          } else {
            const id = `n_${crypto.randomUUID().slice(0, 8)}`;
            remember();
            setNodes((items) => items.map((node) => node.id === pickNodeId && holdsSelector ? { ...node, data: { ...node.data, ...picked } } : node).concat({
              id,
              type: stepType,
              position: { x: current.position.x + 280, y: current.position.y },
              data: { nodeType: stepType, ...picked },
            }));
            setEdges((items) => items.concat([
              { id: `e_${id}`, source: current.id, target: id, sourceHandle: "exec_out", targetHandle: "exec_in" },
              ...(current.data.nodeType === "SEARCH_ELEMENT" ? [{ id: `el_${id}`, source: current.id, target: id, sourceHandle: "element_out", targetHandle: "element_in" }] : []),
            ]));
          }
          setPickOpen(false);
        }}
      />
    </div>
  );
}
