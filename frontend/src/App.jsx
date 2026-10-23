import { useEffect, useState } from "react";
import { api } from "./api";
import Register from "./pages/Register";
import Skills from "./pages/Skills";
import Jobs from "./pages/Jobs";
import Applications from "./pages/Applications";
import PostJob from "./pages/PostJob";
import Admin from "./pages/Admin";

const MODES = [["student", "Job Seeker"], ["employer", "Employer"], ["admin", "Admin"]];
const TABS = [["jobs", "Recommended Jobs"], ["applications", "My Applications"], ["skills", "My Skills"]];

export default function App() {
  const [mode, setMode] = useState("student");
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [tab, setTab] = useState("jobs");
  const [waking, setWaking] = useState(false);

  // Free hosting sleeps when idle: show a friendly message while the server wakes up.
  useEffect(() => {
    const t = setTimeout(() => setWaking(true), 3000);
    api.health().catch(() => {}).finally(() => { clearTimeout(t); setWaking(false); });
    return () => clearTimeout(t);
  }, []);

  // Restore the saved user. The server keeps data in memory, so the id may no longer exist.
  useEffect(() => {
    const id = localStorage.getItem("sb_user");
    if (!id) return setLoading(false);
    api.getUser(id).then(setUser)
      .catch(() => localStorage.removeItem("sb_user"))
      .finally(() => setLoading(false));
  }, []);

  const login = (u) => { localStorage.setItem("sb_user", u.id); setUser(u); setTab("jobs"); };
  const logout = () => { localStorage.removeItem("sb_user"); setUser(null); };

  return (
    <div className="min-h-screen">
      <header className="bg-indigo-700 text-white">
        <div className="mx-auto flex max-w-4xl flex-wrap items-center justify-between gap-2 px-4 py-4">
          <h1 className="text-xl font-bold">SkillBridge</h1>
          <div className="flex gap-1 text-sm">
            {MODES.map(([k, l]) => (
              <button key={k} onClick={() => setMode(k)}
                className={`rounded-full px-3 py-1 ${mode === k ? "bg-white text-indigo-700" : "text-indigo-100 hover:bg-indigo-600"}`}>{l}</button>
            ))}
          </div>
          {mode === "student" && user && (
            <div className="text-right text-sm">
              <div className="font-semibold">{user.name}</div>
              <div className="text-indigo-200">{user.programName}</div>
              <button onClick={logout} className="text-xs underline">Switch user</button>
            </div>
          )}
        </div>
      </header>

      {waking && (
        <div className="bg-amber-100 px-4 py-2 text-center text-sm text-amber-800">
          Waking up the server… this can take up to a minute on the first visit.
        </div>
      )}

      <main className="mx-auto max-w-4xl px-4 py-6">
        {mode === "employer" ? <PostJob /> : mode === "admin" ? <Admin /> :
          loading ? <p>Loading…</p> : !user ? <Register onDone={login} /> : (
            <>
              <nav className="mb-6 flex flex-wrap gap-2">
                {TABS.map(([key, label]) => (
                  <button key={key} onClick={() => setTab(key)}
                    className={`rounded-full px-4 py-2 text-sm font-medium ${tab === key ? "bg-indigo-700 text-white" : "bg-white text-slate-700 shadow-sm"}`}>
                    {label}
                  </button>
                ))}
              </nav>
              {tab === "skills" ? <Skills user={user} onSaved={setUser} />
                : tab === "applications" ? <Applications user={user} />
                : <Jobs user={user} />}
            </>
          )}
      </main>
    </div>
  );
}
