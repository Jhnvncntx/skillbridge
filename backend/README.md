# SkillBridge Backend v3 (Java + Spring Boot, custom data structures, PostgreSQL storage)

## What is stored where
| Data | Where | Survives a restart? |
|---|---|---|
| Job listings | `src/main/resources/data/jobs.csv` (in GitHub) | Yes (it is part of the code) |
| Accounts, profiles, saved jobs, applications | PostgreSQL (Neon) when `DATABASE_URL` is set | **Yes** |
| Login sessions | Server memory | No. A restart means logging in again (accounts are safe). |

All matching still runs on our own data structures in memory. The database only stores data:
at startup the server loads it into the hash tables, lists and undo stacks, and every change is saved to
the database first, then applied in memory.

## Run locally (no database needed)
IntelliJ: open `backend`, run `SkillBridgeApplication`. Or `mvn spring-boot:run`.
Without `DATABASE_URL` it runs in **memory-only** mode (data is lost on restart), exactly like before.
Check: http://localhost:8080/api/health  ->  `"database":"memory-only"` (or `"connected"` with a database).

## Environment variables
| Variable | Default | Purpose |
|---|---|---|
| DATABASE_URL | (unset = memory-only) | PostgreSQL address, e.g. `postgresql://user:password@host/dbname?sslmode=require` |
| SESSION_MINUTES | 10 | Log out after this many minutes of inactivity |
| ADMIN_PASSCODE | admin123 | Admin dashboard passcode. CHANGE on Render. |
| ALLOWED_ORIGIN | http://localhost:5173 | Front-end URL(s) allowed by CORS, comma-separated |
| SEED_DEMO | true | Create the 3 demo accounts (password Demo1234). With a database they are saved once. Set `false` for the real site. |
| JOBS_CSV_PATH | (unset) | Read jobs from this file instead of the bundled jobs.csv |
| PORT | 8080 | Port (Render sets it) |

Never put the database address in GitHub or in chat. It belongs only in Render's Environment settings.

## Setting up Neon (free PostgreSQL)
1. Create a free account at neon.tech and a project. Choose the region closest to the Philippines (Singapore).
2. In the Neon dashboard click **Connect** and copy the **connection string** (it starts with `postgresql://`).
   Copy only the address: no `psql` word and no quotes.
3. Render > your backend service > **Environment** > add `DATABASE_URL` = that string. Optionally add `SEED_DEMO` = `false`.
4. Save. Render redeploys. Open `https://YOUR-BACKEND.onrender.com/api/health` and look for `"database":"connected"`.
5. Test: register an account, save a job, apply, then **Manual Deploy > Restart** (or wait for the free server to sleep and wake).
   Log in again: the account, saved jobs and applications should still be there.
The tables are created automatically on the first start.

If the server cannot reach the database at startup it stops with a clear error in the Render logs (it does not
silently fall back to memory). To roll back, delete `DATABASE_URL`: the app returns to memory-only mode.

## Database tables
`users` (profile, hashed password), `saved_jobs` (user, job key), `applications` (user, job key, plus a copy of the job title,
company and link so history still shows if a posting is removed from jobs.csv). Deleting a user also deletes their saved jobs and applications.

## Job ids
A job's id is built from its link, not its row number: `jobstreet-95263012`, `indeed-8dd834be09a07811`, or `link-<hash>`.
Tracking parameters in the link are ignored. Editing or reordering `jobs.csv` therefore never breaks anyone's saved jobs or applications.

## Updating the real jobs
1. Export the Jobs sheet as CSV and name it `jobs.csv`.
2. Replace `src/main/resources/data/jobs.csv` and push. Render redeploys.
3. Check the admin report: `curl -H "X-Admin-Passcode: ..." https://YOUR-BACKEND.onrender.com/api/admin/stats` -> `dataReport.problems`.

## Demo accounts (password: Demo1234)
cpe@demo.skillbridge (Computer Engineering), cs@demo.skillbridge (CS - Software Development), nurse@demo.skillbridge (Nursing).

## API
Log in first; send the token as `Authorization: Bearer <token>`. Any request counts as activity; after SESSION_MINUTES without one
the session ends (401 "You were logged out due to inactivity.").

| Method | Path | Purpose |
|---|---|---|
| GET | /api/health | status, counts, database (connected / memory-only) |
| GET | /api/programs, /api/programs/tree | program list / College > Program > Major tree |
| GET | /api/programs/{code}/roles, /skills | suggested target roles / skills |
| GET | /api/skills?prefix= | skill autocomplete |
| POST | /api/auth/signup, /login, /logout | account and session |
| GET / PUT | /api/me | view / edit profile |
| POST | /api/me/delete | permanently delete my account (body: `{"password": "..."}`; 403 if wrong) |
| GET | /api/me/home?top=6 | `recommended` + `alsoQualify` |
| GET | /api/jobs?q= | all jobs ranked for me, or keyword search |
| GET | /api/jobs/{id} | one job with score breakdown |
| GET / POST / DELETE | /api/me/saved[/{jobId}] | saved jobs |
| POST / GET | /api/me/applications | apply (returns the job link) / list |
| DELETE | /api/me/applications/last | undo last application |
| DELETE | /api/me/applications/{id} | remove one application from my list (numeric id; only your own; 404 otherwise). Does not withdraw anything on the job site. |
| GET | /api/admin/stats | header `X-Admin-Passcode` |

Errors: `{"error": "message"}` with 400 / 401 / 403 / 404 / 409 / 503 (database temporarily unavailable).

## Scoring (engine/ScoreCalculator.java)
Role match 50 + hard skills 30 x (matched / required) + program match 20 = 100. A student in a major (for example CS-SD)
also matches jobs tagged with the base program (CS). The engine only receives a `MatchProfile` (program codes, target role,
hard skills). Gender and age never reach it.

## Where to read the code
1. datastructures/ (MyLinkedList, MyStack, MyQueue, MyHashTable, MyMaxHeap, MyTree, MyGraph, SortUtil, SearchUtil)
2. engine/MatchingEngine, ScoreCalculator
3. persistence/ (Persistence interface, MemoryPersistence, PostgresPersistence)
4. service/SkillBridgeService (all business logic)
