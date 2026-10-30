import { useState } from "react";
import { api, setToken } from "../api";

export default function Login({ onDone, onSignup, onAdmin, notice }) {
  const [form, setForm] = useState({ email: "", password: "" });
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  const submit = async (e) => {
    e.preventDefault();
    setError("");
    setBusy(true);
    try {
      const res = await api.login(form);
      setToken(res.token);
      onDone(res.user, false);
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="mx-auto grid max-w-5xl items-center gap-8 px-4 py-6 md:grid-cols-2 md:py-10">
      <div>
        <h1 className="text-4xl font-extrabold tracking-tight text-gold md:text-5xl">
          SKILLBRIDGE
        </h1>
        <form
          onSubmit={submit}
          className="mt-6 space-y-3 rounded-3xl bg-white p-5 shadow-xl sm:p-6"
        >
          {notice && (
            <p className="rounded bg-amber-100 p-2 text-sm text-amber-800">
              {notice}
            </p>
          )}
          <div>
            <label className="text-sm font-bold tracking-wide">EMAIL</label>
            <input
              className="field"
              type="email"
              required
              value={form.email}
              onChange={(e) => setForm({ ...form, email: e.target.value })}
            />
          </div>
          <div>
            <label className="text-sm font-bold tracking-wide">PASSWORD</label>
            <input
              className="field"
              type="password"
              required
              value={form.password}
              onChange={(e) => setForm({ ...form, password: e.target.value })}
            />
          </div>
          {error && <p className="text-sm text-red-600">{error}</p>}
          <div className="flex justify-between gap-3 pt-2">
            <button
              type="button"
              onClick={onSignup}
              className="btn-ghost border border-maroon-800"
            >
              SIGN UP
            </button>
            <button disabled={busy} className="btn-primary">
              {busy ? "…" : "LOG IN"}
            </button>
          </div>
        </form>
        <p className="mt-3 text-xs text-maroon-700">
          <button onClick={onAdmin} className="text-pink-200 underline">
            Admin dashboard
          </button>
        </p>
      </div>

      <div className="text-white">
        <p className="text-sm font-semibold text-pink-200">
          Prepared for BPSU Main Campus Graduates
        </p>
        <p className="mt-3 text-lg leading-relaxed">
          Finding the right job shouldn't depend on connections.{" "}
          <b className="text-gold">SkillBridge</b> is a program-specific job
          matching platform built for BPSU graduates — ranking real openings
          based on your skills, target role, and program, with zero bias toward
          gender or background.
        </p>
        <p className="mt-3 font-bold text-gold">Always real. Always fair.</p>
      </div>
    </div>
  );
}
