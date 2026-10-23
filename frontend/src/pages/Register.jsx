import { useEffect, useState } from "react";
import { api } from "../api";

export default function Register({ onDone }) {
  const [programs, setPrograms] = useState([]);
  const [form, setForm] = useState({ name: "", program: "", gender: "" });
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  useEffect(() => { api.programs().then(setPrograms).catch((e) => setError(e.message)); }, []);

  // Group programs by college for the dropdown
  const colleges = [...new Set(programs.map((p) => p.college))];

  const submit = async (e) => {
    e.preventDefault();
    setError(""); setBusy(true);
    try { onDone(await api.register(form)); }
    catch (err) { setError(err.message); }
    finally { setBusy(false); }
  };

  const useSample = async (id) => {
    try { onDone(await api.getUser(id)); } catch (err) { setError(err.message); }
  };

  const field = "w-full rounded-lg border border-slate-300 bg-white px-3 py-2";

  return (
    <div className="mx-auto max-w-md">
      <h2 className="mb-1 text-2xl font-bold">Create your profile</h2>
      <p className="mb-4 text-sm text-slate-600">Pick your exact degree program. Matching is program-specific.</p>
      <form onSubmit={submit} className="space-y-3 rounded-xl bg-white p-5 shadow-sm">
        <input className={field} placeholder="Full name" value={form.name}
          onChange={(e) => setForm({ ...form, name: e.target.value })} required />
        <select className={field} value={form.program} required
          onChange={(e) => setForm({ ...form, program: e.target.value })}>
          <option value="">Select your program…</option>
          {colleges.map((c) => (
            <optgroup key={c} label={c}>
              {programs.filter((p) => p.college === c).map((p) => (
                <option key={p.code} value={p.code}>{p.name}</option>
              ))}
            </optgroup>
          ))}
        </select>
        <select className={field} value={form.gender}
          onChange={(e) => setForm({ ...form, gender: e.target.value })}>
          <option value="">Gender (optional)</option>
          <option value="MALE">Male</option>
          <option value="FEMALE">Female</option>
          <option value="OTHER">Other</option>
        </select>
        <p className="text-xs text-slate-500">Gender is never used for matching. It is only used for anonymous statistics.</p>
        {error && <p className="text-sm text-red-600">{error}</p>}
        <button disabled={busy} className="w-full rounded-lg bg-indigo-700 py-2 font-semibold text-white disabled:opacity-60">
          {busy ? "Creating…" : "Create profile"}
        </button>
      </form>

      <div className="mt-6 text-center text-sm text-slate-600">
        Just exploring? Continue as a sample student:
        <select className="ml-2 rounded border border-slate-300 bg-white px-2 py-1" defaultValue=""
          onChange={(e) => e.target.value && useSample(e.target.value)}>
          <option value="">Choose…</option>
          <option value="1">Juan – Computer Engineering</option>
          <option value="2">Maria – Computer Science</option>
          <option value="3">Carlo – Information Technology</option>
          <option value="4">Bea – Architecture</option>
          <option value="5">Ana – Nursing</option>
          <option value="6">Rico – Accountancy</option>
        </select>
      </div>
    </div>
  );
}
