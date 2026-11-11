export type DataColumn = {
  name: string;
  type: "text" | "long" | "number" | "url" | "boolean" | "choice";
  options: string[];
};

type Props = {
  columns: DataColumn[];
  rows: Array<Record<string, string>>;
  onChange: (columns: DataColumn[], rows: Array<Record<string, string>>) => void;
};

const types: Array<{ id: DataColumn["type"]; label: string }> = [
  { id: "text", label: "Text" },
  { id: "long", label: "Long text" },
  { id: "number", label: "Number" },
  { id: "url", label: "URL" },
  { id: "boolean", label: "Yes / no" },
  { id: "choice", label: "Choice list" },
];

export function TestDataDesk({ columns, rows, onChange }: Props) {
  function commit(nextColumns: DataColumn[], nextRows = rows) {
    const names = nextColumns.map((column) => column.name);
    onChange(nextColumns, nextRows.map((row) => Object.fromEntries(names.map((name) => [name, row[name] || ""]))));
  }

  function addColumn() {
    const name = uniqueName(columns, window.prompt("Placeholder name", "city")?.trim() || "");
    if (!name) return;
    commit([...columns, { name, type: "text", options: [] }]);
  }

  function rename(from: string, to: string) {
    const name = uniqueName(columns.filter((column) => column.name !== from), to.trim());
    if (!name) return;
    commit(
      columns.map((column) => column.name === from ? { ...column, name } : column),
      rows.map((row) => {
        const next = { ...row, [name]: row[from] || "" };
        delete next[from];
        return next;
      }),
    );
  }

  function addRow(random = false) {
    if (!columns.length) return;
    const row = Object.fromEntries(columns.map((column) => [column.name, random ? sample(column) : ""]));
    commit(columns, [...rows, row]);
  }

  return (
    <section className="panel min-h-0 flex-1 overflow-auto p-4" data-testid="test-data-desk">
      <div className="mb-4 flex items-center justify-between gap-3">
        <div>
          <h2 className="text-lg">Inputs</h2>
          <p className="text-sm text-[#999]">These values do not run by themselves. A step chooses all, one random, or selected values.</p>
        </div>
        <div className="flex gap-2">
          <button className="btn" onClick={addColumn}>Add input</button>
          <button className="btn" disabled={!columns.length} onClick={() => addRow(false)}>Add row</button>
          <button className="btn" disabled={!columns.length} onClick={() => addRow(true)}>Add random row</button>
        </div>
      </div>
      {columns.length === 0 && <p className="text-sm text-[#999]">No inputs yet. Name one, then add its values.</p>}
      {columns.map((column) => (
        <article key={column.name} className="mb-4 border border-line p-3">
          <div className="mb-2 flex flex-wrap items-center gap-2">
            <input className="btn w-40" defaultValue={column.name} onBlur={(event) => rename(column.name, event.target.value)} />
            <span className="font-mono text-[11px] text-[#7f6df2]">{`{{${column.name}}}`}</span>
            <select className="btn" value={column.type} onChange={(event) => commit(columns.map((item) => item.name === column.name ? { ...item, type: event.target.value as DataColumn["type"] } : item))}>
              {types.map((type) => <option key={type.id} value={type.id}>{type.label}</option>)}
            </select>
            <button className="btn btn-danger" onClick={() => commit(columns.filter((item) => item.name !== column.name))}>Delete input</button>
          </div>
          {column.type === "choice" && (
            <input
              className="btn mb-2 w-full"
              placeholder="Choices, separated by commas"
              value={column.options.join(", ")}
              onChange={(event) => commit(columns.map((item) => item.name === column.name ? { ...item, options: splitList(event.target.value) } : item))}
            />
          )}
          <ul className="space-y-1">
            {rows.map((row, index) => (
              <li key={index} className="flex gap-2">
                <ValueEditor column={column} value={row[column.name] || ""} onChange={(value) => commit(columns, rows.map((item, rowIndex) => rowIndex === index ? { ...item, [column.name]: value } : item))} />
                <button className="btn btn-danger" onClick={() => commit(columns, rows.filter((_, rowIndex) => rowIndex !== index))}>Delete row {index + 1}</button>
              </li>
            ))}
            {rows.length === 0 && <li className="text-sm text-[#999]">No values yet.</li>}
          </ul>
        </article>
      ))}
    </section>
  );
}

function ValueEditor({ column, value, onChange }: { column: DataColumn; value: string; onChange: (value: string) => void }) {
  if (column.type === "long") return <textarea className="btn min-h-20 flex-1" value={value} onChange={(event) => onChange(event.target.value)} />;
  if (column.type === "boolean") {
    return (
      <select className="btn flex-1" value={value || "false"} onChange={(event) => onChange(event.target.value)}>
        <option value="true">yes</option>
        <option value="false">no</option>
      </select>
    );
  }
  if (column.type === "choice") {
    return (
      <select className="btn flex-1" value={value} onChange={(event) => onChange(event.target.value)}>
        <option value="">Pick</option>
        {column.options.map((option) => <option key={option} value={option}>{option}</option>)}
      </select>
    );
  }
  return <input className="btn flex-1" type={column.type === "number" ? "number" : "text"} value={value} onChange={(event) => onChange(event.target.value)} />;
}

function uniqueName(columns: DataColumn[], raw: string) {
  const name = raw.replace(/[^\w.-]/g, "");
  if (!name || columns.some((column) => column.name === name)) return "";
  return name;
}

function splitList(value: string) {
  return value.split(",").map((part) => part.trim()).filter(Boolean);
}

function sample(column: DataColumn) {
  if (column.type === "choice") return column.options[Math.floor(Math.random() * column.options.length)] || "";
  if (column.type === "number") return String(Math.floor(Math.random() * 100) + 1);
  if (column.type === "boolean") return Math.random() > 0.5 ? "true" : "false";
  if (column.type === "url") return "https://example.com";
  if (column.type === "long") return "A longer sample note.";
  return `sample-${Math.floor(Math.random() * 1000)}`;
}
