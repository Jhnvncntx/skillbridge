// Every backend call lives here. VITE_API_URL points to the Spring Boot server.
const BASE = (import.meta.env.VITE_API_URL || "http://localhost:8080").replace(/\/$/, "");

let token = localStorage.getItem("sb_token");
export const getToken = () => token;
export function setToken(t) {
  token = t;
  if (t) localStorage.setItem("sb_token", t); else localStorage.removeItem("sb_token");
}

async function call(path, method = "GET", body, extraHeaders) {
  const headers = { ...(body ? { "Content-Type": "application/json" } : {}), ...extraHeaders };
  if (token) headers.Authorization = `Bearer ${token}`;
  const res = await fetch(BASE + path, {
    method,
    headers: Object.keys(headers).length ? headers : undefined,
    body: body ? JSON.stringify(body) : undefined,
  });
  const data = await res.json().catch(() => null);
  if (!res.ok) {
    const err = new Error(data?.error || `Request failed (${res.status})`);
    err.status = res.status;
    if (res.status === 401 && token) window.dispatchEvent(new CustomEvent("sb-unauthorized", { detail: err.message }));   // session ended
    throw err;
  }
  return data;
}

export const api = {
  health: () => call("/api/health"),
  tree: () => call("/api/programs/tree"),
  roles: (code) => call(`/api/programs/${code}/roles`),
  programSkills: (code) => call(`/api/programs/${code}/skills`),
  skillSuggest: (prefix) => call(`/api/skills?prefix=${encodeURIComponent(prefix)}`),
  signup: (b) => call("/api/auth/signup", "POST", b),
  login: (b) => call("/api/auth/login", "POST", b),
  logout: () => call("/api/auth/logout", "POST"),
  me: () => call("/api/me"),
  updateMe: (b) => call("/api/me", "PUT", b),
  home: (top = 6) => call(`/api/me/home?top=${top}`),
  jobs: (q = "") => call(`/api/jobs?q=${encodeURIComponent(q)}`),
  saved: () => call("/api/me/saved"),
  save: (id) => call(`/api/me/saved/${id}`, "POST"),
  unsave: (id) => call(`/api/me/saved/${id}`, "DELETE"),
  apply: (jobId, note = "") => call("/api/me/applications", "POST", { jobId, note }),
  applications: () => call("/api/me/applications"),
  undoLast: () => call("/api/me/applications/last", "DELETE"),
  deleteAccount: (password) => call("/api/me/delete", "POST", { password }),
  adminStats: (passcode) => call("/api/admin/stats", "GET", undefined, { "X-Admin-Passcode": passcode }),
};

/**
 * Apply = open the original posting AND record the application (paperless log).
 * The tab is opened first, directly from the click, so the browser does not block it.
 */
export async function applyAndOpen(job) {
  window.open(job.link, "_blank", "noopener,noreferrer");
  try { await api.apply(job.id); return { ok: true, message: "Application recorded. The original posting opened in a new tab." }; }
  catch (e) {
    return e.status === 409
      ? { ok: true, message: "You already applied to this job. The posting opened again." }
      : { ok: false, message: e.message };
  }
}
