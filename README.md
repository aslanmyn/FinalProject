# KBTU Portal

KBTU Portal is a university platform with three application roles: `STUDENT`, `PROFESSOR`, and `ADMIN`.

It includes:
- role-based JWT authentication
- React frontend for student, teacher, and admin workflows
- REST API for web and mobile clients
- PostgreSQL persistence with Flyway migrations
- file storage with signed download links
- real-time notifications over WebSocket
- Gemini-based AI assistants for students and teachers

## Tech Stack

- Backend: Spring Boot 4, Spring Security, Spring Data JPA, Flyway, WebSocket/STOMP
- Database: PostgreSQL
- Frontend: React, Vite, TypeScript
- API docs: OpenAPI / Swagger
- AI: Google Gemini API

## Main Modules

- Public portal: home, news, professor catalog, professor public profile
- Student portal: registration center, schedule, enrollments, journal, transcript, attendance, exams, requests, finance, files, news, notifications, AI assistant
- Teacher portal: sections, roster, attendance, gradebook, materials, announcements, notes, grade changes, notifications, AI assistant
- Admin portal: registration operations, academic setup, finance, moderation, requests, users, notifications
- Shared: chat, signed file downloads, real-time notifications

## Run With Docker

```bash
docker compose up -d --build
```

Default URLs:
- Backend API: `http://localhost:8080`
- Frontend: `http://localhost:3000`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

If ports are busy:

```bash
APP_HOST_PORT=8081 FRONTEND_HOST_PORT=3001 docker compose up -d --build
```

Stop:

```bash
docker compose down
```

## Local Development

Backend:

```bash
./mvnw spring-boot:run
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

## Configuration

The backend loads runtime overrides from:
- `.env.local` via `spring.config.import=optional:file:.env.local[.properties]`
- environment variables
- `.env.example` as the reference template

Important variables:
- `APP_PROFILE`
- `APP_WEB_FRONTEND_URL`
- `APP_CORS_ALLOWED_ORIGINS`
- `APP_SEED_ENABLED`
- `DB_URL`
- `DB_USER`
- `DB_PASSWORD`
- `JWT_SECRET`
- `APP_STORAGE_ROOT`
- `APP_STORAGE_SIGNING_SECRET`
- `AI_ASSISTANT_ENABLED`
- `AI_ASSISTANT_READ_ONLY`
- `GEMINI_API_KEY`
- `GEMINI_MODEL`

Production recommendation:
- set `APP_SEED_ENABLED=false`
- use strong values for `JWT_SECRET` and `APP_STORAGE_SIGNING_SECRET`
- keep `GEMINI_API_KEY` only in local/host environment variables, never in git

## API and Frontend Notes

- Main REST namespace: `/api/v1/**`
- Public endpoints: `/api/v1/public/**`
- Auth endpoints: `/api/v1/auth/**`
- Student assistant: `/api/v1/student/assistant/chat`
- Teacher assistant: `/api/v1/teacher/assistant/chat`
- WebSocket endpoint: `/ws`
- User notification destination: `/user/queue/notifications`

For route details, request/response examples, and mobile guidance, see:
- `API_DOCUMENTATION.md`
- `MIGRATION_FRONTEND_REACT.md`
