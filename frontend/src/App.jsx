import { useCallback, useEffect, useState } from "react";
import { api, getToken, setToken } from "./api";
import Login from "./pages/Login";
import Signup from "./pages/Signup";
import Welcome from "./pages/Welcome";
import Home from "./pages/Home";
import Jobs from "./pages/Jobs";
import Saved from "./pages/Saved";
import Applications from "./pages/Applications";
import Profile from "./pages/Profile";
import Admin from "./pages/Admin";

// Log out after this many minutes without any mouse, keyboard or touch activity (the server enforces the same limit).
const IDLE_MS = (Number(import.meta.env.VITE_IDLE_MINUTES) || 10) * 60 * 1000;
const KEEP_ALIVE_MS = 2 * 60 * 1000; // while the user is active, tell the server now and then so its timer agrees

export default function App() {
  const [user, setUser] = useState(null);
  const [screen, setScreen] = useState("login"); // login | signup | welcome | app | admin
  const [nav, setNav] = useState({ page: "home", q: "", jobId: null });
  const [booting, setBooting] = useState(!!getToken());
  const [waking, setWaking] = useState(false);
  const [notice, setNotice] = useState("");
  const [toast, setToast] = useState(null);
  const [search, setSearch] = useState("");

  // Free hosting sleeps when idle: show a friendly message while the server wakes up.
  useEffect(() => {
    const t = setTimeout(() => setWaking(true), 3000);
    api
      .health()
      .catch(() => {})
      .finally(() => {
        clearTimeout(t);
        setWaking(false);
      });
    return () => clearTimeout(t);
  }, []);

  // Restore a saved session.
  useEffect(() => {
    if (!getToken()) return;
    api
      .me()
      .then((u) => {
        setUser(u);
        setScreen("app");
      })
      .catch(() => setToken(null))
      .finally(() => setBooting(false));
  }, []);

  // The server ended the session (inactivity, or a restart): send the user back to login.
  useEffect(() => {
    const expired = (e) => {
      setToken(null);
      setUser(null);
      setScreen("login");
      setNotice(
        e.detail && e.detail.includes("inactivity")
          ? e.detail
          : "Your session ended (the server may have restarted). Please log in again.",
      );
    };
    window.addEventListener("sb-unauthorized", expired);
    return () => window.removeEventListener("sb-unauthorized", expired);
  }, []);

  // Inactivity check: any mouse, key, scroll or touch counts as activity.
  useEffect(() => {
    if (!user || (screen !== "app" && screen !== "welcome")) return undefined;
    let lastActivity = Date.now(),
      lastPing = Date.now();
    const onActivity = () => {
      lastActivity = Date.now();
      if (lastActivity - lastPing > KEEP_ALIVE_MS) {
        lastPing = lastActivity;
        api.me().catch(() => {});
      }
    };
    const events = ["mousemove", "keydown", "click", "scroll", "touchstart"];
    events.forEach((ev) =>
      window.addEventListener(ev, onActivity, { passive: true }),
    );
    const timer = setInterval(() => {
      if (Date.now() - lastActivity > IDLE_MS) {
        api.logout().catch(() => {});
        setToken(null);
        setUser(null);
        setScreen("login");
        setNotice(
          "You were logged out due to inactivity. Please log in again.",
        );
      }
    }, 15000);
    return () => {
      events.forEach((ev) => window.removeEventListener(ev, onActivity));
      clearInterval(timer);
    };
  }, [user, screen]);

  const notify = useCallback((message, ok = true) => {
    setToast({ message, ok });
    setTimeout(() => setToast(null), 5000);
  }, []);

  const onAuth = (u, isNew) => {
    setUser(u);
    setNotice("");
    setNav({ page: "home", q: "", jobId: null });
    setScreen(isNew ? "welcome" : "app");
  };
  const go = (page, extra = {}) => {
    setNav({ page, q: "", jobId: null, ...extra });
    window.scrollTo(0, 0);
  };
  const logout = async () => {
    try {
      await api.logout();
    } catch {
      /* ignore */
    }
    setToken(null);
    setUser(null);
    setScreen("login");
  };

  const doSearch = (e) => {
    e.preventDefault();
    go("jobs", { q: search.trim() });
  };

  let body;
  if (booting) body = <p className="p-8 text-white">Loading…</p>;
  else if (screen === "signup")
    body = <Signup onDone={onAuth} onBack={() => setScreen("login")} />;
  else if (screen === "admin")
    body = (
      <div className="mx-auto max-w-5xl px-4 py-6">
        <Admin onBack={() => setScreen("login")} />
      </div>
    );
  else if (screen === "welcome" && user)
    body = <Welcome user={user} onContinue={() => setScreen("app")} />;
  else if (screen === "app" && user) {
    body = (
      <>
        <header className="sticky top-0 z-50 bg-maroon-950/80 backdrop-blur-md">
          <div className="mx-auto flex max-w-6xl items-center justify-between gap-3 px-4 py-3">
            {/* Logo */}
            <button
              onClick={() => go("home")}
              className="text-xl font-extrabold tracking-tight text-gold shrink-0 transition-opacity hover:opacity-90"
            >
              SKILLBRIDGE
            </button>

            {/* Search Input (Expandable width) */}
            <form onSubmit={doSearch} className="mx-2 max-w-md flex-1">
              <input
                type="search"
                className="w-full rounded-full border border-black/20 bg-white px-4 py-1.5 text-sm text-gray-900 placeholder:text-gray-400 focus:outline-none focus:ring-2 focus:ring-gold"
                placeholder="ENTER KEYWORDS"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
              />
            </form>

            {/* Navigation Links */}
            <nav className="flex items-center gap-2 sm:gap-3">
              {/* Desktop/Tablet Buttons */}
              <button
                onClick={() => go("saved")}
                className="hidden rounded bg-crimson px-3 py-1.5 text-xs font-bold text-white transition hover:brightness-110 sm:inline-block sm:text-sm"
              >
                SAVED JOBS
              </button>

              <button
                onClick={() => go("applications")}
                className="hidden rounded bg-crimson px-3 py-1.5 text-xs font-bold text-white transition hover:brightness-110 sm:inline-block sm:text-sm"
              >
                MY APPLICATIONS
              </button>

              {/* Mobile Icon Shortcuts (Visible on small screens) */}
              <button
                onClick={() => go("saved")}
                title="Saved Jobs"
                className="rounded bg-crimson p-2 text-white transition hover:brightness-110 sm:hidden"
              >
                {/* Bookmark Icon */}
                <svg
                  className="h-4 w-4"
                  fill="none"
                  stroke="currentColor"
                  viewBox="0 0 24 24"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth="2"
                    d="M5 5a2 2 0 012-2h10a2 2 0 012 2v16l-7-3.5L5 21V5z"
                  />
                </svg>
              </button>

              <button
                onClick={() => go("applications")}
                title="My Applications"
                className="rounded bg-crimson p-2 text-white transition hover:brightness-110 sm:hidden"
              >
                {/* Briefcase Icon */}
                <svg
                  className="h-4 w-4"
                  fill="none"
                  stroke="currentColor"
                  viewBox="0 0 24 24"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth="2"
                    d="M21 13.255A23.931 23.931 0 0112 15c-3.183 0-6.22-.62-9-1.745M16 6V4a2 2 0 00-2-2h-4a2 2 0 00-2 2v2m4 6h.01M5 20h14a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 002 2v10a2 2 0 002 2z"
                  />
                </svg>
              </button>

              {/* Profile Button */}
              <button
                onClick={() => go("profile")}
                className="flex items-center gap-2 text-xs font-medium text-white transition hover:opacity-80"
              >
                <span className="flex h-8 w-8 items-center justify-center rounded-full bg-white font-bold text-maroon-900 shrink-0">
                  {user.fullName[0]}
                </span>
                <span className="hidden md:inline">MY PROFILE</span>
              </button>
            </nav>
          </div>
        </header>
        <main className="mx-auto max-w-6xl px-4 py-6">
          {nav.page === "home" && <Home user={user} go={go} notify={notify} />}
          {nav.page === "jobs" && (
            <Jobs
              q={nav.q}
              selectedId={nav.jobId}
              onSelect={(id) => setNav({ ...nav, jobId: id })}
              notify={notify}
            />
          )}
          {nav.page === "saved" && <Saved go={go} notify={notify} />}
          {nav.page === "applications" && <Applications notify={notify} />}
          {nav.page === "profile" && (
            <Profile
              user={user}
              onSaved={setUser}
              onLogout={logout}
              notify={notify}
              onDeleted={() => {
                setToken(null);
                setUser(null);
                setScreen("login");
                setNotice("Your account and all of its data were deleted.");
              }}
            />
          )}
        </main>
      </>
    );
  } else
    body = (
      <Login
        onDone={onAuth}
        onSignup={() => setScreen("signup")}
        onAdmin={() => setScreen("admin")}
        notice={notice}
      />
    );

  return (
    <div className="min-h-screen">
      {waking && (
        <div className="bg-amber-100 px-4 py-2 text-center text-sm text-amber-800">
          Waking up the server… this can take up to a minute on the first visit.
        </div>
      )}
      {toast && (
        <div
          className={`fixed left-1/2 top-4 z-50 -translate-x-1/2 rounded-xl px-5 py-3 text-sm font-semibold shadow-lg ${toast.ok ? "bg-emerald-600 text-white" : "bg-red-600 text-white"}`}
        >
          {toast.message}
        </div>
      )}
      {body}
    </div>
  );
}
