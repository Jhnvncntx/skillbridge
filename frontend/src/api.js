// All backend calls live here. VITE_API_URL points to the Spring Boot server.
const BASE = (import.meta.env.VITE_API_URL || "http://localhost:8080").replace(/\/$/, "");

async function call(path, method = "GET", body, extraHeaders) {
  const res = await fetch(BASE + path, {
    method,
    headers: body || extraHeaders ? { ...(body ? { "Content-Type": "application/json" } : {}), ...extraHeaders } : undefined,
    body: body ? JSON.stringify(body) : undefined,
  });
  const data = await res.json().catch(() => null);
  if (!res.ok) {
    const err = new Error(data?.error || `Request failed (${res.status})`);
    err.status = res.status;
    throw err;
  }
  return data;
}

export const api = {
  health: () => call("/api/health"),
  programs: () => call("/api/programs"),
  register: (b) => call("/api/users", "POST", b),
  getUser: (id) => call(`/api/users/${id}`),
  setSkills: (id, skills) => call(`/api/users/${id}/skills`, "PUT", { skills }),
  skillIndex: () => call("/api/skills"),
  recommendations: (id, top = 5) => call(`/api/users/${id}/recommendations?top=${top}`),
  ranked: (id) => call(`/api/users/${id}/ranked`),
  apply: (userId, jobId, note = "") => call("/api/applications", "POST", { userId, jobId, note }),
  applications: (id) => call(`/api/users/${id}/applications`),
  jobs: () => call("/api/jobs"),
  postJob: (b) => call("/api/jobs", "POST", b),
  stats: (passcode) => call("/api/admin/stats", "GET", undefined, { "X-Admin-Passcode": passcode }),
  undoLast: (id) => call(`/api/users/${id}/applications/last`, "DELETE"),
};
