import { useState } from "react";
import { api } from "../api";

export default function Admin({ onBack }) {
  const [pass, setPass] = useState("");
  const [stats, setStats] = useState(null);
  const [error, setError] = useState("");
  const [gapsOnly, setGapsOnly] = useState(false);

  const load = async (e) => {
    e?.preventDefault();
    setError("");
    try {
      setStats(await api.adminStats(pass));
    } catch (err) {
      setStats(null);
      setError(err.message);
    }
  };

  if (!stats) {
    return (
      <form
        onSubmit={load}
        className="mx-auto mt-10 max-w-sm space-y-3 rounded-3xl bg-white p-6 shadow-xl"
      >
        <h2 className="text-xl font-extrabold text-maroon-900">
          Admin dashboard
        </h2>
        <input
          className="field"
          type="password"
          placeholder="Admin passcode"
          value={pass}
          onChange={(e) => setPass(e.target.value)}
        />
        {error && <p className="text-sm text-red-600">{error}</p>}
        <div className="flex justify-between">
          <button
            type="button"
            onClick={onBack}
            className="btn-ghost border border-slate-300"
          >
            Back
          </button>
          <button className="btn-primary">Open</button>
        </div>
      </form>
    );
  }

  const Card = ({ label, value }) => (
    <div className="rounded-2xl bg-white p-4 text-center shadow">
      <div className="text-3xl font-extrabold text-crimson">{value}</div>
      <div className="text-sm text-slate-600">{label}</div>
    </div>
  );
  const rows = stats.programs.filter((p) => !gapsOnly || p.gap);
  const maxDemand = Math.max(1, ...stats.topSkills.map((s) => s.jobCount));
  const r = stats.dataReport;

  return (
    <div className="space-y-5 text-slate-800">
      <div className="flex items-center justify-between">
        <h2 className="text-2xl font-extrabold text-white">
          Employability statistics by program
        </h2>
        <div className="flex gap-2">
          <button onClick={load} className="btn-ghost">
            Refresh
          </button>
          <button onClick={onBack} className="btn-ghost">
            Exit
          </button>
        </div>
      </div>
      <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
        <Card label="Registered students" value={stats.totalUsers} />
        <Card label="Job postings" value={stats.totalJobs} />
        <Card label="Applications" value={stats.totalApplications} />
        <Card
          label="Programs with jobs"
          value={`${stats.programsWithJobs} / ${stats.programsTotal}`}
        />
      </div>

      <div className="rounded-2xl bg-white p-4 shadow">
        <div className="mb-2 flex items-center justify-between">
          <h3 className="font-bold">Programs</h3>
          <label className="text-sm">
            <input
              type="checkbox"
              checked={gapsOnly}
              onChange={(e) => setGapsOnly(e.target.checked)}
            />{" "}
            Show coverage gaps only
          </label>
        </div>
        <div className="max-h-96 overflow-auto">
          <table className="w-full text-left text-sm">
            <thead className="sticky top-0 bg-white text-slate-500">
              <tr>
                <th className="py-1">Program</th>
                <th>Students</th>
                <th>Applications</th>
                <th>Jobs</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((p) => (
                <tr key={p.code} className="border-t">
                  <td className="py-1">
                    {p.label}{" "}
                    <span className="text-xs text-slate-400">{p.code}</span>
                  </td>
                  <td>{p.users}</td>
                  <td>{p.applications}</td>
                  <td className={p.gap ? "font-bold text-red-600" : ""}>
                    {p.gap ? "no jobs" : p.jobs}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      <div className="rounded-2xl bg-white p-4 shadow">
        <h3 className="mb-2 font-bold">Most in-demand skills</h3>
        {stats.topSkills.map((s) => (
          <div key={s.skill} className="mb-1 flex items-center gap-2 text-sm">
            <span className="w-44 shrink-0">{s.skill}</span>
            <div
              className="h-3 rounded bg-crimson"
              style={{ width: `${(s.jobCount / maxDemand) * 60}%` }}
            />
            <span>{s.jobCount}</span>
          </div>
        ))}
      </div>

      <div className="rounded-2xl bg-white p-4 shadow">
        <h3 className="mb-1 font-bold">Job data report</h3>
        <p className="text-sm">
          {r.jobsLoaded} of {r.rowsRead} rows loaded.
        </p>
        {r.problems.length > 0 && (
          <ul className="mt-2 list-disc pl-5 text-sm text-red-700">
            {r.problems.map((p) => (
              <li key={p}>{p}</li>
            ))}
          </ul>
        )}
      </div>

      <p className="text-xs text-pink-100">
        Gender (anonymous statistics only, never used in matching):{" "}
        {Object.entries(stats.usersByGender)
          .map(([g, n]) => `${g.toLowerCase()}: ${n}`)
          .join(" · ")}
      </p>
    </div>
  );
}
