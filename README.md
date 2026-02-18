# KBTU University Portal

A university portal with three roles and one shared domain model:
- `STUDENT`
- `PROFESSOR`
- `ADMIN` (with admin permissions)

The project includes:
- Web UI (Thymeleaf + session auth)
- Mobile API (`/api/v1/**`, JWT + refresh tokens)

## Tech Stack

- Java 17
- Spring Boot 4.0.2
- Spring Security
- Spring Data JPA + Hibernate
- Thymeleaf
- Flyway (PostgreSQL schema migrations)
- H2 (demo/dev)
- PostgreSQL (deployment)
- JWT (JJWT)
- Apache POI (Excel export)
- springdoc OpenAPI + Swagger UI
- Actuator
- Bucket4j (login rate limiting)

## Architecture

- `controller/` - web controllers (Thymeleaf pages)
- `controller/api/v1/` - mobile API v1 controllers
- `service/` - business logic and permissions
- `repository/` - JPA repositories
- `entity/` - domain entities
- `web/api/v1/` - API error and pagination model

Key design rule:
- `spring.jpa.open-in-view=false` is enabled.
- Data needed for response rendering is loaded explicitly via `...WithDetails` queries.

## Run

### Default profile (H2)

```bash
./mvnw spring-boot:run
```

App URL: `http://localhost:8080`

### PostgreSQL profile

```bash
$env:APP_PROFILE="postgres"
$env:DB_URL="jdbc:postgresql://localhost:5432/final_project"
$env:DB_USER="postgres"
$env:DB_PASSWORD="12345678"
./mvnw spring-boot:run
```

Notes:
- In `postgres` profile, Flyway is enabled and `ddl-auto=validate`.
- In `h2` profile, Flyway is disabled and `ddl-auto=create-drop`.

## Demo Accounts

| Role | Email | Password |
|---|---|---|
| Admin | `admin@kbtu.kz` | `admin123` |
| Professor | `z.professor@kbtu.kz` | `prof123` |
| Teacher Assistant | `t.assistant@kbtu.kz` | `ta12345` |
| Student | `a_mustafayev@kbtu.kz` | `student123` |

## Main Web Routes

- Public:
  - `/`
  - `/login`
  - `/register`
  - `/news`
  - `/professors`
  - `/professors/{id}`
- Student UI:
  - `/portal/{slug}`
  - `/portal/course-registration`
  - `/portal/add-drop-courses`
  - `/portal/student-requests/*`
- Professor UI:
  - `/professor/dashboard`
  - `/professor/courses`
  - `/professor/course/{id}`
  - `/professor/course/{id}/export-grades`
- Admin UI:
  - `/admin/dashboard` and `/admin/*`

## API Roots (What each root means)

Use these roots for mobile integration:

- `/api/v1/auth`
  - Login, refresh, logout.
  - No bearer token required for login/refresh/logout payload calls.

- `/api/v1/student`
  - Student self-service data and actions.
  - Requires `Authorization: Bearer <accessToken>` with role `STUDENT`.

- `/api/v1/teacher`
  - Teacher section workflows: attendance, gradebook, materials, announcements, grade changes.
  - Requires role `PROFESSOR`.

- `/api/v1/admin`
  - Back-office operations with permission checks.
  - Requires role `ADMIN` plus specific admin permission.

- `/api/v1/files`
  - File link/signature API.
  - `/link` endpoints require bearer token.
  - `/download/*` endpoints are public but require valid signed query params (`exp`, `sig`).

Security rules:
- Legacy session APIs are intentionally blocked:
  - `/api/admin/**`
  - `/api/professor/**`
  - `/api/student/**`
- Every `/api/v1/**` response includes header: `X-API-Version: v1`.

## API Docs

Detailed route list with request/response notes:
- See `API_DOCUMENTATION.md`

## Storage and Uploads

Config keys:
- `APP_STORAGE_ROOT` (default `./storage`)
- `APP_STORAGE_MAX_FILE_MB` (default `20`)
- `APP_STORAGE_ALLOWED_CONTENT_TYPES`
- `APP_STORAGE_LINK_TTL_MINUTES` (default `30`)
- `APP_STORAGE_SIGNING_SECRET`

Multipart limits:
- `APP_MULTIPART_MAX_FILE_SIZE` (default `25MB`)
- `APP_MULTIPART_MAX_REQUEST_SIZE` (default `25MB`)

## Build and Test

```bash
./mvnw clean package
./mvnw test
```

## Swagger and Monitoring

- Swagger UI: `/swagger-ui.html`
- OpenAPI JSON: `/v3/api-docs`
- Actuator health: `/actuator/health`
