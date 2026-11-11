import { useCallback, useEffect, useState } from "react";
import { useEdgesState, useNodesState, type Edge, type Node } from "@xyflow/react";
import { BlueprintCanvas } from "./components/BlueprintCanvas";
import { LiveRunOverlay } from "./components/LiveRunOverlay";
import { NodeInspector } from "./components/NodeInspector";
import { NodePalette } from "./components/NodePalette";
import { PickerModal } from "./components/PickerModal";
import { ScheduleModal } from "./components/ScheduleModal";
import { demoGraph, fixtureActionsGraph, fixtureGraph, starterNodes } from "./demo";
import { api, type PickResult } from "./services/api";
import { openRunSocket } from "./services/socket";
import type { BlueprintData, TelemetryEvent, TestRun, Workflow } from "./types/blueprint";

export function App() {
  const [workflows, setWorkflows] = useState<Workflow[]>([]);
  const [activeId, setActiveId] = useState<string | null>(null);
  const [name, setName] = useState("Untitled test");
  const [nodes, setNodes, onNodesChange] = useNodesState<Node<BlueprintData>>([]);
  const [edges, setEdges, onEdgesChange] = useEdgesState<Edge>([]);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [dirty, setDirty] = useState(false);
  const [grid, setGrid] = useState("checking");
  const [run, setRun] = useState<TestRun | null>(null);
  const [events, setEvents] = useState<TelemetryEvent[]>([]);
  const [shot, setShot] = useState("");
  const [error, setError] = useState("");
  const [scheduleOpen, setScheduleOpen] = useState(false);
  const [probing, setProbing] = useState(false);
  const [probeMessage, setProbeMessage] = useState("");
  const [runs, setRuns] = useState<TestRun[]>([]);
  const [pickOpen, setPickOpen] = useState(false);
  const [pickBusy, setPickBusy] = useState(false);
  const [pickResult, setPickResult] = useState<PickResult | null>(null);
  const [pickError, setPickError] = useState("");
  const [pickNodeId, setPickNodeId] = useState<string | null>(null);

  const active = workflows.find((item) => item.id === activeId) || null;
  const selected = nodes.find((node) => node.id === selectedId) || null;

  const loadList = useCallback(async () => {
    const rows = await api.workflows();
    setWorkflows(rows);
    return rows;
  }, []);

  function applyGraph(workflow: Workflow) {
    const graph = workflow.graph || { nodes: [], edges: [] };
    setName(workflow.name);
    setNodes((graph.nodes || []) as Node<BlueprintData>[]);
    setEdges((graph.edges || []) as Edge[]);
    setSelectedId(null);
    setDirty(false);
    setRun(null);
    setEvents([]);
    setShot("");
    api.runs(workflow.id).then(setRuns).catch(() => setRuns([]));
  }

  useEffect(() => {
    loadList().then((rows) => {
      if (rows[0]) {
        setActiveId(rows[0].id);
        applyGraph(rows[0]);
      }
    }).catch((err: Error) => setError(err.message));
    const timer = window.setInterval(() => {
      api.status().then((status) => {
        setGrid(status.grid.ready ? `${status.grid.node_count} browsers` : "grid down");
      }).catch(() => setGrid("api down"));
    }, 8000);
    return () => window.clearInterval(timer);
  }, [loadList]);

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
        setRun((current) => current ? { ...current, status: event.status || (event.event_type === "RUN_COMPLETED" ? "PASSED" : event.event_type === "RUN_FAILED" ? "FAILED" : current.status), error_message: event.message || current.error_message } : current);
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
    setEvents(results.map((row) => ({
      event_type: `NODE_${row.status}`,
      run_id: item.id,
      node_id: row.node_id,
      node_type: row.node_type,
      duration_ms: row.duration_ms,
      message: row.message,
      error_stack: row.error_message,
      output_data: row.output_data,
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
    }
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  });

  return (
    <div className="flex h-screen flex-col">
      <header className="flex h-14 shrink-0 items-center gap-3 border-b border-line bg-[#0b141e] px-3">
        <div className="grid h-7 w-7 place-items-center bg-[#49c2ff] text-xs font-bold text-[#081018]" style={{ clipPath: "polygon(50% 0, 100% 50%, 50% 100%, 0 50%)" }}>B</div>
        <div>
          <p className="text-sm font-semibold leading-4">BlueprintQA</p>
          <p className="font-mono text-[10px] text-[#8ea6bb]">node to node browser tests</p>
        </div>
        <select className="btn max-w-56" value={activeId || ""} onChange={(event) => {
          const next = workflows.find((item) => item.id === event.target.value);
          if (!next) return;
          setActiveId(next.id);
          applyGraph(next);
        }}>
          {workflows.map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}
        </select>
        <input className="btn w-48" value={name} onChange={(event) => { setName(event.target.value); setDirty(true); }} />
        <button className="btn" onClick={() => { if (!confirmDirty()) return; createTest().catch((err: Error) => setError(err.message)); }}>New</button>
        <button className="btn" onClick={() => { if (!confirmDirty()) return; const demo = demoGraph(); setNodes(demo.nodes); setEdges(demo.edges); setDirty(true); }}>Load demo</button>
        <button className="btn" onClick={() => { if (!confirmDirty()) return; const fixture = fixtureGraph(); setNodes(fixture.nodes); setEdges(fixture.edges); setDirty(true); }}>Load fixture</button>
        <button className="btn" onClick={() => { if (!confirmDirty()) return; const fixture = fixtureActionsGraph(); setNodes(fixture.nodes); setEdges(fixture.edges); setDirty(true); }}>Load actions</button>
        <button className="btn btn-danger" onClick={() => {
          if (!activeId || !window.confirm("Delete this blueprint and its runs?")) return;
          api.removeWorkflow(activeId).then(() => loadList()).then((rows) => {
            if (rows && rows[0]) { setActiveId(rows[0].id); applyGraph(rows[0]); }
            else { setActiveId(null); setNodes([]); setEdges([]); }
          }).catch((err: Error) => setError(err.message));
        }}>Delete</button>
        <button className="btn" onClick={() => save().catch((err: Error) => setError(err.message))}>{dirty ? "Save" : "Saved"}</button>
        <button className="btn btn-run" onClick={() => runBlueprint().catch((err: Error) => setError(err.message))}>Run blueprint</button>
        <button className="btn" onClick={() => setScheduleOpen(true)}>Automatic runs</button>
        <span className="ml-auto font-mono text-[11px] text-[#8ea6bb]">{grid}</span>
        {run && <a className="btn" href={`/api/v1/runs/${run.id}/junit`}>JUnit</a>}
      </header>
      {error && <p className="bg-[#3a1420] px-3 py-1 text-sm text-[#ffb3c1]">{error}</p>}
      <div className="flex min-h-0 flex-1">
        <NodePalette />
        <BlueprintCanvas
          nodes={nodes}
          edges={edges}
          onNodesChange={(changes) => { setDirty(true); onNodesChange(changes); }}
          onEdgesChange={(changes) => { setDirty(true); onEdgesChange(changes); }}
          setNodes={(value) => { setDirty(true); setNodes(value); }}
          setEdges={(value) => { setDirty(true); setEdges(value); }}
          onSelect={setSelectedId}
        />
        <NodeInspector
          node={selected}
          probing={probing}
          probeMessage={probeMessage}
          onPick={async (node) => {
            const opener = nodes.find((item) => item.data.nodeType === "NAVIGATE");
            const url = String(opener?.data.url || "");
            if (!url) {
              setPickError("Add an Open page step with a URL first.");
              setPickOpen(true);
              setPickResult(null);
              return;
            }
            setPickNodeId(node.id);
            setPickOpen(true);
            setPickBusy(true);
            setPickError("");
            setPickResult(null);
            try {
              const result = await api.pick({ url, browser: "chrome", headless: true });
              setPickResult(result);
              if (!result.ok) setPickError(result.error || "Pick failed");
            } catch (err) {
              setPickError(err instanceof Error ? err.message : "Pick failed");
            } finally {
              setPickBusy(false);
            }
          }}
          onChange={(id, patch) => {
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
        />
      </div>
      <LiveRunOverlay run={run} events={events} shot={shot} runs={runs} onPick={(event) => { if (event.screenshot_base64) setShot(event.screenshot_base64); }} onOpenRun={(item) => openPast(item).catch((err: Error) => setError(err.message))} />
      <ScheduleModal open={scheduleOpen} workflowId={activeId} webhookToken={active?.webhook_token || ""} onClose={() => setScheduleOpen(false)} />
      <PickerModal
        open={pickOpen}
        busy={pickBusy}
        result={pickResult}
        error={pickError}
        onClose={() => setPickOpen(false)}
        onChoose={(strategy, query) => {
          if (!pickNodeId) return;
          setDirty(true);
          setNodes((current) => current.map((node) => node.id === pickNodeId ? { ...node, data: { ...node.data, strategy, query, xpath: query } } : node));
          setPickOpen(false);
        }}
      />
    </div>
  );
}
