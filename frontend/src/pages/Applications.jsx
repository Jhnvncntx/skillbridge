import { useCallback, useEffect, useState } from "react";
import { api } from "../api";

// Top-level on purpose: a component declared inside another would be rebuilt on every render.
function ApplicationItem({ a, onRemove }) {
  const [confirming, setConfirming] = useState(false);
  const [busy, setBusy] = useState(false);

  const remove = async () => { setBusy(true); await onRemove(a.id); setBusy(false); setConfirming(false); };

  return (
    <li className="py-3">
      <div className="break-words font-semibold">{a.jobTitle}</div>
      <div className="text-sm text-slate-600">{a.company} · {new Date(a.appliedAt).toLocaleString()}</div>
      <div className="mt-1 flex flex-wrap items-center gap-x-4 gap-y-1">
        <a href={a.link} target="_blank" rel="noopener noreferrer" className="py-1 text-sm font-semibold text-crimson underline">Open original posting</a>
        {!confirming ? (
          <button onClick={() => setConfirming(true)} className="py-1 text-sm font-semibold text-slate-600 underline">Remove from my list</button>
        ) : (
          <span className="flex flex-wrap items-center gap-2 text-sm">
            <span className="text-slate-700">Remove this one?</span>
            <button onClick={remove} disabled={busy} className="rounded bg-red-700 px-3 py-1 font-semibold text-white disabled:opacity-50">{busy ? "Removing…" : "Yes, remove"}</button>
            <button onClick={() => setConfirming(false)} className="rounded border border-slate-300 px-3 py-1 font-semibold">Cancel</button>
          </span>
        )}
      </div>
    </li>
  );
}

export default function Applications({ notify }) {
  const [apps, setApps] = useState(null);
  const load = useCallback(() => { api.applications().then(setApps).catch((e) => notify(e.message, false)); }, [notify]);
  useEffect(load, [load]);

  const undo = async () => {
    try { await api.undoLast(); notify("Last application removed.", true); load(); }
    catch (e) { notify(e.message, false); }
  };
  const removeOne = async (id) => {
    try { await api.removeApplication(id); notify("Application removed from your list.", true); load(); }
    catch (e) { notify(e.message, false); }
  };

  return (
    <div className="rounded-3xl bg-white p-4 shadow sm:p-5">
      <div className="mb-2 flex flex-wrap items-center justify-between gap-2">
        <h2 className="text-xl font-extrabold text-maroon-900">My Applications</h2>
        <button onClick={undo} disabled={!apps?.length} className="btn-primary">Undo last</button>
      </div>
      <p className="mb-3 text-sm text-slate-600">
        Paperless: every application is recorded digitally. “Undo last” removes the most recent one, and “Remove from my list” removes any one.
        This only changes your SkillBridge list. It does not withdraw anything you sent on JobStreet or Indeed.
      </p>
      {!apps ? <p>Loading…</p> : apps.length === 0 ? <p className="text-slate-600">You haven't applied to any jobs yet.</p> : (
        <ul className="divide-y">
          {[...apps].reverse().map((a) => <ApplicationItem key={a.id} a={a} onRemove={removeOne} />)}
        </ul>
      )}
    </div>
  );
}
