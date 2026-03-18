# Frontend Migration: Thymeleaf -> React

## Current State

The migration is complete at the web layer.

- Legacy Thymeleaf controllers and templates have been removed.
- React is the only browser UI.
- Backend is API-first and serves `/api/v1/**` for web and mobile clients.
- Browser hits for public SPA routes are redirected to the frontend application URL.
- Protected application UI is now handled by React routes under `/app/**`.

## Frontend Route Coverage

### Public React pages

- `/`
- `/login`
- `/register`
- `/news`
- `/professors`
- `/professors/:id`

### Student React pages

- `/app/student`
- `/app/student/registration`
- `/app/student/notifications`
- `/app/student/schedule`
- `/app/student/enrollments`
- `/app/student/journal`
- `/app/student/transcript`
- `/app/student/attendance`
- `/app/student/exams`
- `/app/student/assistant`
- `/app/student/financial`
- `/app/student/files`
- `/app/student/news`
- `/app/student/requests`

### Teacher React pages

- `/app/teacher`
- `/app/teacher/assistant`
- `/app/teacher/notifications`
- `/app/teacher/sections`
- `/app/teacher/sections/:sectionId`
- `/app/teacher/attendance`
- `/app/teacher/gradebook`
- `/app/teacher/announcements`
- `/app/teacher/materials`
- `/app/teacher/notes`
- `/app/teacher/grade-changes`

### Admin React pages

- `/app/admin`
- `/app/admin/registration`
- `/app/admin/notifications`
- `/app/admin/users`
- `/app/admin/requests`
- `/app/admin/academic`
- `/app/admin/finance`
- `/app/admin/moderation`

### Shared React pages

- `/app/chat`

## API Coverage By Role

### Student API coverage

Covered by React UI:
- profile and profile photo
- registration center
- schedule and enrollment filters
- journal and transcript
- attendance and exam schedule
- finance and holds
- requests and request messages
- files and materials
- news and notifications
- AI assistant

API roots:
- `/api/v1/student`
- `/api/v1/student/assistant`

### Teacher API coverage

Covered by React UI:
- teacher profile and profile photo
- sections overview and section details
- roster
- attendance
- gradebook and final grades
- announcements
- materials
- student notes
- grade change requests
- notifications
- AI assistant
- upload to student files

API roots:
- `/api/v1/teacher`
- `/api/v1/teacher/assistant`

### Admin API coverage

Covered by React UI:
- dashboard stats
- registration operations
- academic setup
- finance
- moderation and news
- users
- requests
- notifications

API root:
- `/api/v1/admin`

### Shared coverage

- auth: `/api/v1/auth/**`
- public data: `/api/v1/public/**`
- chat: `/api/v1/chat/**`
- signed file downloads: `/api/v1/files/**`
- WebSocket endpoint: `/ws`
- live notification destination: `/user/queue/notifications`

## Migration Work Completed

1. Removed legacy Thymeleaf web layer from the application.
2. Kept backend business logic behind REST controllers and services.
3. Added React app under `frontend/` with role-protected routes.
4. Added JWT login, refresh, logout, and protected route handling in the frontend.
5. Added public React pages for home, news, professors, and registration.
6. Added student workflows for academic and service modules.
7. Added teacher workflows for course delivery and moderation tasks.
8. Added admin workflows for academic, finance, support, and registration operations.
9. Added Gemini-powered student and teacher assistants.
10. Added notification centers for all roles.
11. Added live unread badges and WebSocket-driven notification refresh.
12. Added Docker frontend service with SPA-friendly Nginx routing.
13. Kept backend CORS and frontend URL configuration externalized through env variables.

## Runtime Notes

### Frontend dev mode

- Vite dev server default: `http://localhost:5173`
- Backend default: `http://localhost:8080`

### Docker mode

- Frontend default: `http://localhost:3000`
- Backend default: `http://localhost:8080`

### Env/config that matter for the React split

- `APP_WEB_FRONTEND_URL`
- `APP_CORS_ALLOWED_ORIGINS`
- `VITE_API_BASE_URL`
- `APP_SEED_ENABLED`

## Result

The project is no longer a mixed Thymeleaf app.

It now runs as:
- Spring Boot backend API
- React frontend SPA
- PostgreSQL database
- WebSocket support for chat and live notification updates
