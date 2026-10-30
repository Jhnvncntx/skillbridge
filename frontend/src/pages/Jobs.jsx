import { useCallback, useEffect, useState } from "react";
import { api, applyAndOpen } from "../api";

function Bar({ label, value, max }) {
  return (
    <div className="mb-1 text-xs">
      <div className="flex justify-between">
        <span>{label}</span>
        <span>
          {Math.round(value)} / {max}
        </span>
      </div>
      <div className="h-2 rounded bg-slate-200">
        <div
          className="h-2 rounded bg-crimson"
          style={{ width: `${(value / max) * 100}%` }}
        />
      </div>
    </div>
  );
}

export default function Jobs({ q, selectedId, onSelect, notify }) {
  const [list, setList] = useState(null);
  const [error, setError] = useState("");
  const [showDetail, setShowDetail] = useState(Boolean(selectedId));

  const load = useCallback(() => {
    api
      .jobs(q)
      .then(setList)
      .catch((e) => setError(e.message));
  }, [q]);
  useEffect(() => {
    setList(null);
    load();
  }, [load]);

  useEffect(() => {
    setShowDetail(Boolean(selectedId));
  }, [q, selectedId]);

  const current = list
    ? list.find((m) => m.job.id === selectedId) || list[0]
    : null;

  const open = (id) => {
    onSelect(id);
    setShowDetail(true);
    window.scrollTo(0, 0);
  };
  const apply = async (job) => {
    const r = await applyAndOpen(job);
    notify(r.message, r.ok);
    load();
  };
  const toggleSave = async (job) => {
    try {
      await (job.saved ? api.unsave(job.id) : api.save(job.id));
      load();
    } catch (e) {
      notify(e.message, false);
    }
  };

  return (
    <div>
      <h2 className="mb-3 break-words text-lg font-extrabold uppercase text-white">
        {q ? <>Results for “{q}”</> : "All jobs, best match first"}{" "}
        {list && <span className="text-pink-200">({list.length})</span>}
      </h2>
      {error && (
        <p className="mb-3 rounded bg-red-100 p-2 text-sm text-red-700">
          {error}
        </p>
      )}
      {!list && !error && <p className="text-white">Loading jobs…</p>}
      {list && list.length === 0 && (
        <p className="rounded-2xl bg-white/10 p-4 text-pink-100">
          No jobs found{q ? " for that search" : " yet"}.
        </p>
      )}

      {list && list.length > 0 && (
        <div className="flex flex-col gap-4 md:flex-row">
          <div
            data-testid="job-list"
            className={`${showDetail ? "hidden" : "block"} space-y-2 rounded-3xl bg-pink-100 p-3 md:block md:max-h-[75vh] md:w-2/5 md:overflow-y-auto`}
          >
            {list.map((m) => (
              <button
                key={m.job.id}
                onClick={() => open(m.job.id)}
                className={`block w-full rounded-2xl bg-white p-3 text-left shadow-sm ${current?.job.id === m.job.id ? "md:ring-2 md:ring-crimson" : ""}`}
              >
                <div className="flex justify-between gap-2">
                  <span className="break-words font-bold text-maroon-900">
                    {m.job.title}
                  </span>
                  <span className="shrink-0 text-xs font-bold text-crimson">
                    {Math.round(m.score)}
                  </span>
                </div>
                <div className="text-sm text-slate-700">
                  {m.job.company} · {m.job.location}
                </div>
                <div className="clamp-2 text-xs text-slate-500">
                  {m.job.description}
                </div>
              </button>
            ))}
          </div>

          {current && (
            <div
              data-testid="job-detail"
              className={`${showDetail ? "block" : "hidden"} rounded-3xl bg-orange-100 p-4 sm:p-5 md:block md:sticky md:top-4 md:max-h-[75vh] md:w-3/5 md:self-start md:overflow-y-auto`}
            >
              <button
                onClick={() => setShowDetail(false)}
                className="mb-3 py-1 text-sm font-bold text-maroon-800 md:hidden"
              >
                ← Back to list
              </button>
              <h3 className="break-words text-2xl font-extrabold text-maroon-900">
                {current.job.title}
              </h3>
              <p className="font-semibold">{current.job.company}</p>
              <p className="text-sm text-slate-600">{current.job.location}</p>
              {current.job.licensure && (
                <p className="mt-2 rounded bg-amber-200 px-2 py-1 text-sm font-semibold text-amber-900">
                  {current.job.licensure}
                </p>
              )}
              <p className="mt-3 break-words text-sm">
                {current.job.description}
              </p>

              <div className="mt-4 rounded-xl bg-white p-3">
                <p className="mb-2 text-sm font-bold">
                  Your match: {Math.round(current.score)} / 100
                </p>
                <Bar
                  label="Target job role"
                  value={current.roleScore}
                  max={50}
                />
                <Bar label="Hard skills" value={current.skillScore} max={30} />
                <Bar
                  label="Degree program"
                  value={current.programScore}
                  max={20}
                />
              </div>

              <div className="mt-3 text-sm">
                <p className="font-bold">Required skills</p>
                <div className="mt-1 flex flex-wrap gap-1">
                  {current.matchedSkills.map((s) => (
                    <span
                      key={s}
                      className="rounded-full bg-emerald-100 px-2 py-0.5 text-emerald-800"
                    >
                      ✓ {s}
                    </span>
                  ))}
                  {current.missingSkills.map((s) => (
                    <span
                      key={s}
                      className="rounded-full bg-white px-2 py-0.5 text-slate-600"
                    >
                      needs {s}
                    </span>
                  ))}
                </div>
              </div>

              <p className="mt-3 text-xs text-slate-500">
                Source: {current.job.source} · collected{" "}
                {current.job.dateCollected}
              </p>
              {/* on phones this bar stays visible above the tab bar while you scroll the details */}
              <div className="sticky bottom-[4.5rem] -mx-4 mt-4 flex flex-wrap gap-3 bg-orange-100 px-4 py-3 sm:-mx-5 sm:px-5 md:static md:mx-0 md:px-0 md:py-0">
                <button
                  onClick={() => apply(current.job)}
                  className="btn-primary px-8"
                >
                  {current.job.applied ? "APPLIED ✓ OPEN AGAIN" : "APPLY NOW"}
                </button>
                <button
                  onClick={() => toggleSave(current.job)}
                  className="btn-ghost border border-maroon-800"
                >
                  {current.job.saved ? "★ Saved" : "☆ Save job"}
                </button>
              </div>
              <p className="mt-2 text-xs text-slate-500">
                Apply opens the original posting and records it in My
                Applications.
              </p>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
