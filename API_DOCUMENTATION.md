# API Documentation - KBTU Portal (v1)

Base URL: `http://localhost:8080`
Swagger UI: `http://localhost:8080/swagger-ui/index.html`
OpenAPI JSON: `http://localhost:8080/v3/api-docs`

This document summarizes the current API surface under `/api/v1/**`.
For exact schemas, prefer Swagger as the source of truth.

## 1. API Roots

- `/api/v1/auth`
- `/api/v1/public`
- `/api/v1/student`
- `/api/v1/student/assistant`
- `/api/v1/teacher`
- `/api/v1/teacher/assistant`
- `/api/v1/admin`
- `/api/v1/chat`
- `/api/v1/files`

## 2. Authentication

### 2.1 Login

`POST /api/v1/auth/login`

Request:
```json
{
  "email": "a_mustafayev@kbtu.kz",
  "password": "student123"
}
```

Response:
```json
{
  "tokenType": "Bearer",
  "accessToken": "...",
  "accessTokenExpiresInSeconds": 43200,
  "refreshToken": "...",
  "refreshTokenExpiresInDays": 30,
  "role": "STUDENT",
  "permissions": []
}
```

### 2.2 Refresh

`POST /api/v1/auth/refresh`

```json
{
  "refreshToken": "..."
}
```

### 2.3 Logout

`POST /api/v1/auth/logout`

```json
{
  "refreshToken": "..."
}
```

### 2.4 Register

`POST /api/v1/auth/register`

```json
{
  "email": "a_newstudent@kbtu.kz",
  "password": "secret123",
  "confirmPassword": "secret123",
  "fullName": "New Student"
}
```

Role detection rules:
- student: `a_surname@kbtu.kz`
- professor: `a.surname@kbtu.kz`
- admin: explicit admin-style emails such as `admin@kbtu.kz`

### 2.5 Authorization Header

Protected routes require:

`Authorization: Bearer <accessToken>`

## 3. Common Behavior

### 3.1 Version Header

All `/api/v1/**` responses include:

`X-API-Version: v1`

### 3.2 Error Format

```json
{
  "code": "BAD_REQUEST",
  "message": "...",
  "details": {},
  "timestamp": "2026-03-18T...Z"
}
```

Typical codes:
- `VALIDATION_ERROR`
- `UNAUTHORIZED`
- `FORBIDDEN`
- `BAD_REQUEST`
- `STATE_CONFLICT`
- `INTERNAL_ERROR`

### 3.3 Pagination

Paginated endpoints return:

```json
{
  "items": [],
  "page": 0,
  "size": 20,
  "totalItems": 0,
  "totalPages": 0
}
```

Common query params:
- `page`
- `size`
- `sort`
- `direction`

## 4. Public API

Auth: none

| Method | Path | Description |
|---|---|---|
| GET | `/api/v1/public/news` | Public news feed |
| GET | `/api/v1/public/professors` | Public professor list |
| GET | `/api/v1/public/professors/{id}` | Public professor profile |

## 5. Student API

Auth: `STUDENT`

### 5.1 Profile and Academic Data

| Method | Path | Description |
|---|---|---|
| GET | `/api/v1/student/profile` | Student profile |
| POST | `/api/v1/student/profile-photo` | Upload student profile photo |
| GET | `/api/v1/student/schedule` | Weekly schedule |
| GET | `/api/v1/student/schedule/options` | Schedule semester filters |
| GET | `/api/v1/student/enrollments` | Enrollments list |
| GET | `/api/v1/student/enrollments/options` | Enrollment semester filters |
| GET | `/api/v1/student/journal` | Journal table |
| GET | `/api/v1/student/journal/options` | Journal semester filters |
| GET | `/api/v1/student/transcript` | Transcript and GPA summary |
| GET | `/api/v1/student/attendance` | Attendance summary and records |
| GET | `/api/v1/student/exam-schedule` | Exam schedule |

### 5.2 Registration Center

| Method | Path | Description |
|---|---|---|
| GET | `/api/v1/student/course-registration/overview` | Registration dashboard state |
| GET | `/api/v1/student/course-registration/catalog` | Full current-semester catalog |
| GET | `/api/v1/student/course-registration/available` | Sections currently available for registration/add |
| POST | `/api/v1/student/course-registration/submit` | Submit registration for section |
| POST | `/api/v1/student/add-drop/add` | Add section during add/drop |
| POST | `/api/v1/student/add-drop/drop` | Drop section during add/drop |
| GET | `/api/v1/student/fx` | FX overview |
| POST | `/api/v1/student/fx` | Create FX request |

Body for registration/add/drop/FX:
```json
{
  "sectionId": 1
}
```

### 5.3 Finance, Holds, Files

| Method | Path | Description |
|---|---|---|
| GET | `/api/v1/student/financial` | Charges, payments, balance |
| GET | `/api/v1/student/holds` | Active holds |
| GET | `/api/v1/student/files` | Student files with signed links |
| GET | `/api/v1/student/materials/{sectionId}` | Published section materials |

### 5.4 Requests and Student Services

| Method | Path | Description |
|---|---|---|
| GET | `/api/v1/student/checklist` | Checklist items |
| GET | `/api/v1/student/mobility` | Mobility applications |
| GET | `/api/v1/student/clearance` | Clearance sheet |
| GET | `/api/v1/student/requests` | Requests page |
| POST | `/api/v1/student/requests` | Create request |
| GET | `/api/v1/student/requests/{id}/messages` | Request thread |
| POST | `/api/v1/student/requests/{id}/messages` | Add message to request |

Request body examples:

Create request:
```json
{
  "category": "FINANCE",
  "description": "Need an invoice copy"
}
```

Add request message:
```json
{
  "message": "Please update status"
}
```

### 5.5 News and Notifications

| Method | Path | Description |
|---|---|---|
| GET | `/api/v1/student/news` | Student news feed |
| GET | `/api/v1/student/announcements` | Course announcements |
| GET | `/api/v1/student/notifications` | Notification center data |
| POST | `/api/v1/student/notifications/{id}/read` | Mark notification as read |
| POST | `/api/v1/student/notifications/read-all` | Mark all notifications as read |

### 5.6 Student AI Assistant

| Method | Path | Description |
|---|---|---|
| POST | `/api/v1/student/assistant/chat` | Student AI assistant |

Request:
```json
{
  "message": "How many points do I need on the final for Calculus II?"
}
```

## 6. Teacher API

Auth: `PROFESSOR`

### 6.1 Profile and Sections

| Method | Path | Description |
|---|---|---|
| GET | `/api/v1/teacher/profile` | Teacher profile |
| POST | `/api/v1/teacher/profile-photo` | Upload teacher profile photo |
| GET | `/api/v1/teacher/sections` | Teacher section list |
| GET | `/api/v1/teacher/sections/{sectionId}/roster` | Section roster |

### 6.2 Attendance and Grades

| Method | Path | Description |
|---|---|---|
| POST | `/api/v1/teacher/sections/{sectionId}/attendance` | Save attendance |
| GET | `/api/v1/teacher/sections/{sectionId}/components` | Assessment components |
| POST | `/api/v1/teacher/sections/{sectionId}/components` | Create assessment component |
| POST | `/api/v1/teacher/sections/{sectionId}/components/{componentId}/publish?published=true` | Publish/unpublish component |
| POST | `/api/v1/teacher/sections/{sectionId}/components/{componentId}/lock?locked=true` | Lock/unlock component |
| POST | `/api/v1/teacher/sections/{sectionId}/grades` | Save component grade |
| POST | `/api/v1/teacher/sections/{sectionId}/final-grades` | Save final grade |
| POST | `/api/v1/teacher/sections/{sectionId}/final-grades/{studentId}/publish` | Publish final grade |
| GET | `/api/v1/teacher/sections/{sectionId}/grades/export` | Export grades as XLSX |

### 6.3 Announcements, Materials, Notes

| Method | Path | Description |
|---|---|---|
| GET | `/api/v1/teacher/sections/{sectionId}/announcements` | Section announcements |
| POST | `/api/v1/teacher/sections/{sectionId}/announcements` | Create announcement |
| GET | `/api/v1/teacher/sections/{sectionId}/materials` | List materials |
| POST | `/api/v1/teacher/sections/{sectionId}/materials` | Upload course material |
| POST | `/api/v1/teacher/materials/{materialId}/visibility?published=true` | Publish/hide material |
| DELETE | `/api/v1/teacher/materials/{materialId}` | Delete material |
| GET | `/api/v1/teacher/sections/{sectionId}/student-notes` | Student notes |
| POST | `/api/v1/teacher/sections/{sectionId}/student-notes` | Upsert student note |
| POST | `/api/v1/teacher/sections/{sectionId}/student-files` | Upload file to student files |

Multipart routes:
- `POST /api/v1/teacher/sections/{sectionId}/materials`
  - `title`
  - `description` (optional)
  - `visibility` (`PUBLIC` or `ENROLLED_ONLY`)
  - `file`
- `POST /api/v1/teacher/sections/{sectionId}/student-files`
  - `studentId`
  - `file`

### 6.4 Grade Changes and Notifications

| Method | Path | Description |
|---|---|---|
| GET | `/api/v1/teacher/grade-change-requests` | Teacher grade change requests |
| POST | `/api/v1/teacher/sections/{sectionId}/grade-change-requests` | Create grade change request |
| GET | `/api/v1/teacher/notifications` | Notification center data |
| POST | `/api/v1/teacher/notifications/{id}/read` | Mark notification as read |
| POST | `/api/v1/teacher/notifications/read-all` | Mark all notifications as read |

### 6.5 Teacher AI Assistant

| Method | Path | Description |
|---|---|---|
| POST | `/api/v1/teacher/assistant/chat` | Teacher AI assistant |

Request:
```json
{
  "message": "Show me students at risk in my sections"
}
```

## 7. Admin API

Auth: `ADMIN`

Admin permissions:
- `SUPER`
- `REGISTRAR`
- `FINANCE`
- `SUPPORT`
- `CONTENT`
- `MOBILITY`

### 7.1 User and Core Reference Data

| Method | Path | Permission | Description |
|---|---|---|---|
| GET | `/api/v1/admin/users` | `SUPER` | List users |
| POST | `/api/v1/admin/users/{id}/permissions` | `SUPER` | Update admin permissions |
| GET | `/api/v1/admin/subjects` | `ADMIN role` | List subjects |
| GET | `/api/v1/admin/teachers` | `ADMIN role` | List teachers |
| GET | `/api/v1/admin/students` | `ADMIN role` | List students |
| POST | `/api/v1/admin/students/{id}/status` | `ADMIN role` | Update student status |
| GET | `/api/v1/admin/stats` | `ADMIN role` | Dashboard statistics |

### 7.2 Academic Setup and Registration Operations

| Method | Path | Permission | Description |
|---|---|---|---|
| POST | `/api/v1/admin/terms` | `REGISTRAR` | Create term |
| GET | `/api/v1/admin/terms` | `REGISTRAR` | List terms |
| POST | `/api/v1/admin/sections` | `REGISTRAR` | Create section |
| GET | `/api/v1/admin/sections` | `REGISTRAR` | List sections |
| POST | `/api/v1/admin/sections/{id}/assign-professor` | `REGISTRAR` | Assign professor |
| POST | `/api/v1/admin/sections/{id}/meeting-times` | `REGISTRAR` | Add meeting time |
| POST | `/api/v1/admin/windows` | `REGISTRAR` | Upsert registration window |
| GET | `/api/v1/admin/windows` | `ADMIN role` | List registration windows |
| POST | `/api/v1/admin/enrollments/override` | `REGISTRAR` | Manual enrollment override |
| GET | `/api/v1/admin/fx` | `ADMIN role` | FX queue |
| POST | `/api/v1/admin/fx/{id}/status` | `ADMIN role` | Update FX status |

### 7.3 Exams, Finance, Holds

| Method | Path | Permission | Description |
|---|---|---|---|
| GET | `/api/v1/admin/exams` | `REGISTRAR` | List exam sessions |
| POST | `/api/v1/admin/exams` | `REGISTRAR` | Create exam session |
| PUT | `/api/v1/admin/exams/{id}` | `REGISTRAR` | Update exam session |
| DELETE | `/api/v1/admin/exams/{id}` | `REGISTRAR` | Delete exam session |
| GET | `/api/v1/admin/holds` | `FINANCE` | List active holds |
| POST | `/api/v1/admin/holds` | `FINANCE` | Create hold |
| POST | `/api/v1/admin/holds/{id}/remove` | `FINANCE` | Remove hold |
| POST | `/api/v1/admin/finance/invoices` | `FINANCE` | Create invoice |
| POST | `/api/v1/admin/finance/payments` | `FINANCE` | Register payment |

### 7.4 Mobility, Clearance, Surveys, Requests

| Method | Path | Permission | Description |
|---|---|---|---|
| GET | `/api/v1/admin/mobility` | `MOBILITY` | List mobility applications |
| POST | `/api/v1/admin/mobility/{id}/status` | `MOBILITY` | Update mobility status |
| GET | `/api/v1/admin/clearance` | `MOBILITY` | List clearance sheets |
| POST | `/api/v1/admin/clearance/checkpoints/{id}/review` | `MOBILITY` | Review clearance checkpoint |
| GET | `/api/v1/admin/surveys` | `CONTENT` | List surveys |
| POST | `/api/v1/admin/surveys` | `CONTENT` | Create survey |
| POST | `/api/v1/admin/surveys/{id}/close` | `CONTENT` | Close survey |
| GET | `/api/v1/admin/surveys/{id}/responses` | `CONTENT` | Survey responses |
| GET | `/api/v1/admin/requests` | `SUPPORT` | List requests |
| POST | `/api/v1/admin/requests/{id}/assign` | `SUPPORT` | Assign request |
| POST | `/api/v1/admin/requests/{id}/status` | `SUPPORT` | Update request status |

### 7.5 Moderation, Checklist, Audit, Notifications

| Method | Path | Permission | Description |
|---|---|---|---|
| GET | `/api/v1/admin/grade-change-requests` | `REGISTRAR` | Grade change moderation queue |
| POST | `/api/v1/admin/grade-change-requests/{id}/review` | `REGISTRAR` | Review grade change request |
| POST | `/api/v1/admin/news` | `CONTENT` | Create news |
| GET | `/api/v1/admin/checklist-templates` | `REGISTRAR` | List checklist templates |
| POST | `/api/v1/admin/checklist-templates` | `REGISTRAR` | Create checklist template |
| POST | `/api/v1/admin/checklist/generate` | `REGISTRAR` | Generate checklist items |
| GET | `/api/v1/admin/audit` | `SUPER` | Audit log |
| GET | `/api/v1/admin/notifications` | `ADMIN role` | Notification center data |
| POST | `/api/v1/admin/notifications/{id}/read` | `ADMIN role` | Mark notification as read |
| POST | `/api/v1/admin/notifications/read-all` | `ADMIN role` | Mark all notifications as read |

## 8. Chat API

Auth: bearer token required

| Method | Path | Description |
|---|---|---|
| GET | `/api/v1/chat/rooms` | User chat rooms |
| POST | `/api/v1/chat/rooms/section/{sectionId}` | Get or create section room |
| POST | `/api/v1/chat/rooms/direct` | Get or create direct room |
| POST | `/api/v1/chat/rooms/group` | Create group room |
| GET | `/api/v1/chat/users` | Search users |
| GET | `/api/v1/chat/rooms/{roomId}/messages` | Paginated room messages |
| GET | `/api/v1/chat/rooms/{roomId}/members` | Room members |

Example direct room request:
```json
{
  "userId": 5
}
```

## 9. Files API

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/v1/files/asset/{id}/link` | Bearer | Signed asset link |
| GET | `/api/v1/files/material/{id}/link` | Bearer | Signed material link |
| GET | `/api/v1/files/download/asset/{id}?exp=...&sig=...` | No bearer | Asset download by signed URL |
| GET | `/api/v1/files/download/material/{id}?exp=...&sig=...` | No bearer | Material download by signed URL |

## 10. Real-Time Notifications

WebSocket endpoint:
- `/ws`

Notification destination used by the frontend:
- `/user/queue/notifications`

Current behavior:
- unread counters update live in the app sidebar
- notification center pages refresh automatically after new events

## 11. Quick cURL Examples

### Login

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"a_mustafayev@kbtu.kz","password":"student123"}'
```

### Student registration overview

```bash
curl http://localhost:8080/api/v1/student/course-registration/overview \
  -H "Authorization: Bearer <ACCESS_TOKEN>"
```

### Student assistant

```bash
curl -X POST http://localhost:8080/api/v1/student/assistant/chat \
  -H "Authorization: Bearer <ACCESS_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"message":"How many points do I need on the final for Calculus II?"}'
```

### Teacher uploads material

```bash
curl -X POST "http://localhost:8080/api/v1/teacher/sections/1/materials" \
  -H "Authorization: Bearer <ACCESS_TOKEN>" \
  -F "title=Week 1 slides" \
  -F "description=Intro" \
  -F "visibility=ENROLLED_ONLY" \
  -F "file=@./week1.pdf"
```

### Admin creates invoice

```bash
curl -X POST http://localhost:8080/api/v1/admin/finance/invoices \
  -H "Authorization: Bearer <ACCESS_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"studentId":1,"amount":150000,"description":"Tuition","dueDate":"2026-03-01"}'
```

## 12. Notes for Mobile Team

- Use only `/api/v1/**`.
- Public endpoints under `/api/v1/public/**` do not require bearer token.
- Expect `401`, `403`, and `409` as functional states.
- Use `/api/v1/auth/refresh` for token rotation.
- Use Swagger for exact DTO shapes.
- Real-time notifications are available through WebSocket if the mobile client wants live unread counters.
