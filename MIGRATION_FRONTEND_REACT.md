# Frontend Migration: Thymeleaf -> React

## Current State

- Backend already has full REST API namespace: `/api/v1/**`.
- Legacy Thymeleaf controllers and templates have been removed.
- Backend is now API-first and redirects browser hits for public SPA routes to the frontend app URL.
- React is the only web UI.

## What Is Already Covered By API

### Student flows (covered)

- Profile, schedule, journal, transcript, attendance
- Finance, holds, exam schedule
- Course registration + add/drop
- Requests + request messages
- Notifications, materials, files, checklist, mobility, clearance

API root: `/api/v1/student`

### Teacher flows (covered)

- Profile, sections, roster
- Attendance, components, grades, final grades
- Announcements, materials, student notes
- Grade change requests
- Upload to student files

API root: `/api/v1/teacher`

### Admin flows (covered)

- Users + permissions
- Terms, sections, meeting times, windows, enrollment override
- Exams, holds, finance invoices/payments
- Mobility, clearance, surveys, requests
- Grade change moderation, news, checklist templates, audit, stats

API root: `/api/v1/admin`

### Shared flows (covered)

- Auth: `/api/v1/auth/login`, `/refresh`, `/logout`
- File links/downloads: `/api/v1/files/**`

## Migration Steps Completed

1. Public API layer for unauthenticated pages:
- `GET /api/v1/public/news`
- `GET /api/v1/public/professors`
- `GET /api/v1/public/professors/{id}`

2. Registration endpoint for SPA signup flow:
- `POST /api/v1/auth/register`

3. Teacher grade export API:
- `GET /api/v1/teacher/sections/{sectionId}/grades/export`

4. Admin student status update API:
- `POST /api/v1/admin/students/{id}/status`

- Added `frontend/` React shell (Vite + TypeScript + Router + JWT login flow).
- Added public React pages: home, news, professors, professor profile, register.
- Extended role dashboards/pages for student, teacher, admin routes.
- Added React pages for student financial/files and admin requests.
- Added JWT auto-refresh handling in frontend API client (`401` -> refresh -> retry once).
- Added teacher workflow in React section page to upload files into student files.
- Added backend CORS config for separate frontend app:
  - property: `app.cors.allowed-origins`
  - default origins: `http://localhost:5173,http://localhost:3000`
- Enabled CORS handling in API security chain.
- Added missing API routes required for Thymeleaf removal.
- Removed Thymeleaf controllers, templates, CSRF web advice, and web-session filter.
- Removed Thymeleaf dependencies from `pom.xml`.
- Switched backend-generated links from legacy `/portal/**` routes to React `/app/**` routes.
- Added Docker service for React frontend with Nginx SPA routing and `/api` reverse-proxy.
