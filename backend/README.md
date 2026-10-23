# SkillBridge Backend (Spring Boot + custom data structures)

## Requirements
- JDK 17 or 21
- Maven 3.9+ (or let VS Code / IntelliJ's Maven handle it)

## Run locally
```
cd backend
mvn spring-boot:run
```
Server starts on http://localhost:8080. In VS Code you can also open
`SkillBridgeApplication.java` and click **Run** above `main`.

## Environment variables (all optional locally)
| Variable | Default | Purpose |
|---|---|---|
| `PORT` | 8080 | Port (Render sets this) |
| `ADMIN_PASSCODE` | admin123 | Passcode for `/api/admin/stats` - CHANGE on Render |
| `ALLOWED_ORIGIN` | http://localhost:5173 | Front-end URL(s) allowed by CORS, comma-separated |

## Try it (browser or curl)
```
curl localhost:8080/api/health
curl localhost:8080/api/programs
curl localhost:8080/api/jobs
curl "localhost:8080/api/users/1/recommendations?top=3"
curl localhost:8080/api/users/2/ranked
curl "localhost:8080/api/skills/check?name=python"
curl localhost:8080/api/skills

# register a student, tag skills, get matches
curl -X POST localhost:8080/api/users -H "Content-Type: application/json" \
     -d '{"name":"Test Student","program":"BSIT","gender":"FEMALE"}'
curl -X PUT localhost:8080/api/users/7/skills -H "Content-Type: application/json" \
     -d '{"skills":["html","css","javascript"]}'
curl localhost:8080/api/users/7/recommendations

# employer posts a job
curl -X POST localhost:8080/api/jobs -H "Content-Type: application/json" \
     -d '{"title":"Junior Web Dev","company":"Demo Co","targetPrograms":["BSIT","BSCS"],"requiredSkills":["html","css","react"]}'

# apply, list, undo last (per user)
curl -X POST localhost:8080/api/applications -H "Content-Type: application/json" \
     -d '{"userId":7,"jobId":"J9","note":"Interested!"}'
curl localhost:8080/api/users/7/applications
curl -X DELETE localhost:8080/api/users/7/applications/last

# admin (needs header)
curl -H "X-Admin-Passcode: admin123" localhost:8080/api/admin/stats
```
Seed data: 24 simulated jobs (all 21 programs covered) and 6 sample students (ids 1-6).
Everything is in memory and resets on restart.

Errors come back as `{"error": "message"}` with 400 / 401 / 404 / 409.

## Package map
- `datastructures/` MyLinkedList, MyStack, MyHashTable, MyGraph, MyMaxHeap, SortUtil, SearchUtil
- `engine/`  MatchingEngine, ScoreCalculator (gender-blind), MatchResult, SkillDemand
- `model/`   Program (21 programs), Gender, User, Job, Application
- `service/` SkillBridgeService (all logic), SeedData, ApiException
- `controller/` ApiController (REST), ApiExceptionHandler
- `dto/`     request / response records
- `config/`  CorsConfig

## Deploy on Render (Docker)
New > Web Service > connect repo > Root Directory `backend` > Runtime: Docker.
Set env vars `ADMIN_PASSCODE` and `ALLOWED_ORIGIN`. Health check path: `/api/health`.
