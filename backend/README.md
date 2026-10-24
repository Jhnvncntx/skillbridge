# SkillBridge Backend v2 (Java + Spring Boot, custom data structures)

## Run locally
IntelliJ: open the `backend` folder, then run `SkillBridgeApplication` (green arrow).
Terminal: `mvn spring-boot:run`. The server starts on http://localhost:8080.
Check: http://localhost:8080/api/health  ->  {"status":"ok","programs":68,"jobs":16,"users":3}

## Demo accounts (password for all: Demo1234)
| Email | Program | Target role |
|---|---|---|
| cpe@demo.skillbridge | Computer Engineering (CPE) | Embedded Systems Engineer |
| cs@demo.skillbridge | CS - Software Development (CS-SD) | Backend Developer |
| nurse@demo.skillbridge | Nursing (BSN) | Staff Nurse |
Accounts live in memory and are re-created on every start. Disable them with env var SEED_DEMO=false.

## Data files (src/main/resources/data)
| File | What it is |
|---|---|
| programs.csv | The 68 programs/majors (code, group, program, major, level, status, note) |
| program_keywords.csv | The leader's 6 job-role + 4 skill keywords per program (used for the target-role dropdown) |
| jobs.csv | The job listings. **Currently 16 SAMPLE rows (example.com links).** Replace with the real data. |

### Updating the real jobs
1. In the Excel template (Jobs sheet): File > Download > CSV.
2. Replace `src/main/resources/data/jobs.csv` with that file (keep the same name).
3. Restart the server (or `git push` so Render redeploys).
4. Open the admin report to see any rows that were skipped and why:
   `curl -H "X-Admin-Passcode: admin123" localhost:8080/api/admin/stats`  ->  `dataReport.problems`
   (Console output on startup also lists skipped rows.)
Rows marked EXAMPLE and blank rows are ignored automatically.

## Environment variables
| Variable | Default | Purpose |
|---|---|---|
| PORT | 8080 | Port (Render sets it) |
| ADMIN_PASSCODE | admin123 | Admin dashboard passcode. CHANGE on Render. |
| ALLOWED_ORIGIN | http://localhost:5173 | Front-end URL(s) allowed by CORS, comma-separated |
| SEED_DEMO | true | Create the demo accounts |
| JOBS_CSV_PATH | (unset) | Read jobs from this file instead of the bundled jobs.csv |

## API
Log in first; send the token as `Authorization: Bearer <token>`.

| Method | Path | Purpose |
|---|---|---|
| GET | /api/health | status and counts |
| GET | /api/programs | flat program list (tree order) |
| GET | /api/programs/tree | College > Program > Major tree |
| GET | /api/programs/{code}/roles | suggested target roles |
| GET | /api/programs/{code}/skills | suggested hard skills |
| GET | /api/skills?prefix= | skill autocomplete |
| POST | /api/auth/signup | create account (returns token + user) |
| POST | /api/auth/login | log in (returns token + user) |
| POST | /api/auth/logout | end the session |
| GET / PUT | /api/me | view / edit profile |
| GET | /api/me/home?top=6 | `recommended` + `alsoQualify` |
| GET | /api/jobs?q= | all jobs ranked for me, or keyword search |
| GET | /api/jobs/{id} | one job with score breakdown |
| GET | /api/me/saved | saved jobs |
| POST / DELETE | /api/me/saved/{jobId} | save / unsave |
| POST | /api/me/applications | apply (body: jobId, note); response has the job `link` to open |
| GET | /api/me/applications | my applications |
| DELETE | /api/me/applications/last | undo last application |
| GET | /api/admin/stats | header `X-Admin-Passcode`; per-program stats, coverage gaps, data report |

Errors: `{"error": "message"}` with 400 / 401 / 404 / 409.

## Scoring (engine/ScoreCalculator.java)
Role match 50 + hard skills 30 x (matched / required) + program match 20 = 100.
The matching engine only receives a `MatchProfile` (program code, target role, hard skills).
Gender, age, address and contact never reach it.

## Where to read the data structures (suggested order)
1. datastructures/MyLinkedList, MyStack, MyQueue (linear)
2. MyHashTable (chaining + resize), SearchUtil, SortUtil (merge sort)
3. MyMaxHeap, MyTree (preorder with a stack, level order with a queue), MyGraph (BFS with a queue)
4. engine/MatchingEngine (how they are combined), engine/ScoreCalculator
