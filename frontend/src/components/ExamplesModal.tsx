type Props = {
  open: boolean;
  rows: Array<Record<string, string>>;
  onClose: () => void;
  onChange: (rows: Array<Record<string, string>>) => void;
};

export function ExamplesModal({ open, rows, onClose, onChange }: Props) {
  if (!open) return null;
  const keys = Array.from(new Set(rows.flatMap((row) => Object.keys(row))));
  const columns = keys.length ? keys : ["value"];

  function update(index: number, key: string, value: string) {
    const next = rows.map((row, rowIndex) => (rowIndex === index ? { ...row, [key]: value } : row));
    onChange(next);
  }

  return (
    <div className="fixed inset-0 z-20 grid place-items-center bg-black/60 p-6" onClick={onClose}>
      <div className="panel w-full max-w-2xl rounded-lg border border-line p-5" onClick={(event) => event.stopPropagation()}>
        <div className="mb-3 flex items-center justify-between">
          <div>
            <h2 className="text-lg">Examples</h2>
            <p className="text-xs text-[#8ea6bb]">One run per row. Use the column name in a step as {"{{name}}"}.</p>
          </div>
          <button className="btn" onClick={onClose}>Close</button>
        </div>
        <div className="mb-3 max-h-72 overflow-auto">
          <table className="w-full text-left text-sm">
            <thead>
              <tr>
                {columns.map((key) => <th key={key} className="px-1 py-1 font-mono text-[10px] text-[#8ea6bb]">{key}</th>)}
              </tr>
            </thead>
            <tbody>
              {rows.map((row, index) => (
                <tr key={index}>
                  {columns.map((key) => (
                    <td key={key} className="p-1">
                      <input className="btn w-full" value={row[key] || ""} onChange={(event) => update(index, key, event.target.value)} />
                    </td>
                  ))}
                </tr>
              ))}
            </tbody>
          </table>
          {rows.length === 0 && <p className="text-sm text-[#8ea6bb]">No rows. The blueprint runs once.</p>}
        </div>
        <div className="flex gap-2">
          <button className="btn" onClick={() => onChange([...rows, Object.fromEntries(columns.map((key) => [key, ""]))])}>Add row</button>
          <button className="btn" onClick={() => {
            const name = window.prompt("Column name", "value");
            if (!name) return;
            onChange((rows.length ? rows : [{}]).map((row) => ({ ...row, [name]: row[name] || "" })));
          }}>Add column</button>
        </div>
      </div>
    </div>
  );
}
