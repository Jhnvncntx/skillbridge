# SkillBridge Frontend (React + Vite + Tailwind)

Requires Node.js LTS.

    npm install
    npm run dev        # http://localhost:5173  (backend must be running on :8080)

Set the backend URL in `.env` (see `.env.example`): `VITE_API_URL=http://localhost:8080`

Render Static Site: build command `npm install && npm run build`, publish directory `dist`,
env var `VITE_API_URL` = your backend's Render URL.
