import { useEffect, useRef, useState, type MouseEvent } from "react";
import { api, type InspectElement, type InspectSession } from "../services/api";

type Props = {
  open: boolean;
  nodes: unknown[];
  edges: unknown[];
  stopBefore: string | null;
  onClose: () => void;
  onUse: (stepType: string, strategy: string, query: string, selectors: Array<{ kind: string; value: string }>) => void;
};

const labels: Record<string, string> = {
  SELECT_OPTION: "Add Select",
  SET_CHECKBOX: "Add Checkbox",
  TYPE_TEXT: "Add Type",
  CLICK: "Add Click",
  UPLOAD: "Add Upload",
  VERIFY: "Add Verify",
  SEARCH_ELEMENT: "Add Find",
};

export function InspectModal({ open, nodes, edges, stopBefore, onClose, onUse }: Props) {
  const [session, setSession] = useState<InspectSession | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [element, setElement] = useState<InspectElement | null>(null);
  const [locked, setLocked] = useState(false);
  const [resolving, setResolving] = useState(false);
  const [attempt, setAttempt] = useState(0);
  const imageRef = useRef<HTMLImageElement | null>(null);
  const timer = useRef<number | null>(null);
  const sessionId = useRef("");

  useEffect(() => {
    if (!open) return;
    let cancelled = false;
    setBusy(true);
    setError("");
    setElement(null);
    setLocked(false);
    const payload = {
      nodes: nodes.map((node) => {
        const item = node as { id?: string; type?: string; position?: { x: number; y: number }; data?: Record<string, unknown> };
        const data = { ...(item.data || {}) };
        delete data.runStatus;
        return { id: item.id, type: item.type || data.nodeType, position: item.position, data };
      }),
      edges: edges.map((edge) => {
        const item = edge as { source?: string; target?: string; sourceHandle?: string; targetHandle?: string };
        return { source: item.source, target: item.target, sourceHandle: item.sourceHandle, targetHandle: item.targetHandle };
      }),
      stop_before: stopBefore || undefined,
    };
    api.inspectStart(payload).then((result) => {
      if (cancelled) {
        if (result.session_id) api.inspectClose(result.session_id).catch(() => undefined);
        return;
      }
      setSession(result);
      sessionId.current = result.session_id;
      if (!result.ok) setError(result.error || "Could not open the page");
    }).catch((err: Error) => {
      if (!cancelled) setError(err.message === "Failed to fetch" ? "The page took too long to open, or the studio lost the runner. Try again." : err.message);
    }).finally(() => {
      if (!cancelled) setBusy(false);
    });
    return () => {
      cancelled = true;
      if (sessionId.current) api.inspectClose(sessionId.current).catch(() => undefined);
      sessionId.current = "";
    };
  }, [open, stopBefore, attempt]);

  if (!open) return null;
  const viewport = session?.viewport || {};
  const width = viewport.width || 1280;
  const height = viewport.height || 800;

  function move(event: MouseEvent<HTMLImageElement>) {
    if (locked || !sessionId.current || !imageRef.current) return;
    const box = imageRef.current.getBoundingClientRect();
    const x = Math.round(((event.clientX - box.left) / box.width) * width);
    const y = Math.round(((event.clientY - box.top) / box.height) * height);
    if (timer.current) window.clearTimeout(timer.current);
    timer.current = window.setTimeout(() => {
      api.inspectHover(sessionId.current, x, y).then((result) => {
        if (result.ok && result.element) setElement(result.element);
      }).catch(() => undefined);
    }, 60);
  }

  function lock() {
    if (!element || !sessionId.current) return;
    setLocked(true);
    setResolving(true);
    api.inspectResolve(sessionId.current, element).then((result) => {
      if (result.selectors?.length) {
        setElement((current) => current ? { ...current, selectors: result.selectors } : current);
      }
    }).catch(() => undefined).finally(() => setResolving(false));
  }

  function choose(stepType: string) {
    const selectors = element?.selectors || [];
    const selector = selectors[0];
    if (!selector) return;
    onUse(stepType, selector.kind, selector.value, selectors);
    onClose();
  }

  return (
    <div className="fixed inset-0 z-30 grid place-items-center bg-black/70 p-4" onClick={onClose}>
      <div className="panel flex max-h-[92vh] w-full max-w-5xl flex-col rounded-lg border border-line" onClick={(event) => event.stopPropagation()}>
        <div className="flex items-center justify-between border-b border-line px-4 py-3">
          <div>
            <h2 className="text-lg">Select on page</h2>
            <p className="text-xs text-[#8ea6bb]">Move the cursor. Click to lock. The page is the blueprint state before this step.</p>
          </div>
          <button className="btn" onClick={onClose}>Close</button>
        </div>
        <div className="grid min-h-0 flex-1 grid-cols-[1fr_300px]">
          <div className="overflow-auto bg-[#070d14] p-3">
            {busy && <p className="text-sm text-[#8ea6bb]">Replaying the path to this step...</p>}
            {error && (
              <div className="mb-3">
                <p className="text-sm text-[#ff8fa3]">{error}</p>
                <button className="btn mt-2" onClick={() => setAttempt((value) => value + 1)}>Try again</button>
              </div>
            )}
            {session?.screenshot_base64 && (
              <div className="relative inline-block">
                <img
                  ref={imageRef}
                  alt="Current page"
                  className="max-w-full cursor-crosshair"
                  src={`data:${session.screenshot_mime || "image/png"};base64,${session.screenshot_base64}`}
                  onMouseMove={move}
                  onClick={lock}
                />
                {element && (
                  <div
                    className="pointer-events-none absolute border-2 border-[#49c2ff] bg-[#49c2ff]/15"
                    style={{
                      left: `${(element.x / width) * 100}%`,
                      top: `${(element.y / height) * 100}%`,
                      width: `${(element.width / width) * 100}%`,
                      height: `${(element.height / height) * 100}%`,
                    }}
                  />
                )}
              </div>
            )}
          </div>
          <aside className="overflow-auto border-l border-line p-3 text-sm">
            {!element && <p className="text-[#8ea6bb]">Hover the page.</p>}
            {element && (
              <>
                <p className="font-mono text-[10px] uppercase tracking-[0.14em] text-[#8ea6bb]">{locked ? "Locked" : "Hover"} · {element.tag}{element.name ? `[name=${element.name}]` : ""}</p>
                <p className="mt-1 text-[#e8f0f7]">{element.text || element.placeholder || element.tag}</p>
                {element.options?.length > 0 && (
                  <ul className="mt-2 text-xs text-[#9ad8ff]">
                    {element.options.slice(0, 8).map((option) => <li key={option.value}>{option.text}</li>)}
                  </ul>
                )}
                <p className="mt-3 font-mono text-[11px] text-[#8ea6bb]">{resolving ? "Building a stable path..." : `${element.selectors[0]?.kind || ""} ${element.selectors[0]?.value || ""}`}</p>
                <div className="mt-3 flex flex-col gap-2">
                  {(element.suggested || []).map((step) => (
                    <button key={step} className="btn text-left" disabled={resolving} onClick={() => choose(step)}>{labels[step] || step}</button>
                  ))}
                </div>
              </>
            )}
          </aside>
        </div>
      </div>
    </div>
  );
}
