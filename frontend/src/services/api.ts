import type { NodeResult, Schedule, TestRun, Workflow } from "../types/blueprint";

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(path, {
    ...init,
    headers: {
      "Content-Type": "application/json",
      ...(init?.headers || {}),
    },
  });
  if (!response.ok) {
    let detail = response.statusText;
    try {
      const body = await response.json();
      detail = typeof body.detail === "string" ? body.detail : JSON.stringify(body.detail ?? body);
    } catch {
      detail = response.statusText;
    }
    throw new Error(detail);
  }
  if (response.status === 204) return undefined as T;
  const text = await response.text();
  return (text ? JSON.parse(text) : undefined) as T;
}

export const api = {
  status: () => request<{ database: boolean; redis: boolean; grid: { ready: boolean; node_count: number; message: string } }>("/api/v1/system/status"),
  settings: () => request<{ browser_slots: number; ready_ms: number; window_width: number; window_height: number; scale_note?: string; grid?: { ready: boolean; nodes: number; slots: number } }>("/api/v1/settings"),
  saveSettings: (body: { browser_slots: number; ready_ms: number; window_width: number; window_height: number }) =>
    request<{ browser_slots: number; ready_ms: number; window_width: number; window_height: number; scale_note?: string; grid?: { ready: boolean; nodes: number; slots: number } }>("/api/v1/settings", { method: "PUT", body: JSON.stringify(body) }),
  workflows: () => request<Workflow[]>("/api/v1/workflows"),
  createWorkflow: (body: { name: string; description?: string; graph?: Workflow["graph"] }) =>
    request<Workflow>("/api/v1/workflows", { method: "POST", body: JSON.stringify(body) }),
  saveWorkflow: (id: string, body: { name?: string; description?: string; graph?: Workflow["graph"] }) =>
    request<Workflow>(`/api/v1/workflows/${id}`, { method: "PUT", body: JSON.stringify(body) }),
  removeWorkflow: (id: string) => request<void>(`/api/v1/workflows/${id}`, { method: "DELETE" }),
  run: (id: string) => request<TestRun>(`/api/v1/workflows/${id}/run`, { method: "POST" }),
  rotateWebhook: (id: string) => request<Workflow>(`/api/v1/workflows/${id}/webhook/rotate`, { method: "POST" }),
  schedules: (id: string) => request<Schedule[]>(`/api/v1/workflows/${id}/schedules`),
  automationLogs: (id: string) => request<Array<{ id: string; run_id?: string | null; level: string; message: string; created_at: string }>>(`/api/v1/workflows/${id}/automation-logs`),
  createSchedule: (id: string, body: Partial<Schedule>) =>
    request<Schedule>(`/api/v1/workflows/${id}/schedules`, { method: "POST", body: JSON.stringify(body) }),
  updateSchedule: (id: string, body: Partial<Schedule>) =>
    request<Schedule>(`/api/v1/schedules/${id}`, { method: "PATCH", body: JSON.stringify(body) }),
  removeSchedule: (id: string) => request<void>(`/api/v1/schedules/${id}`, { method: "DELETE" }),
  getRun: (id: string) => request<TestRun>(`/api/v1/runs/${id}`),
  runs: (workflowId: string) => request<TestRun[]>(`/api/v1/runs?workflow_id=${workflowId}`),
  removeRun: (id: string) => request<void>(`/api/v1/runs/${id}`, { method: "DELETE" }),
  results: (runId: string) => request<NodeResult[]>(`/api/v1/runs/${runId}/results`),
  probe: (body: { url: string; strategy: string; query: string; timeout_seconds?: number }) =>
    request<{ ok: boolean; count: number; samples: string[]; error: string | null; screenshot_base64: string }>("/api/v1/tools/probe", {
      method: "POST",
      body: JSON.stringify(body),
    }),
  pick: (body: { url: string; browser?: string; headless?: boolean }) =>
    request<PickResult>("/api/v1/tools/pick", { method: "POST", body: JSON.stringify(body) }),
  inspectStart: (body: { nodes: unknown[]; edges: unknown[]; stop_before?: string; variables?: Record<string, unknown> }) =>
    request<InspectSession>("/api/v1/tools/inspect/start", { method: "POST", body: JSON.stringify(body) }),
  inspectHover: (sessionId: string, x: number, y: number) =>
    request<{ ok: boolean; element: InspectElement | null; error?: string }>(`/api/v1/tools/inspect/${sessionId}/hover`, {
      method: "POST",
      body: JSON.stringify({ x, y }),
    }),
  inspectResolve: (sessionId: string, element: InspectElement) =>
    request<{ ok: boolean; selectors: Array<{ kind: string; value: string; rank?: number }> }>(`/api/v1/tools/inspect/${sessionId}/resolve`, {
      method: "POST",
      body: JSON.stringify({
        path: element.path || [],
        tag: element.tag,
        id: element.id,
        name: element.name,
        text: element.text,
        aria: element.aria,
        placeholder: element.placeholder,
        type: element.type,
      }),
    }),
  inspectClose: (sessionId: string) => request<void>(`/api/v1/tools/inspect/${sessionId}/close`, { method: "POST" }),
};

export type InspectElement = {
  tag: string;
  type: string;
  kind: string;
  text: string;
  name: string;
  id: string;
  placeholder: string;
  aria: string;
  x: number;
  y: number;
  width: number;
  height: number;
  options: Array<{ text: string; value: string }>;
  selectors: Array<{ kind: string; value: string }>;
  suggested: string[];
  path?: Array<Record<string, unknown>>;
};

export type InspectSession = {
  ok: boolean;
  session_id: string;
  current_url: string;
  title: string;
  screenshot_base64: string;
  screenshot_mime?: string;
  viewport: { width?: number; height?: number };
  error?: string;
};

export type PickResult = {
  ok: boolean;
  current_url: string;
  title: string;
  screenshot_base64: string;
  viewport: { width?: number; height?: number };
  elements: Array<{
    tag: string;
    text: string;
    x: number;
    y: number;
    width: number;
    height: number;
    selectors: Array<{ kind: string; value: string; rank: number }>;
  }>;
  error: string | null;
};
