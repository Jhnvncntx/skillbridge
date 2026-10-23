import { useCallback, useEffect, useState } from "react";
import { api } from "../api";

export default function Applications({ user }) {
  const [apps, setApps] = useState(null);
  const [error, setError] = useState("");

  const load = useCallback(() => {
    api.applications(user.id).then(setApps).catch((e) => setError(e.message));
  }, [user.id]);
  useEffect(load, [load]);

  const undo = async () => { try { await api.undoLast(user.id); setError(""); load(); } catch (e) { setError(e.message); } };

  return (
    <div className="rounded-xl bg-white p-5 shadow-sm">
      <div className="mb-3 flex items-center justify-between">
        <h2 className="text-xl font-bold">My Applications</h2>
        <button onClick={undo} disabled={!apps?.length}
          className="rounded-lg bg-slate-800 px-3 py-1 text-sm text-white disabled:opacity-40">Undo last</button>
      </div>
      <p className="mb-3 text-sm text-slate-600">Paperless: applications are recorded digitally. Undo removes the most recent one.</p>
      {error && <p className="mb-2 text-sm text-red-600">{error}</p>}
      {apps === null ? <p>Loading…</p> : apps.length === 0 ? <p className="text-slate-600">You haven't applied to any jobs yet.</p> : (
        <ul className="divide-y">
          {[...apps].reverse().map((a) => (
            <li key={a.id} className="py-3">
              <div className="font-semibold">{a.jobTitle}</div>
              <div className="text-sm text-slate-600">{a.company} · {new Date(a.appliedAt).toLocaleString()}</div>
              {a.note && <div className="mt-1 text-sm italic text-slate-500">“{a.note}”</div>}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
