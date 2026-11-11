import type { Edge, Node } from "@xyflow/react";
import type { BlueprintData } from "./types/blueprint";

export function starterNodes(): Node<BlueprintData>[] {
  return [
    {
      id: "start",
      type: "START",
      position: { x: 80, y: 200 },
      data: {
        nodeType: "START",
        title: "Start browser",
        browser: "chrome",
        headless: true,
        windowWidth: 1280,
        windowHeight: 800,
      },
    },
  ];
}

export function demoGraph(): { nodes: Node<BlueprintData>[]; edges: Edge[] } {
  const nodes: Node<BlueprintData>[] = [
    node("start", "START", 40, 220, { browser: "chrome", headless: true, windowWidth: 1280, windowHeight: 800 }),
    node("open", "NAVIGATE", 340, 220, { url: "https://quotes.toscrape.com", timeoutSeconds: 20 }),
    node("find", "SEARCH_ELEMENT", 660, 220, {
      strategy: "XPATH",
      query: "//div[@class='quote'][1]/span[@class='text']",
      timeoutSeconds: 12,
    }),
    node("read", "EXTRACT_CONTENT", 980, 220, { extractType: "INNER_TEXT", saveAsVariable: "quoteText" }),
    node("branch", "IF_CONDITION", 1300, 220, { conditionType: "TEXT_CONTAINS", rightValue: "world" }),
    node("check", "ASSERT", 1620, 80, { operator: "NOT_EMPTY", expected: "" }),
  ];
  const edges: Edge[] = [
    exec("e1", "start", "open"),
    exec("e2", "open", "find"),
    exec("e3", "find", "read"),
    exec("e4", "read", "branch"),
    exec("e5", "branch", "check", "exec_true", "exec_in"),
    data("d1", "find", "read", "element_out", "element_in", "#3ddc97"),
    data("d2", "read", "branch", "text_out", "text_in", "#49c2ff"),
    data("d3", "read", "check", "text_out", "text_in", "#49c2ff"),
  ];
  return { nodes, edges };
}

function node(id: string, type: string, x: number, y: number, data: Record<string, unknown>): Node<BlueprintData> {
  return {
    id,
    type,
    position: { x, y },
    data: { nodeType: type, title: type, ...data },
  };
}

function exec(id: string, source: string, target: string, sourceHandle = "exec_out", targetHandle = "exec_in"): Edge {
  return {
    id,
    source,
    target,
    sourceHandle,
    targetHandle,
    style: { stroke: "#f4f7fb", strokeWidth: 1.6 },
  };
}

function data(id: string, source: string, target: string, sourceHandle: string, targetHandle: string, color: string): Edge {
  return {
    id,
    source,
    target,
    sourceHandle,
    targetHandle,
    style: { stroke: color, strokeWidth: 1.6, strokeDasharray: "4 3" },
  };
}
