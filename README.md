# KBTU Portal

KBTU Portal is a full-stack university platform with four major surfaces:
- public portal
- student portal
- teacher portal
- admin portal

It already includes:
- JWT authentication and refresh tokens
- React SPA frontend
- Spring Boot API backend
- PostgreSQL with Flyway migrations
- chat and real-time notifications over WebSocket
- AI assistants for student, teacher, and admin roles
- academic workflows: registration, add/drop, FX, grade changes, requests, analytics
- campus life modules: dorm, food ordering, campus map, laundry

## Architecture

Frontend:
- React + Vite + TypeScript
- role-protected routes under `/app/**`

Backend:
- Spring Boot 4
- Spring Security
- Spring Data JPA
- Flyway
- WebSocket/STOMP
- OpenAPI / Swagger

Database:
- PostgreSQL

## Main Modules

Public:
- home
- public news
- professor directory
- public professor profile

Student:
- profile and profile photo
- registration center
- course detail page with grades, attendance, exam, announcements, and materials
- schedule
- enrollments
- journal
- transcript
- attendance and self check-in attendance
- exam schedule
- requests and request messages
- finance and holds
- files and materials
- notifications
- planner and workflows
- AI assistant
- wellbeing / psychological support assistant
- dorm
- food ordering
- campus map
- laundry

Teacher:
- profile and profile photo
- sections and section details
- roster
- live attendance control
- gradebook and final grades
- announcements
- materials
- student notes
- grade change requests
- notifications
- risk dashboard
- AI assistant

Admin:
- users and admin permissions
- academic setup: subjects, teachers, terms, sections, windows, exams
- registration override
- finance and holds
- mobility, clearance, surveys, requests
- notifications
- analytics and workflows
- AI assistant
- create and update student records
- create and update teacher records
- create and update subject records

Shared:
- chat
- signed file downloads
- real-time notifications
- real-time attendance events

## Local Development

Backend:

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments=--app.seed.enabled=false
```

Frontend:

```bash
cd frontend
npm install
npm run dev
```

Default local URLs:
- Backend API: `http://localhost:8080`
- Frontend dev server: `http://localhost:5173`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## Docker

```bash
docker compose up -d --build
```

Default Docker URLs:
- Backend API: `http://localhost:8080`
- Frontend: `http://localhost:3000`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`

Stop:

```bash
docker compose down
```

## Configuration

Runtime overrides are loaded from:
- `.env.local`
- environment variables
- `.env.example` as the template

Important variables:
- `APP_PROFILE`
- `APP_WEB_FRONTEND_URL`
- `APP_CORS_ALLOWED_ORIGINS`
- `DB_URL`
- `DB_USER`
- `DB_PASSWORD`
- `JWT_SECRET`
- `APP_STORAGE_ROOT`
- `APP_STORAGE_SIGNING_SECRET`
- `APP_SEED_ENABLED`
- `APP_SEED_CONFIRM_TOKEN`
- `AI_ASSISTANT_ENABLED`
- `AI_ASSISTANT_READ_ONLY`
- `GEMINI_API_KEY`
- `GEMINI_MODEL`

## Demo Data and Seed

Important:
- the repository does not contain your local PostgreSQL data
- the repository does not contain `.env.local`
- the repository does not contain `seed-users.local.txt`
- `storage/` is also local and ignored by git

That means another person who clones the repo will not automatically get your existing 100 students, passwords, files, or local database state.

Seed behavior:
- seed is disabled by default
- seed is additionally protected by a confirm token
- default safe mode is:
  - `APP_SEED_ENABLED=false`
  - `APP_SEED_CONFIRM_TOKEN=`

To generate the demo dataset on a fresh database, both must be set:

```env
APP_SEED_ENABLED=true
APP_SEED_CONFIRM_TOKEN=DEMO_ONLY_RESET
```

If someone needs your exact current data, they need a PostgreSQL dump, not just the repository.

## Login and Role Rules

Role is inferred from the email format.

Examples:
- student: `a_mustafayev@kbtu.kz`
- professor: `a.nurgaliyev@kbtu.kz`
- admin: `admin@kbtu.kz`

Typical demo passwords:
- student: `student123`
- professor: `prof123`
- admin: `admin123`

## AI Assistant

The AI assistant does not have direct database access.

How it works:
1. frontend sends the question to backend
2. backend authenticates the user
3. backend loads the needed context from repositories/services
4. backend sends structured context plus the question to Gemini
5. Gemini returns an answer
6. backend returns the answer to the frontend

So:
- database access is only done by the backend
- Gemini does not connect to PostgreSQL directly
- assistants are read-only
- some planner calculations are deterministic and computed in backend first

## API Notes

Main namespaces:
- `/api/v1/auth/**`
- `/api/v1/public/**`
- `/api/v1/student/**`
- `/api/v1/student/assistant/**`
- `/api/v1/teacher/**`
- `/api/v1/teacher/assistant/**`
- `/api/v1/admin/**`
- `/api/v1/chat/**`
- `/api/v1/files/**`

Realtime:
- WebSocket endpoint: `/ws`
- notifications: `/user/queue/notifications`
- attendance events: section and user-specific destinations used by frontend

For route details and example payloads, see:
- `API_DOCUMENTATION.md`
- `MIGRATION_FRONTEND_REACT.md`
