export type PinKind = "exec" | "element" | "text" | "bool";

export type PinDef = {
  id: string;
  label: string;
  kind: PinKind;
  direction: "in" | "out";
};

export type StepCategory = "flow" | "action" | "search" | "assert";

export type StepDef = {
  type: string;
  title: string;
  category: StepCategory;
  hint: string;
  pins: PinDef[];
  defaults: Record<string, unknown>;
};

export type BlueprintData = {
  nodeType: string;
  title?: string;
  runStatus?: string;
  [key: string]: unknown;
};

export type TelemetryEvent = {
  event_type: string;
  run_id?: string;
  node_id?: string;
  node_type?: string;
  status?: string;
  duration_ms?: number;
  message?: string;
  error_stack?: string;
  output_data?: Record<string, unknown>;
  screenshot_base64?: string;
  screenshot_omitted?: boolean;
  result_id?: string;
  sequence?: number;
};

export type Workflow = {
  id: string;
  name: string;
  description: string;
  graph: {
    nodes: Array<Record<string, unknown>>;
    edges: Array<Record<string, unknown>>;
    maxSteps?: number;
    variables?: Record<string, unknown>;
    examples?: Array<Record<string, string>>;
    dataColumns?: Array<{ name: string; type: "text" | "long" | "number" | "url" | "boolean" | "choice"; options?: string[] }>;
  };
  webhook_token: string;
  created_at: string;
  updated_at: string;
};

export type Schedule = {
  id: string;
  workflow_id: string;
  name: string;
  schedule_type: "CRON" | "INTERVAL";
  cron_expression: string | null;
  interval_seconds: number | null;
  enabled: boolean;
  last_run_at: string | null;
};

export type TestRun = {
  id: string;
  workflow_id: string;
  status: string;
  trigger: string;
  error_message: string | null;
  variation_label?: string | null;
  started_at: string | null;
  finished_at: string | null;
  created_at: string;
};

export type NodeResult = {
  id: string;
  run_id: string;
  node_id: string;
  node_type: string;
  status: string;
  duration_ms: number;
  message: string;
  error_message: string;
  output_data: Record<string, unknown>;
  has_screenshot: boolean;
  sequence: number;
  created_at: string;
};
