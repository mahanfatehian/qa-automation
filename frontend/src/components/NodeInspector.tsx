import type { Node } from "@xyflow/react";
import { CATALOG_BY_TYPE } from "../catalog";
import type { DataColumn } from "./TestDataDesk";
import type { BlueprintData } from "../types/blueprint";

type Props = {
  node: Node<BlueprintData> | null;
  dataColumns: DataColumn[];
  rows: Array<Record<string, string>>;
  onChange: (id: string, patch: Record<string, unknown>) => void;
  onProbe: (node: Node<BlueprintData>) => void;
  onPick: (node: Node<BlueprintData>) => void;
  probing: boolean;
  probeMessage: string;
};

export function NodeInspector({ node, dataColumns, rows, onChange, onProbe, onPick, probing, probeMessage }: Props) {
  if (!node) {
    return (
      <aside className="panel w-72 shrink-0 border-l border-line p-4 text-sm text-[#8ea6bb]">
        Select a step. White pins set the order. Colored pins carry the element or the text.
      </aside>
    );
  }
  fieldColumns = dataColumns;
  const data = node.data;
  const def = CATALOG_BY_TYPE[String(data.nodeType)];
  const set = (patch: Record<string, unknown>) => onChange(node.id, patch);
  return (
    <aside className="panel w-72 shrink-0 overflow-auto border-l border-line p-4">
      <p className="font-mono text-[10px] uppercase tracking-[0.16em] text-[#8ea6bb]">{def?.title || data.nodeType}</p>
      <p className="mb-4 mt-1 text-xs leading-5 text-[#8ea6bb]">{def?.hint}</p>
      <label className="field">
        Label
        <input value={String(data.title || "")} onChange={(event) => set({ title: event.target.value })} />
      </label>
      {data.nodeType === "START" && (
        <>
          <Select label="Browser" value={String(data.browser || "chrome")} options={["chrome", "firefox"]} onChange={(browser) => set({ browser })} />
          <Check label="Hide browser window" checked={data.headless === true} onChange={(headless) => set({ headless })} />
          <NumberField label="Width" value={Number(data.windowWidth || 1280)} onChange={(windowWidth) => set({ windowWidth })} />
          <NumberField label="Height" value={Number(data.windowHeight || 800)} onChange={(windowHeight) => set({ windowHeight })} />
        </>
      )}
      {data.nodeType === "NAVIGATE" && (
        <>
          <Text label="URL" value={String(data.url || "")} onChange={(url) => set({ url })} />
          <NumberField label="Timeout seconds" value={Number(data.timeoutSeconds || 20)} onChange={(timeoutSeconds) => set({ timeoutSeconds })} />
        </>
      )}
      {["SEARCH_ELEMENT", "CLICK", "TYPE_TEXT", "EXTRACT_CONTENT", "HOVER", "SCROLL", "SELECT_OPTION", "UPLOAD", "ENTER_FRAME"].includes(String(data.nodeType)) && (
        <button className="btn mb-3 w-full" onClick={() => onPick(node)}>Select on page</button>
      )}
      {data.nodeType === "SEARCH_ELEMENT" && (
        <>
          <FindBy value={String(data.strategy || "XPATH")} onChange={(strategy) => set({ strategy })} />
          <Area label="Query" value={String(data.query || "")} onChange={(query) => set({ query })} />
          <NumberField label="Timeout seconds" value={Number(data.timeoutSeconds || 10)} onChange={(timeoutSeconds) => set({ timeoutSeconds })} />
          <button className="btn mb-2 w-full" disabled={probing} onClick={() => onProbe(node)}>{probing ? "Probing grid..." : "Probe this selector"}</button>
          {probeMessage && <p className="mb-3 text-xs text-[#9ad8ff]">{probeMessage}</p>}
        </>
      )}
      {(data.nodeType === "CLICK" || data.nodeType === "TYPE_TEXT") && (
        <>
          <FindBy value={String(data.strategy || "XPATH")} onChange={(strategy) => set({ strategy })} />
          <Text label="Selector if no element pin" value={String(data.xpath || "")} onChange={(xpath) => set({ xpath, query: xpath })} />
        </>
      )}
      {data.nodeType === "CLICK" && <Check label="Force JavaScript click" checked={Boolean(data.forceJsClick)} onChange={(forceJsClick) => set({ forceJsClick })} />}
      {data.nodeType === "TYPE_TEXT" && (
        <>
          <SiteValue label="Text typed into the page" value={String(data.text || "")} onChange={(text) => set({ text })} />
          <Check label="Clear first" checked={data.clearFirst !== false} onChange={(clearFirst) => set({ clearFirst })} />
          <Check label="Press Enter" checked={Boolean(data.pressEnter)} onChange={(pressEnter) => set({ pressEnter })} />
        </>
      )}
      {data.nodeType === "EXTRACT_CONTENT" && (
        <>
          <Select label="Read" value={String(data.extractType || "INNER_TEXT")} options={["INNER_TEXT", "ATTRIBUTE", "OUTER_HTML", "REGEX_MATCH"]} onChange={(extractType) => set({ extractType })} />
          {data.extractType === "ATTRIBUTE" && <Text label="Attribute" value={String(data.attributeName || "")} onChange={(attributeName) => set({ attributeName })} />}
          {data.extractType === "REGEX_MATCH" && <Text label="Regex" value={String(data.regexPattern || "")} onChange={(regexPattern) => set({ regexPattern })} />}
          <Text label="Save as variable" value={String(data.saveAsVariable || "")} onChange={(saveAsVariable) => set({ saveAsVariable })} />
        </>
      )}
      {data.nodeType === "IF_CONDITION" && (
        <>
          <Select label="Condition" value={String(data.conditionType || "TEXT_CONTAINS")} options={["ELEMENT_EXISTS", "TEXT_CONTAINS", "TEXT_EQUALS", "REGEX_MATCH"]} onChange={(conditionType) => set({ conditionType })} />
          {data.conditionType === "ELEMENT_EXISTS" ? (
            <Area label="XPath" value={String(data.xpath || "")} onChange={(xpath) => set({ xpath })} />
          ) : (
            <Text label="Expected" value={String(data.rightValue || "")} onChange={(rightValue) => set({ rightValue })} />
          )}
        </>
      )}
      {data.nodeType === "LOOP_ELEMENTS" && <Area label="XPath" value={String(data.xpath || "")} onChange={(xpath) => set({ xpath })} />}
      {data.nodeType === "WAIT" && (
        <>
          <Select label="Wait for" value={String(data.waitType || "FIXED_MS")} options={["FIXED_MS", "URL_CONTAINS", "NETWORK_IDLE", "ELEMENT_VISIBLE", "ELEMENT_CLICKABLE"]} onChange={(waitType) => set({ waitType })} />
          <Text label="Value" value={String(data.value || "")} onChange={(value) => set({ value })} />
        </>
      )}
      {data.nodeType === "SCROLL" && (
        <>
          <Select label="Mode" value={String(data.mode || "PIXELS")} options={["PIXELS", "ELEMENT", "TOP", "BOTTOM"]} onChange={(mode) => set({ mode })} />
          {data.mode === "PIXELS" || !data.mode ? (
            <>
              <Select label="Direction" value={String(data.direction || "down")} options={["down", "up", "left", "right"]} onChange={(direction) => set({ direction })} />
              <NumberField label="Pixels" value={Number(data.pixels || 600)} onChange={(pixels) => set({ pixels })} />
            </>
          ) : null}
          {data.mode === "ELEMENT" && (
            <>
              <FindBy value={String(data.strategy || "XPATH")} onChange={(strategy) => set({ strategy })} />
              <Text label="Selector if no element pin" value={String(data.xpath || "")} onChange={(xpath) => set({ xpath })} />
            </>
          )}
        </>
      )}
      {data.nodeType === "SELECT_OPTION" && (
        <>
          <SiteValue label="Option chosen on the page" value={String(data.option || "")} onChange={(option) => set({ option })} />
          <Select label="Match by" value={String(data.by || "TEXT")} options={["TEXT", "VALUE"]} onChange={(by) => set({ by })} />
        </>
      )}
      {["HOVER", "ENTER_FRAME", "DOUBLE_CLICK", "RIGHT_CLICK", "DRAG_DROP"].includes(String(data.nodeType)) && (
        <>
          <FindBy value={String(data.strategy || "XPATH")} onChange={(strategy) => set({ strategy })} />
          <Text label="Selector if no element pin" value={String(data.xpath || data.query || "")} onChange={(xpath) => set({ xpath, query: xpath })} />
        </>
      )}
      {data.nodeType === "PRESS_KEY" && <Text label="Key" value={String(data.key || "ENTER")} onChange={(key) => set({ key })} />}
      {data.nodeType === "SWITCH_TAB" && (
        <>
          <NumberField label="Tab index" value={Number(data.index || 0)} onChange={(index) => set({ index })} />
          <Text label="Or URL contains" value={String(data.urlContains || "")} onChange={(urlContains) => set({ urlContains })} />
        </>
      )}
      {data.nodeType === "ALERT" && <Select label="Action" value={String(data.action || "ACCEPT")} options={["ACCEPT", "DISMISS"]} onChange={(action) => set({ action })} />}
      {data.nodeType === "UPLOAD" && <Text label="File inside the runner" value={String(data.filePath || "/app/fixtures/sample.txt")} onChange={(filePath) => set({ filePath })} />}
      {data.nodeType === "VERIFY" && (
        <>
          <Select label="Check" value={String(data.check || "VISIBLE")} options={["VISIBLE", "ENABLED", "SELECTED", "ATTRIBUTE", "COUNT", "TEXT_CONTAINS", "TEXT_EQUALS", "URL_CONTAINS", "URL_EQUALS", "TITLE_CONTAINS"]} onChange={(check) => set({ check })} />
          <FindBy value={String(data.strategy || "XPATH")} onChange={(strategy) => set({ strategy })} />
          <Text label="Selector if no element pin" value={String(data.xpath || "")} onChange={(xpath) => set({ xpath })} />
          {data.check === "ATTRIBUTE" && <Text label="Attribute" value={String(data.attributeName || "")} onChange={(attributeName) => set({ attributeName })} />}
          <Text label="Expected" value={String(data.expected || "")} onChange={(expected) => set({ expected })} />
        </>
      )}
      {data.nodeType === "SET_CHECKBOX" && (
        <>
          <Check label="Checked" checked={data.checked !== false} onChange={(checked) => set({ checked })} />
          <Text label="Selector if no element pin" value={String(data.xpath || "")} onChange={(xpath) => set({ xpath })} />
        </>
      )}
      {data.nodeType === "COOKIE" && (
        <>
          <Select label="Action" value={String(data.action || "GET")} options={["GET", "SET", "DELETE", "DELETE_ALL"]} onChange={(action) => set({ action })} />
          <Text label="Name" value={String(data.cookieName || "")} onChange={(cookieName) => set({ cookieName })} />
          <Text label="Value" value={String(data.cookieValue || "")} onChange={(cookieValue) => set({ cookieValue })} />
        </>
      )}
      {data.nodeType === "DRAG_DROP" && <Text label="Target selector" value={String(data.targetXpath || "")} onChange={(targetXpath) => set({ targetXpath })} />}
      {data.nodeType === "SCREENSHOT" && <Text label="Label" value={String(data.label || "")} onChange={(label) => set({ label })} />}
      {data.nodeType === "OPEN_TAB" && <SiteValue label="URL" value={String(data.url || "")} onChange={(url) => set({ url })} />}
      {["FOCUS", "CLEAR_FIELD", "SUBMIT"].includes(String(data.nodeType)) && (
        <>
          <FindBy value={String(data.strategy || "CSS")} onChange={(strategy) => set({ strategy })} />
          <Text label="Selector if no element pin" value={String(data.xpath || "")} onChange={(xpath) => set({ xpath, query: xpath })} />
        </>
      )}
      {data.nodeType === "SET_WINDOW" && (
        <>
          <NumberField label="Width" value={Number(data.windowWidth || 1280)} onChange={(windowWidth) => set({ windowWidth })} />
          <NumberField label="Height" value={Number(data.windowHeight || 800)} onChange={(windowHeight) => set({ windowHeight })} />
        </>
      )}
      {["COUNT_ELEMENTS", "WAIT_GONE"].includes(String(data.nodeType)) && (
        <>
          <FindBy value={String(data.strategy || "CSS")} onChange={(strategy) => set({ strategy })} />
          <Text label="Selector" value={String(data.query || "")} onChange={(query) => set({ query, xpath: query })} />
        </>
      )}
      {data.nodeType === "PAGE_STORAGE" && (
        <>
          <Select label="Action" value={String(data.action || "GET")} options={["GET", "SET", "DELETE"]} onChange={(action) => set({ action })} />
          <Text label="Key" value={String(data.storageKey || "")} onChange={(storageKey) => set({ storageKey })} />
          {data.action === "SET" && <SiteValue label="Value" value={String(data.storageValue || "")} onChange={(storageValue) => set({ storageValue })} />}
        </>
      )}
      <InputUses data={data} columns={dataColumns} rows={rows} onChange={(inputUses) => set({ inputUses })} />
      {data.nodeType === "ASSERT" && (
        <>
          <Select label="Operator" value={String(data.operator || "NOT_EMPTY")} options={["NOT_EMPTY", "EQUALS", "CONTAINS", "MATCHES_REGEX"]} onChange={(operator) => set({ operator })} />
          {data.operator !== "NOT_EMPTY" && <Text label="Expected" value={String(data.expected || "")} onChange={(expected) => set({ expected })} />}
        </>
      )}
    </aside>
  );
}

const FIND_BY = [
  ["ROLE", "Role and name"],
  ["XPATH", "XPath"],
  ["CSS", "CSS"],
  ["CLASS", "Class"],
  ["ID", "ID"],
  ["NAME", "Name"],
  ["LINK_TEXT", "Link text"],
  ["PARTIAL_LINK_TEXT", "Partial link"],
  ["TAG", "Tag"],
  ["TEXT_CONTAINS", "Text contains"],
  ["REGEX_DOM", "Regex"],
];
let fieldColumns: DataColumn[] = [];

function Text({ label, value, onChange }: { label: string; value: string; onChange: (value: string) => void }) {
  return (
    <label className="field">
      {label}
      <input value={value} onChange={(event) => onChange(event.target.value)} />
    </label>
  );
}

function SiteValue({ label, value, onChange }: { label: string; value: string; onChange: (value: string) => void }) {
  return (
    <label className="field">
      {label}
      <input value={value} onChange={(event) => onChange(event.target.value)} />
      <DataPick onPick={onChange} />
    </label>
  );
}

function FindBy({ value, onChange }: { value: string; onChange: (value: string) => void }) {
  return (
    <label className="field">
      Find by
      <select value={value} onChange={(event) => onChange(event.target.value)}>
        {FIND_BY.map(([id, label]) => <option key={id} value={id}>{label}</option>)}
      </select>
    </label>
  );
}

function Area({ label, value, onChange }: { label: string; value: string; onChange: (value: string) => void }) {
  return (
    <label className="field">
      {label}
      <textarea value={value} onChange={(event) => onChange(event.target.value)} />
    </label>
  );
}

function NumberField({ label, value, onChange }: { label: string; value: number; onChange: (value: number) => void }) {
  return <label className="field">{label}<input type="number" value={value} onChange={(event) => onChange(Number(event.target.value))} /></label>;
}

function Select({ label, value, options, onChange }: { label: string; value: string; options: string[]; onChange: (value: string) => void }) {
  return (
    <label className="field">
      {label}
      <select value={value} onChange={(event) => onChange(event.target.value)}>
        {options.map((option) => <option key={option}>{option}</option>)}
      </select>
    </label>
  );
}

function DataPick({ onPick }: { onPick: (value: string) => void }) {
  if (!fieldColumns.length) return null;
  return (
    <select className="btn" value="" onChange={(event) => { if (event.target.value) onPick(event.target.value); }}>
      <option value="">Use an input</option>
      {fieldColumns.map((column) => <option key={column.name} value={`{{${column.name}}}`}>{`{{${column.name}}}`}</option>)}
    </select>
  );
}

function InputUses({ data, columns, rows, onChange }: { data: BlueprintData; columns: DataColumn[]; rows: Array<Record<string, string>>;   onChange: (uses: Record<string, { mode?: string; values?: string[] }>) => void }) {
  const siteText = [data.text, data.option].filter((value) => typeof value === "string").join(" ");
  const names = [...new Set(siteText.match(/\{\{\s*[\w.-]+\s*\}\}/g)?.map((token) => token.replace(/[{}\s]/g, "")) || [])];
  const uses = (data.inputUses || {}) as Record<string, { mode?: string; values?: string[] }>;
  if (!names.length) return null;
  return (
    <div className="mt-3 border-t border-line pt-3">
      {names.map((name) => {
        const spec = uses[name] || { mode: "one", values: [] };
        const pool = [...new Set(rows.map((row) => row[name]).filter(Boolean).concat(columns.find((column) => column.name === name)?.options || []))];
        return (
          <div key={name} className="mb-3">
            <p className="font-mono text-[11px] text-[#7f6df2]">{`{{${name}}}`}</p>
            <select className="btn mt-1 w-full" value={spec.mode || "one"} onChange={(event) => onChange({ ...uses, [name]: { mode: event.target.value, values: spec.values || [] } })}>
              <option value="one">One value I choose</option>
              <option value="random">One random value</option>
              <option value="random-selected">Random from the ones I tick</option>
              <option value="selected">Each ticked value</option>
              <option value="all">All values</option>
            </select>
            {spec.mode === "one" && (
              <select className="btn mt-1 w-full" value={(spec.values || [])[0] || ""} onChange={(event) => onChange({ ...uses, [name]: { mode: "one", values: event.target.value ? [event.target.value] : [] } })}>
                <option value="">Choose one</option>
                {pool.map((value) => <option key={value} value={value}>{value}</option>)}
              </select>
            )}
            {(spec.mode === "selected" || spec.mode === "random-selected") && pool.map((value) => (
              <label key={value} className="mt-1 flex items-center gap-2 text-xs">
                <input type="checkbox" checked={(spec.values || []).includes(value)} onChange={(event) => {
                  const current = spec.values || [];
                  const values = event.target.checked ? [...current, value] : current.filter((item) => item !== value);
                  onChange({ ...uses, [name]: { mode: spec.mode || "selected", values } });
                }} />
                {value}
              </label>
            ))}
          </div>
        );
      })}
    </div>
  );
}

function Check({ label, checked, onChange }: { label: string; checked: boolean; onChange: (value: boolean) => void }) {
  return <label className="mb-3 flex items-center gap-2 text-sm"><input type="checkbox" checked={checked} onChange={(event) => onChange(event.target.checked)} />{label}</label>;
}
