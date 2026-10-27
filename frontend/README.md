# SkillBridge Frontend v2 (React + Vite + Tailwind)

Requires Node.js LTS and the v2 backend running on http://localhost:8080.

    npm install
    npm run dev        # http://localhost:5173

Backend URL: `.env` -> `VITE_API_URL=http://localhost:8080` (see `.env.example`).

## Screens (from the leader's Canva)
| Screen | File |
|---|---|
| Login | src/pages/Login.jsx |
| Sign up (3 steps + privacy notice) | src/pages/Signup.jsx |
| Welcome | src/pages/Welcome.jsx |
| Home (Recommended / You might also qualify to) | src/pages/Home.jsx |
| Jobs (list left, detail right, Apply Now) | src/pages/Jobs.jsx |
| Profile | src/pages/Profile.jsx |
| Saved jobs, My Applications | src/pages/Saved.jsx, Applications.jsx |
| Admin dashboard (link on the login page) | src/pages/Admin.jsx |

Shared pieces: `src/api.js` (all backend calls), `components/ProgramPicker.jsx` (College > Program > Major),
`components/TagInput.jsx` (skills, languages), `components/JobCard.jsx`.

## Render Static Site
Build command `npm install && npm run build`, publish directory `dist`,
env var `VITE_API_URL` = your backend's Render URL.
