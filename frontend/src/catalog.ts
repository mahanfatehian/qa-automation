import type { StepDef } from "./types/blueprint";

const execIn = { id: "exec_in", label: "exec", kind: "exec" as const, direction: "in" as const };
const execOut = { id: "exec_out", label: "then", kind: "exec" as const, direction: "out" as const };

export const CATALOG: StepDef[] = [
  {
    type: "START",
    title: "Start browser",
    category: "flow",
    hint: "Opens the browser session every later step uses.",
    pins: [execOut],
    defaults: { browser: "chrome", headless: true, windowWidth: 1280, windowHeight: 800 },
  },
  {
    type: "NAVIGATE",
    title: "Open page",
    category: "action",
    hint: "Goes to a URL and waits until the document is loaded.",
    pins: [execIn, { id: "text_in", label: "url", kind: "text", direction: "in" }, execOut, { id: "text_out", label: "url", kind: "text", direction: "out" }],
    defaults: { url: "https://example.com", timeoutSeconds: 20 },
  },
  {
    type: "SEARCH_ELEMENT",
    title: "Find element",
    category: "search",
    hint: "Waits for an element by XPath, CSS, text, or a regex.",
    pins: [execIn, execOut, { id: "element_out", label: "element", kind: "element", direction: "out" }],
    defaults: { strategy: "XPATH", query: "", timeoutSeconds: 10 },
  },
  {
    type: "CLICK",
    title: "Click",
    category: "action",
    hint: "Clicks the element coming in on the green pin, or an XPath.",
    pins: [execIn, { id: "element_in", label: "element", kind: "element", direction: "in" }, execOut],
    defaults: { xpath: "", forceJsClick: false, timeoutSeconds: 10 },
  },
  {
    type: "TYPE_TEXT",
    title: "Type",
    category: "action",
    hint: "Types into an element. Use {{variable}} to insert a saved value.",
    pins: [execIn, { id: "element_in", label: "element", kind: "element", direction: "in" }, { id: "text_in", label: "text", kind: "text", direction: "in" }, execOut],
    defaults: { text: "", xpath: "", clearFirst: true, pressEnter: false },
  },
  {
    type: "EXTRACT_CONTENT",
    title: "Read text",
    category: "search",
    hint: "Reads text, an attribute, HTML, or a regex match into a data pin.",
    pins: [execIn, { id: "element_in", label: "element", kind: "element", direction: "in" }, execOut, { id: "text_out", label: "text", kind: "text", direction: "out" }],
    defaults: { extractType: "INNER_TEXT", saveAsVariable: "", attributeName: "", regexPattern: "" },
  },
  {
    type: "IF_CONDITION",
    title: "If / else",
    category: "flow",
    hint: "White true and false pins choose the next step.",
    pins: [
      execIn,
      { id: "text_in", label: "text", kind: "text", direction: "in" },
      { id: "exec_true", label: "true", kind: "exec", direction: "out" },
      { id: "exec_false", label: "false", kind: "exec", direction: "out" },
      { id: "bool_out", label: "bool", kind: "bool", direction: "out" },
    ],
    defaults: { conditionType: "TEXT_CONTAINS", rightValue: "", xpath: "", timeoutSeconds: 5 },
  },
  {
    type: "LOOP_ELEMENTS",
    title: "Repeat for each",
    category: "flow",
    hint: "Runs the body pin once per matched element, then the done pin.",
    pins: [
      execIn,
      { id: "exec_loop_body", label: "each", kind: "exec", direction: "out" },
      { id: "exec_completed", label: "done", kind: "exec", direction: "out" },
      { id: "current_element", label: "element", kind: "element", direction: "out" },
    ],
    defaults: { xpath: "" },
  },
  {
    type: "WAIT",
    title: "Wait",
    category: "action",
    hint: "Pauses, waits for a URL fragment, or waits for the page to settle.",
    pins: [execIn, execOut],
    defaults: { waitType: "FIXED_MS", value: "1000", timeoutSeconds: 15 },
  },
  {
    type: "ASSERT",
    title: "Check",
    category: "assert",
    hint: "Fails the blueprint if the incoming text does not match.",
    pins: [execIn, { id: "text_in", label: "text", kind: "text", direction: "in" }, execOut],
    defaults: { operator: "NOT_EMPTY", expected: "" },
  },
];

export const CATALOG_BY_TYPE = Object.fromEntries(CATALOG.map((step) => [step.type, step]));

export function pinKind(handle?: string | null): string {
  if (!handle) return "exec";
  if (handle.startsWith("exec_")) return "exec";
  if (handle.includes("element")) return "element";
  if (handle.includes("bool")) return "bool";
  return "text";
}
