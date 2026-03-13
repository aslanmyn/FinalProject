# KBTU Portal

University portal with three roles: `STUDENT`, `PROFESSOR`, `ADMIN`.

## Tech Stack

- Backend: Spring Boot 3, Spring Security, Spring Data JPA, Flyway
- Database: PostgreSQL
- Frontend: React + Vite + TypeScript
- API docs: OpenAPI / Swagger

## Run With Docker

```bash
docker compose up -d --build
```

Default URLs:
- Backend API: `http://localhost:8080`
- React frontend: `http://localhost:3000`
- Swagger: `http://localhost:8080/swagger-ui/index.html`

If ports are busy:

```bash
APP_HOST_PORT=8081 FRONTEND_HOST_PORT=3001 docker compose up -d --build
```

Stop:

```bash
docker compose down
```

## Local Frontend Dev

```bash
cd frontend
npm install
npm run dev
```

Default frontend dev URL: `http://localhost:5173`.

## Configuration

Use `.env.example` as template for runtime variables.

Required in production:
- `DB_URL`
- `DB_USER`
- `DB_PASSWORD`
- `JWT_SECRET`
- `APP_STORAGE_SIGNING_SECRET`

Useful variables:
- `APP_PROFILE` (`postgres`)
- `APP_WEB_FRONTEND_URL` (where backend redirects `/`, `/home`, `/login`, `/register`, `/news`, `/professors`, `/app/**`)
- `APP_SEED_ENABLED` (`false` for production)
- `APP_STORAGE_ROOT`
- `APP_CORS_ALLOWED_ORIGINS`
- `FRONTEND_HOST_PORT`

## API

Detailed API routes and examples: `API_DOCUMENTATION.md`.

Migration tracking: `MIGRATION_FRONTEND_REACT.md`.
