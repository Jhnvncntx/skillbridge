import { useEffect, useState } from "react";
import { api } from "../api";

export default function PostJob() {
  const [programs, setPrograms] = useState([]);
  const [form, setForm] = useState({ title: "", company: "", skills: "" });
  const [selected, setSelected] = useState([]);
  const [msg, setMsg] = useState({ type: "", text: "" });
  const [busy, setBusy] = useState(false);

  useEffect(() => { api.programs().then(setPrograms).catch((e) => setMsg({ type: "error", text: e.message })); }, []);
  const colleges = [...new Set(programs.map((p) => p.college))];
  const toggle = (c) => setSelected(selected.includes(c) ? selected.filter((x) => x !== c) : [...selected, c]);

  const submit = async (e) => {
    e.preventDefault();
    setBusy(true); setMsg({ type: "", text: "" });
    try {
      const job = await api.postJob({
        title: form.title, company: form.company, targetPrograms: selected,
        requiredSkills: form.skills.split(","),
      });
      setMsg({ type: "ok", text: `Posted “${job.title}” (${job.id}). Matching students will now see it.` });
      setForm({ title: "", company: "", skills: "" }); setSelected([]);
    } catch (err) { setMsg({ type: "error", text: err.message }); }
    finally { setBusy(false); }
  };

  const field = "w-full rounded-lg border border-slate-300 bg-white px-3 py-2";
  return (
    <div className="mx-auto max-w-2xl">
      <h2 className="mb-1 text-2xl font-bold">Post a job</h2>
      <p className="mb-4 text-sm text-slate-600">Tag the exact programs and skills you need so the right graduates find you.</p>
      <form onSubmit={submit} className="space-y-3 rounded-xl bg-white p-5 shadow-sm">
        <input className={field} placeholder="Job title" value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} required />
        <input className={field} placeholder="Company" value={form.company} onChange={(e) => setForm({ ...form, company: e.target.value })} required />
        <input className={field} placeholder="Required skills, separated by commas (e.g. java, sql, communication)"
          value={form.skills} onChange={(e) => setForm({ ...form, skills: e.target.value })} required />
        <div>
          <p className="mb-1 text-sm font-medium">Target programs ({selected.length} selected)</p>
          <div className="max-h-64 space-y-2 overflow-y-auto rounded-lg border border-slate-200 p-3">
            {colleges.map((c) => (
              <div key={c}>
                <div className="text-xs font-semibold uppercase text-slate-500">{c}</div>
                {programs.filter((p) => p.college === c).map((p) => (
                  <label key={p.code} className="flex items-center gap-2 text-sm">
                    <input type="checkbox" checked={selected.includes(p.code)} onChange={() => toggle(p.code)} /> {p.name}
                  </label>
                ))}
              </div>
            ))}
          </div>
        </div>
        {msg.text && <p className={`text-sm ${msg.type === "ok" ? "text-emerald-700" : "text-red-600"}`}>{msg.text}</p>}
        <button disabled={busy} className="w-full rounded-lg bg-indigo-700 py-2 font-semibold text-white disabled:opacity-60">
          {busy ? "Posting…" : "Post job"}
        </button>
      </form>
    </div>
  );
}
