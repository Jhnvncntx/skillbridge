import { useCallback, useEffect, useState } from "react";
import { api, applyAndOpen } from "../api";
import JobCard from "../components/JobCard";

export default function Saved({ go, notify }) {
  const [jobs, setJobs] = useState(null);
  const load = useCallback(() => { api.saved().then(setJobs).catch((e) => notify(e.message, false)); }, [notify]);
  useEffect(load, [load]);

  const apply = async (job) => { const r = await applyAndOpen(job); notify(r.message, r.ok); load(); };
  const remove = async (job) => { await api.unsave(job.id); load(); };

  return (
    <div>
      <h2 className="mb-3 text-lg font-extrabold uppercase text-white">Saved jobs</h2>
      {!jobs ? <p className="text-white">Loading…</p> : jobs.length === 0 ? (
        <p className="rounded-2xl bg-white/10 p-4 text-pink-100">No saved jobs yet. Use “Save job” on the Jobs page.</p>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {jobs.map((j) => (
            <div key={j.id}>
              <JobCard job={j} onOpen={() => go("jobs", { jobId: j.id })} onApply={() => apply(j)} />
              <button onClick={() => remove(j)} className="mt-1 text-xs text-pink-200 underline">Remove from saved</button>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
