import { useEffect, useState } from "react";
import { api } from "../services/api";

type StudioSettings = {
  browser_slots: number;
  ready_ms: number;
  window_width: number;
  window_height: number;
  scale_note?: string;
  grid?: { ready: boolean; nodes: number; slots: number };
};

export function SettingsDesk() {
  const [form, setForm] = useState<StudioSettings | null>(null);
  const [saved, setSaved] = useState("");

  useEffect(() => {
    api.settings().then(setForm).catch(() => setSaved("Could not load settings"));
  }, []);

  if (!form) return <section className="panel min-h-0 flex-1 p-4 text-sm text-[#999]">Loading settings...</section>;

  function set<K extends keyof StudioSettings>(key: K, value: StudioSettings[K]) {
    setForm((current) => current ? { ...current, [key]: value } : current);
    setSaved("");
  }

  return (
    <section className="panel min-h-0 flex-1 overflow-auto p-4" data-testid="settings-desk">
      <h2 className="text-lg">Settings</h2>
      <p className="mb-4 text-sm text-[#999]">
        Grid has {form.grid?.nodes ?? 0} Chrome browsers and {form.grid?.slots ?? 0} sessions.
        Saving a number starts that many browsers. Zero keeps whatever is already up.
      </p>
      <div className="grid max-w-md gap-3">
        <label className="text-sm text-[#dcddde]">
          Browsers at once
          <input className="btn mt-1 w-full" type="number" min={0} max={16} value={form.browser_slots} onChange={(event) => set("browser_slots", Number(event.target.value))} />
        </label>
        <label className="text-sm text-[#dcddde]">
          Page ready wait (ms)
          <input className="btn mt-1 w-full" type="number" min={0} max={5000} value={form.ready_ms} onChange={(event) => set("ready_ms", Number(event.target.value))} />
        </label>
        <label className="text-sm text-[#dcddde]">
          Window width
          <input className="btn mt-1 w-full" type="number" min={320} max={1920} value={form.window_width} onChange={(event) => set("window_width", Number(event.target.value))} />
        </label>
        <label className="text-sm text-[#dcddde]">
          Window height
          <input className="btn mt-1 w-full" type="number" min={240} max={1200} value={form.window_height} onChange={(event) => set("window_height", Number(event.target.value))} />
        </label>
        <button className="btn btn-run w-fit" onClick={() => api.saveSettings(form).then((next) => { setForm(next); setSaved(next.scale_note || "Saved"); }).catch((err: Error) => setSaved(err.message))}>Save</button>
        {saved && <p className="text-sm text-[#999]">{saved}</p>}
      </div>
    </section>
  );
}
