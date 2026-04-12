# Frontend Migration: Thymeleaf -> React

## Current State

The browser migration is complete.

- Legacy Thymeleaf pages are no longer the active web UI.
- React is the main frontend for browser use.
- Backend is API-first and serves `/api/v1/**` for web and mobile clients.
- Public browser traffic goes to the React app.
- Protected application UI is handled by React routes under `/app/**`.

## Frontend Route Coverage

### Public React pages

- `/`
- `/login`
- `/register`
- `/news`
- `/professors`
- `/professors/:id`

### Student React pages

Core:
- `/app/student`
- `/app/student/registration`
- `/app/student/notifications`
- `/app/student/planner`
- `/app/student/workflows`
- `/app/student/schedule`
- `/app/student/enrollments`
- `/app/student/journal`
- `/app/student/sections/:sectionId`
- `/app/student/transcript`
- `/app/student/attendance`
- `/app/student/exams`
- `/app/student/assistant`
- `/app/student/financial`
- `/app/student/files`
- `/app/student/news`
- `/app/student/requests`

Campus life:
- `/app/student/dorm`
- `/app/student/food`
- `/app/student/campus-map`
- `/app/student/laundry`

### Teacher React pages

- `/app/teacher`
- `/app/teacher/assistant`
- `/app/teacher/notifications`
- `/app/teacher/risk`
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
- `/app/admin/analytics`
- `/app/admin/assistant`
- `/app/admin/workflows`
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
- student section detail page with grades, attendance, exam, announcements, and materials
- schedule and enrollment filters
- journal and transcript
- attendance and self check-in attendance
- exam schedule
- finance and holds
- requests and request messages
- checklist, mobility, clearance
- files and materials
- news and notifications
- planner and workflows
- AI assistant
- dorm
- food ordering
- campus map
- laundry

API roots used by student UI:
- `/api/v1/student/**`
- `/api/v1/student/assistant/**`
- `/api/v1/student/dorm/**`
- `/api/v1/student/food/**`
- `/api/v1/student/campus-map/**`
- `/api/v1/student/laundry/**`

### Teacher API coverage

Covered by React UI:
- teacher profile and profile photo
- sections overview and section details
- roster
- live attendance flow
- attendance override and close flow
- gradebook and final grades
- announcements
- materials
- student notes
- grade change requests
- notifications
- risk dashboard
- AI assistant
- upload to student files

API roots used by teacher UI:
- `/api/v1/teacher/**`
- `/api/v1/teacher/assistant/**`

### Admin API coverage

Covered by React UI:
- dashboard stats
- analytics
- workflows
- AI assistant
- registration operations
- academic setup
- finance
- moderation and news
- users and permissions
- requests
- notifications

Admin academic setup now includes backend support for:
- create subject
- update subject
- create teacher
- update teacher

Admin student management now includes backend support for:
- create student
- update student
- update student status

API roots used by admin UI:
- `/api/v1/admin/**`

### Shared coverage

- auth: `/api/v1/auth/**`
- public data: `/api/v1/public/**`
- chat: `/api/v1/chat/**`
- signed file downloads: `/api/v1/files/**`
- WebSocket endpoint: `/ws`
- live notifications: `/user/queue/notifications`
- live attendance events for students and teachers

## Migration Work Completed

1. Removed the old Thymeleaf browser layer as the primary UI.
2. Kept backend business logic behind REST controllers and services.
3. Added React app under `frontend/` with role-protected routes.
4. Added JWT login, refresh, logout, and protected route handling in frontend.
5. Added public React pages for home, news, professors, and registration.
6. Added complete student academic and service flows.
7. Added a student section detail page backed by an aggregated academic endpoint.
8. Added teacher workflows for course delivery, attendance, and moderation.
9. Added admin workflows for academic operations, subject and teacher management, finance, support, and analytics.
10. Added student, teacher, and admin AI assistants.
11. Added notification centers for all roles.
12. Added live unread badges and WebSocket-driven notification refresh.
13. Added live student self check-in attendance flow with teacher control.
14. Added campus life modules: dorm, food, campus map, laundry.
15. Added Docker frontend service with SPA-friendly routing.
16. Kept backend CORS and frontend URL configuration externalized through env variables.

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
- `APP_SEED_CONFIRM_TOKEN`

## Demo Data Note

Frontend migration does not mean demo data ships with the repository.

Important:
- local PostgreSQL data is not stored in git
- `.env.local` is not stored in git
- `seed-users.local.txt` is not stored in git
- local uploaded files are not stored in git

So another developer can clone the repository and still have an empty or fresh database unless they:
- run the guarded demo seed on a fresh DB
- or restore a PostgreSQL dump

## Result

The project now runs as:
- Spring Boot backend API
- React frontend SPA
- PostgreSQL database
- WebSocket support for chat, live notifications, and live attendance events
- AI-assisted student, teacher, and admin workflows
