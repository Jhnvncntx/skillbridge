import { useState } from "react";
import { api } from "../api";

export default function Admin() {
  const [pass, setPass] = useState("");
  const [stats, setStats] = useState(null);
  const [error, setError] = useState("");

  const load = async (e) => {
    e?.preventDefault();
    setError("");
    try { setStats(await api.stats(pass)); }
    catch (err) { setStats(null); setError(err.message); }
  };

  if (!stats) {
    return (
      <form onSubmit={load} className="mx-auto max-w-sm space-y-3 rounded-xl bg-white p-5 shadow-sm">
        <h2 className="text-xl font-bold">Admin dashboard</h2>
        <input type="password" value={pass} onChange={(e) => setPass(e.target.value)} placeholder="Admin passcode"
          className="w-full rounded-lg border border-slate-300 px-3 py-2" />
        {error && <p className="text-sm text-red-600">{error}</p>}
        <button className="w-full rounded-lg bg-indigo-700 py-2 font-semibold text-white">Open dashboard</button>
      </form>
    );
  }

  const maxDemand = Math.max(1, ...stats.topSkills.map((s) => s.jobCount));
  const Card = ({ label, value }) => (
    <div className="rounded-xl bg-white p-4 text-center shadow-sm">
      <div className="text-3xl font-bold text-indigo-700">{value}</div>
      <div className="text-sm text-slate-600">{label}</div>
    </div>
  );

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h2 className="text-2xl font-bold">Employability statistics by program</h2>
        <button onClick={load} className="rounded-lg bg-white px-3 py-1 text-sm shadow-sm">Refresh</button>
      </div>
      <div className="grid grid-cols-3 gap-3">
        <Card label="Students" value={stats.totalUsers} />
        <Card label="Job postings" value={stats.totalJobs} />
        <Card label="Applications" value={stats.totalApplications} />
      </div>

      <div className="overflow-x-auto rounded-xl bg-white p-4 shadow-sm">
        <table className="w-full text-left text-sm">
          <thead><tr className="border-b text-slate-500">
            <th className="py-2">Program</th><th>Students</th><th>Applications</th><th>Jobs targeting</th>
          </tr></thead>
          <tbody>
            {stats.programs.map((p) => (
              <tr key={p.code} className="border-b last:border-0">
                <td className="py-2">{p.name}</td><td>{p.users}</td><td>{p.applications}</td>
                <td className={p.jobsTargeting === 0 ? "text-red-600" : ""}>{p.jobsTargeting}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="rounded-xl bg-white p-4 shadow-sm">
        <h3 className="mb-2 font-semibold">Most in-demand skills</h3>
        {stats.topSkills.map((s) => (
          <div key={s.skill} className="mb-1 flex items-center gap-2 text-sm">
            <span className="w-40 shrink-0">{s.skill}</span>
            <div className="h-3 rounded bg-indigo-500" style={{ width: `${(s.jobCount / maxDemand) * 100}%` }} />
            <span>{s.jobCount}</span>
          </div>
        ))}
      </div>

      <p className="text-xs text-slate-500">
        Gender (anonymous statistics only, never used in matching):{" "}
        {Object.entries(stats.usersByGender).map(([g, n]) => `${g.toLowerCase()}: ${n}`).join(" · ")}
      </p>
    </div>
  );
}
