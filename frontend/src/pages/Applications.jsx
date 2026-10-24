import { useCallback, useEffect, useState } from "react";
import { api } from "../api";

export default function Applications({ notify }) {
  const [apps, setApps] = useState(null);
  const load = useCallback(() => { api.applications().then(setApps).catch((e) => notify(e.message, false)); }, [notify]);
  useEffect(load, [load]);

  const undo = async () => {
    try { await api.undoLast(); notify("Last application removed.", true); load(); }
    catch (e) { notify(e.message, false); }
  };

  return (
    <div className="rounded-3xl bg-white p-5 shadow">
      <div className="mb-2 flex items-center justify-between">
        <h2 className="text-xl font-extrabold text-maroon-900">My Applications</h2>
        <button onClick={undo} disabled={!apps?.length} className="btn-primary">Undo last</button>
      </div>
      <p className="mb-3 text-sm text-slate-600">Paperless: every application is recorded digitally. “Undo last” removes the most recent one.</p>
      {!apps ? <p>Loading…</p> : apps.length === 0 ? <p className="text-slate-600">You haven't applied to any jobs yet.</p> : (
        <ul className="divide-y">
          {[...apps].reverse().map((a) => (
            <li key={a.id} className="py-3">
              <div className="font-semibold">{a.jobTitle}</div>
              <div className="text-sm text-slate-600">{a.company} · {new Date(a.appliedAt).toLocaleString()}</div>
              <a href={a.link} target="_blank" rel="noopener noreferrer" className="text-sm font-semibold text-crimson underline">Open original posting</a>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
