import { useCallback, useEffect, useState } from "react";
import { api } from "../api";

function Chip({ children, className }) {
  return <span className={`rounded-full px-2 py-0.5 text-xs ${className}`}>{children}</span>;
}

export default function Jobs({ user }) {
  const [mode, setMode] = useState("top");
  const [jobs, setJobs] = useState(null);
  const [applied, setApplied] = useState([]);
  const [error, setError] = useState("");

  const load = useCallback(async () => {
    setError("");
    try {
      const [j, a] = await Promise.all([
        mode === "top" ? api.recommendations(user.id, 5) : api.ranked(user.id),
        api.applications(user.id),
      ]);
      setJobs(j); setApplied(a);
    } catch (e) { setError(e.message); }
  }, [mode, user.id, user.skills]);

  useEffect(() => { setJobs(null); load(); }, [load]);

  const apply = async (jobId) => { try { await api.apply(user.id, jobId); load(); } catch (e) { setError(e.message); } };
  const undo = async () => { try { await api.undoLast(user.id); load(); } catch (e) { setError(e.message); } };
  const appliedIds = new Set(applied.map((a) => a.jobId));

  return (
    <div>
      <div className="mb-4 flex flex-wrap items-center justify-between gap-2">
        <div className="flex gap-2 text-sm">
          {[["top", "Top 5"], ["all", "All matches"]].map(([k, l]) => (
            <button key={k} onClick={() => setMode(k)}
              className={`rounded-lg px-3 py-1 ${mode === k ? "bg-slate-800 text-white" : "bg-white shadow-sm"}`}>{l}</button>
          ))}
        </div>
        <button onClick={undo} disabled={applied.length === 0}
          className="rounded-lg bg-white px-3 py-1 text-sm shadow-sm disabled:opacity-40">
          Undo last application ({applied.length})
        </button>
      </div>

      {error && <p className="mb-3 text-sm text-red-600">{error}</p>}
      {jobs === null && !error && <p>Finding your matches…</p>}
      {jobs?.length === 0 && <p className="text-slate-600">No matches yet. Add skills in “My Skills”.</p>}

      <div className="space-y-3">
        {jobs?.map((j) => (
          <div key={j.jobId} className="rounded-xl bg-white p-4 shadow-sm">
            <div className="flex items-start justify-between gap-3">
              <div>
                <h3 className="font-semibold">{j.title}</h3>
                <p className="text-sm text-slate-600">{j.company}</p>
              </div>
              <div className="text-right">
                <div className="text-2xl font-bold text-indigo-700">{Math.round(j.score)}</div>
                <div className="text-xs text-slate-500">match score</div>
              </div>
            </div>
            <div className="mt-2 flex flex-wrap gap-1">
              {j.programMatch && <Chip className="bg-indigo-100 text-indigo-800">Matches your program</Chip>}
              {j.matchedSkills.map((s) => <Chip key={s} className="bg-emerald-100 text-emerald-800">✓ {s}</Chip>)}
              {j.missingSkills.map((s) => <Chip key={s} className="bg-slate-100 text-slate-600">needs {s}</Chip>)}
            </div>
            <button onClick={() => apply(j.jobId)} disabled={appliedIds.has(j.jobId)}
              className="mt-3 rounded-lg bg-indigo-700 px-4 py-1.5 text-sm font-semibold text-white disabled:bg-emerald-600">
              {appliedIds.has(j.jobId) ? "Applied ✓" : "Apply"}
            </button>
          </div>
        ))}
      </div>
    </div>
  );
}
