import { useState } from "react";
import type { PickResult } from "../services/api";

type Props = {
  open: boolean;
  busy: boolean;
  result: PickResult | null;
  error: string;
  onClose: () => void;
  onChoose: (strategy: string, query: string) => void;
};

export function PickerModal({ open, busy, result, error, onClose, onChoose }: Props) {
  const [choice, setChoice] = useState<number | null>(null);
  if (!open) return null;
  const viewport = result?.viewport || {};
  const width = viewport.width || 1280;
  const height = viewport.height || 800;
  const selected = choice == null ? null : result?.elements[choice];
  return (
    <div className="fixed inset-0 z-30 grid place-items-center bg-black/70 p-4" onClick={onClose}>
      <div className="panel flex max-h-[90vh] w-full max-w-5xl flex-col rounded-lg border border-line" onClick={(event) => event.stopPropagation()}>
        <div className="flex items-center justify-between border-b border-line px-4 py-3">
          <div>
            <h2 className="text-lg">Pick element</h2>
            <p className="text-xs text-[#8ea6bb]">Click a box, then choose the selector to write into this step.</p>
          </div>
          <button className="btn" onClick={onClose}>Close</button>
        </div>
        <div className="grid min-h-0 flex-1 grid-cols-[1fr_280px]">
          <div className="overflow-auto bg-[#070d14] p-3">
            {busy && <p className="text-sm text-[#8ea6bb]">Opening the page on the grid...</p>}
            {error && <p className="text-sm text-[#ff8fa3]">{error}</p>}
            {result?.screenshot_base64 && (
              <div className="relative inline-block">
                <img alt="Page" className="max-w-full" src={`data:image/png;base64,${result.screenshot_base64}`} />
                {result.elements.map((element, index) => (
                  <button
                    key={`${element.tag}-${index}`}
                    className={`absolute border ${choice === index ? "border-[#49c2ff] bg-[#49c2ff]/20" : "border-[#3ddc97]/80"}`}
                    style={{
                      left: `${(element.x / width) * 100}%`,
                      top: `${(element.y / height) * 100}%`,
                      width: `${(element.width / width) * 100}%`,
                      height: `${(element.height / height) * 100}%`,
                    }}
                    onClick={() => setChoice(index)}
                    title={element.text || element.tag}
                  />
                ))}
              </div>
            )}
          </div>
          <div className="overflow-auto border-l border-line p-3 text-sm">
            {!selected && <p className="text-[#8ea6bb]">No element selected.</p>}
            {selected && (
              <>
                <p className="mb-2 font-mono text-xs text-[#9ad8ff]">{selected.tag} {selected.text}</p>
                {selected.selectors.length === 0 && <p className="text-[#ff8fa3]">No stable selector was derived.</p>}
                {selected.selectors.map((selector) => (
                  <button key={selector.value} className="btn mb-2 block w-full text-left" onClick={() => onChoose(selector.kind, selector.value)}>
                    <span className="block text-[10px] text-[#8ea6bb]">{selector.kind}</span>
                    {selector.value}
                  </button>
                ))}
              </>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
