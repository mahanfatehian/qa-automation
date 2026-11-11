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
  workflows: () => request<Workflow[]>("/api/v1/workflows"),
  createWorkflow: (body: { name: string; description?: string; graph?: Workflow["graph"] }) =>
    request<Workflow>("/api/v1/workflows", { method: "POST", body: JSON.stringify(body) }),
  saveWorkflow: (id: string, body: { name?: string; description?: string; graph?: Workflow["graph"] }) =>
    request<Workflow>(`/api/v1/workflows/${id}`, { method: "PUT", body: JSON.stringify(body) }),
  removeWorkflow: (id: string) => request<void>(`/api/v1/workflows/${id}`, { method: "DELETE" }),
  run: (id: string) => request<TestRun>(`/api/v1/workflows/${id}/run`, { method: "POST" }),
  rotateWebhook: (id: string) => request<Workflow>(`/api/v1/workflows/${id}/webhook/rotate`, { method: "POST" }),
  schedules: (id: string) => request<Schedule[]>(`/api/v1/workflows/${id}/schedules`),
  createSchedule: (id: string, body: Partial<Schedule>) =>
    request<Schedule>(`/api/v1/workflows/${id}/schedules`, { method: "POST", body: JSON.stringify(body) }),
  updateSchedule: (id: string, body: Partial<Schedule>) =>
    request<Schedule>(`/api/v1/schedules/${id}`, { method: "PATCH", body: JSON.stringify(body) }),
  removeSchedule: (id: string) => request<void>(`/api/v1/schedules/${id}`, { method: "DELETE" }),
  runs: (workflowId: string) => request<TestRun[]>(`/api/v1/runs?workflow_id=${workflowId}`),
  results: (runId: string) => request<NodeResult[]>(`/api/v1/runs/${runId}/results`),
  probe: (body: { url: string; strategy: string; query: string; timeout_seconds?: number }) =>
    request<{ ok: boolean; count: number; samples: string[]; error: string | null; screenshot_base64: string }>("/api/v1/tools/probe", {
      method: "POST",
      body: JSON.stringify(body),
    }),
  pick: (body: { url: string; browser?: string; headless?: boolean }) =>
    request<PickResult>("/api/v1/tools/pick", { method: "POST", body: JSON.stringify(body) }),
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
