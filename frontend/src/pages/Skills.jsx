import { useEffect, useState } from "react";
import { api } from "../api";

export default function Skills({ user, onSaved }) {
  const [skills, setSkills] = useState(user.skills);
  const [known, setKnown] = useState(new Set());
  const [index, setIndex] = useState([]);
  const [text, setText] = useState("");
  const [msg, setMsg] = useState("");

  useEffect(() => { api.skillIndex().then((s) => { setIndex(s); setKnown(new Set(s)); }).catch(() => {}); }, []);

  const add = (e) => {
    e.preventDefault();
    const s = text.trim().toLowerCase().replace(/\s+/g, " ");
    if (s && !skills.includes(s)) setSkills([...skills, s]);
    setText(""); setMsg("");
  };

  const save = async () => {
    try { onSaved(await api.setSkills(user.id, skills)); setMsg("Saved! Check your Recommended Jobs."); }
    catch (err) { setMsg(err.message); }
  };

  return (
    <div className="rounded-xl bg-white p-5 shadow-sm">
      <h2 className="mb-1 text-xl font-bold">My Skills</h2>
      <p className="mb-4 text-sm text-slate-600">Add your technical and soft skills. Green skills are required by at least one job.</p>
      <form onSubmit={add} className="mb-4 flex gap-2">
        <input list="skill-index" value={text} onChange={(e) => setText(e.target.value)}
          placeholder="e.g. java, communication…" className="flex-1 rounded-lg border border-slate-300 px-3 py-2" />
        <datalist id="skill-index">{index.map((s) => <option key={s} value={s} />)}</datalist>
        <button className="rounded-lg bg-slate-800 px-4 py-2 text-white">Add</button>
      </form>
      <div className="mb-4 flex flex-wrap gap-2">
        {skills.length === 0 && <span className="text-sm text-slate-500">No skills yet.</span>}
        {skills.map((s) => (
          <span key={s} className={`flex items-center gap-1 rounded-full px-3 py-1 text-sm ${known.has(s) ? "bg-emerald-100 text-emerald-800" : "bg-slate-200 text-slate-700"}`}>
            {s}
            <button onClick={() => setSkills(skills.filter((x) => x !== s))} className="font-bold" aria-label={`Remove ${s}`}>×</button>
          </span>
        ))}
      </div>
      <button onClick={save} className="rounded-lg bg-indigo-700 px-5 py-2 font-semibold text-white">Save skills</button>
      {msg && <p className="mt-3 text-sm text-slate-700">{msg}</p>}
    </div>
  );
}
