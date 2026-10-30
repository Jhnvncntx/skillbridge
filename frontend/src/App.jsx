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

function BottomNav({ page, go }) {
  const items = [
    ["home", "🏠", "Home"],
    ["jobs", "💼", "Jobs"],
    ["saved", "⭐", "Saved"],
    ["applications", "📄", "Applied"],
    ["profile", "👤", "Profile"],
  ];
  return (
    <nav className="fixed inset-x-0 bottom-0 z-40 flex border-t border-maroon-800 bg-maroon-950 pb-[env(safe-area-inset-bottom)] md:hidden">
      {items.map(([key, icon, label]) => (
        <button
          key={key}
          onClick={() => go(key)}
          aria-label={label}
          className={`flex flex-1 flex-col items-center py-2 text-xs ${page === key ? "font-bold text-gold" : "text-pink-200"}`}
        >
          <span className="text-lg leading-none">{icon}</span>
          {label}
        </button>
      ))}
    </nav>
  );
}

const IDLE_MS = (Number(import.meta.env.VITE_IDLE_MINUTES) || 10) * 60 * 1000;
const KEEP_ALIVE_MS = 2 * 60 * 1000;

export default function App() {
  const [user, setUser] = useState(null);
  const [screen, setScreen] = useState("login");
  const [nav, setNav] = useState({ page: "home", q: "", jobId: null });
  const [booting, setBooting] = useState(!!getToken());
  const [waking, setWaking] = useState(false);
  const [notice, setNotice] = useState("");
  const [toast, setToast] = useState(null);
  const [search, setSearch] = useState("");

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
        <header className="bg-maroon-950/70 backdrop-blur">
          <div className="mx-auto flex max-w-6xl flex-wrap items-center gap-x-3 gap-y-2 px-4 py-3">
            <button
              onClick={() => go("home")}
              className="text-xl font-extrabold text-gold"
            >
              SKILLBRIDGE
            </button>
            {/* phones: the search bar drops to its own full-width row (order-3); desktop: one row */}
            <form
              onSubmit={doSearch}
              className="order-3 w-full md:order-0 md:w-auto md:min-w-50 md:flex-1"
            >
              <input
                className="w-full rounded-full border-2 border-black bg-white px-4 py-1.5"
                placeholder="ENTER KEYWORDS"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
              />
            </form>
            <div className="ml-auto flex items-center gap-3 md:ml-0">
              {/* on phones these two live in the bottom tab bar instead */}
              <button
                onClick={() => go("saved")}
                className="hidden rounded bg-crimson px-3 py-1 text-sm font-bold text-white md:block"
              >
                SAVED JOBS
              </button>
              <button
                onClick={() => go("applications")}
                className="hidden rounded bg-crimson px-3 py-1 text-sm font-bold text-white md:block"
              >
                MY APPLICATIONS
              </button>
              <button
                onClick={() => go("profile")}
                className="text-center text-xs text-white"
              >
                <span className="mx-auto flex h-8 w-8 items-center justify-center rounded-full bg-white font-bold text-maroon-900">
                  {user.fullName[0]}
                </span>
                MY PROFILE
              </button>
            </div>
          </div>
        </header>
        <main className="mx-auto max-w-6xl px-4 py-6 pb-24 md:pb-6">
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
        <BottomNav page={nav.page} go={go} />
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
    <div className="min-h-dvh">
      {waking && (
        <div className="bg-amber-100 px-4 py-2 text-center text-sm text-amber-800">
          Waking up the server… this can take up to a minute on the first visit.
        </div>
      )}
      {toast && (
        <div
          className={`fixed left-1/2 top-4 z-50 w-[90%] max-w-md -translate-x-1/2 rounded-xl px-5 py-3 text-center text-sm font-semibold shadow-lg ${toast.ok ? "bg-emerald-600 text-white" : "bg-red-600 text-white"}`}
        >
          {toast.message}
        </div>
      )}
      {body}
    </div>
  );
}
