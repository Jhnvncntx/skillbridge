/** Compact job card used on the Home and Saved pages. */
export default function JobCard({ job, score, onOpen, onApply, busy }) {
  return (
    <div className="flex flex-col justify-between rounded-2xl bg-white p-4 shadow">
      <button onClick={onOpen} className="text-left">
        <div className="flex items-start justify-between gap-2">
          <h3 className="font-bold leading-tight text-maroon-900">{job.title}</h3>
          {score !== undefined && (
            <span className="shrink-0 rounded-full bg-maroon-800 px-2 py-0.5 text-xs font-bold text-gold">{Math.round(score)}</span>
          )}
        </div>
        <p className="mt-1 text-sm text-slate-700">{job.company}</p>
        <p className="text-xs text-slate-500">{job.location}</p>
      </button>
      <div className="mt-3 text-right">
        <button onClick={onApply} disabled={busy} className="text-sm font-bold text-crimson hover:underline">
          {job.applied ? "APPLIED ✓ (open again)" : "APPLY NOW!"}
        </button>
      </div>
    </div>
  );
}
