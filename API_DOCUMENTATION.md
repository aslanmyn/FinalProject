# API Documentation - KBTU Portal (v1)

Base URL: `http://localhost:8080`

This document describes the mobile API under `/api/v1/**`.

## 1. API Roots

- `/api/v1/auth` - authentication and token lifecycle
- `/api/v1/student` - student cabinet API
- `/api/v1/teacher` - teacher cabinet API
- `/api/v1/admin` - admin back-office API
- `/api/v1/files` - secure/signed file download API

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

Request:
```json
{
  "refreshToken": "..."
}
```

Returns a rotated refresh token and new access token.

### 2.3 Logout

`POST /api/v1/auth/logout`

Request:
```json
{
  "refreshToken": "..."
}
```

Revokes the refresh token.

### 2.4 Authorization Header

For protected routes:

`Authorization: Bearer <accessToken>`

## 3. Common API Behavior

### 3.1 Version Header

All `/api/v1/**` responses include:

`X-API-Version: v1`

### 3.2 Pagination

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
- `page` (default `0`)
- `size` (default `20`)
- `sort` (optional)
- `direction` (`asc` or `desc`, default `desc`)

### 3.3 Error Format

```json
{
  "code": "BAD_REQUEST",
  "message": "...",
  "details": {},
  "timestamp": "2026-02-18T...Z"
}
```

Possible `code` values:
- `VALIDATION_ERROR`
- `UNAUTHORIZED`
- `FORBIDDEN`
- `BAD_REQUEST`
- `STATE_CONFLICT`
- `INTERNAL_ERROR`

## 4. Student API (`/api/v1/student`)

Auth: `STUDENT`

| Method | Path | Description |
|---|---|---|
| GET | `/api/v1/student/profile` | Student profile |
| GET | `/api/v1/student/schedule` | Personal schedule |
| GET | `/api/v1/student/journal` | Published component grades |
| GET | `/api/v1/student/transcript` | Published final grades + GPA summary |
| GET | `/api/v1/student/attendance` | Attendance records + summary |
| GET | `/api/v1/student/financial` | Charges, payments, balance, financial hold |
| GET | `/api/v1/student/holds` | Active holds |
| GET | `/api/v1/student/exam-schedule` | Exam schedule for current enrollments |
| GET | `/api/v1/student/news` | News feed |
| GET | `/api/v1/student/announcements` | Course announcements (paginated) |
| GET | `/api/v1/student/notifications` | Notifications + unread count |
| POST | `/api/v1/student/notifications/{id}/read` | Mark notification as read |
| GET | `/api/v1/student/materials/{sectionId}` | Published materials for enrolled section |
| GET | `/api/v1/student/files` | Student files with signed download URL |
| GET | `/api/v1/student/checklist` | Checklist items |
| GET | `/api/v1/student/mobility` | Mobility applications |
| GET | `/api/v1/student/clearance` | Clearance sheet |
| GET | `/api/v1/student/enrollments` | Enrollment list |
| GET | `/api/v1/student/course-registration/available` | Sections available for add/register |
| POST | `/api/v1/student/course-registration/submit` | Register for section |
| POST | `/api/v1/student/add-drop/add` | Add section |
| POST | `/api/v1/student/add-drop/drop` | Drop section |
| GET | `/api/v1/student/requests` | Student requests (paginated) |
| POST | `/api/v1/student/requests` | Create request |
| GET | `/api/v1/student/requests/{id}/messages` | Request thread |
| POST | `/api/v1/student/requests/{id}/messages` | Add request message |

Request body snippets:
- `POST /course-registration/submit`, `/add-drop/add`, `/add-drop/drop`
```json
{ "sectionId": 1 }
```
- `POST /requests`
```json
{ "category": "FINANCE", "description": "Need invoice copy" }
```
- `POST /requests/{id}/messages`
```json
{ "message": "Please update status" }
```

## 5. Teacher API (`/api/v1/teacher`)

Auth: `PROFESSOR`

| Method | Path | Description |
|---|---|---|
| GET | `/api/v1/teacher/profile` | Teacher profile |
| GET | `/api/v1/teacher/sections` | Teacher sections |
| GET | `/api/v1/teacher/sections/{sectionId}/roster` | Section roster |
| POST | `/api/v1/teacher/sections/{sectionId}/attendance` | Mark attendance |
| GET | `/api/v1/teacher/sections/{sectionId}/components` | Assessment components |
| POST | `/api/v1/teacher/sections/{sectionId}/components` | Create assessment component |
| POST | `/api/v1/teacher/sections/{sectionId}/components/{componentId}/publish?published=true` | Publish/unpublish component |
| POST | `/api/v1/teacher/sections/{sectionId}/components/{componentId}/lock?locked=true` | Lock/unlock component |
| POST | `/api/v1/teacher/sections/{sectionId}/grades` | Save student grade |
| POST | `/api/v1/teacher/sections/{sectionId}/final-grades` | Save final grade |
| POST | `/api/v1/teacher/sections/{sectionId}/final-grades/{studentId}/publish` | Publish final grade |
| GET | `/api/v1/teacher/sections/{sectionId}/announcements` | Section announcements |
| POST | `/api/v1/teacher/sections/{sectionId}/announcements` | Create announcement |
| GET | `/api/v1/teacher/sections/{sectionId}/materials` | List materials |
| POST | `/api/v1/teacher/sections/{sectionId}/materials` | Upload material (multipart) |
| POST | `/api/v1/teacher/materials/{materialId}/visibility?published=true` | Publish/hide material |
| DELETE | `/api/v1/teacher/materials/{materialId}` | Delete material |
| GET | `/api/v1/teacher/sections/{sectionId}/student-notes` | Internal student notes |
| POST | `/api/v1/teacher/sections/{sectionId}/student-notes` | Upsert student note |
| GET | `/api/v1/teacher/grade-change-requests` | Teacher grade change requests |
| POST | `/api/v1/teacher/sections/{sectionId}/grade-change-requests` | Create grade change request |
| POST | `/api/v1/teacher/sections/{sectionId}/student-files` | Upload file to student files (multipart) |

Multipart routes:

### Upload course material
`POST /api/v1/teacher/sections/{sectionId}/materials`
- `Content-Type: multipart/form-data`
- Fields:
  - `title` (string)
  - `description` (string, optional)
  - `visibility` (`PUBLIC` or `ENROLLED_ONLY`)
  - `file` (binary)

### Upload student file
`POST /api/v1/teacher/sections/{sectionId}/student-files`
- `Content-Type: multipart/form-data`
- Fields:
  - `studentId` (number)
  - `file` (binary)

## 6. Admin API (`/api/v1/admin`)

Auth: `ADMIN`

Permission model:
- `SUPER`
- `REGISTRAR`
- `FINANCE`
- `SUPPORT`
- `CONTENT`
- `MOBILITY`

| Method | Path | Permission | Description |
|---|---|---|---|
| GET | `/api/v1/admin/users` | `SUPER` | List users (paginated) |
| POST | `/api/v1/admin/users/{id}/permissions` | `SUPER` | Set admin permissions |
| POST | `/api/v1/admin/terms` | `REGISTRAR` | Create academic term |
| GET | `/api/v1/admin/terms` | `REGISTRAR` | List terms |
| POST | `/api/v1/admin/sections` | `REGISTRAR` | Create section |
| GET | `/api/v1/admin/sections` | `REGISTRAR` | List sections (`semesterId` optional) |
| POST | `/api/v1/admin/sections/{id}/assign-professor` | `REGISTRAR` | Assign professor |
| POST | `/api/v1/admin/sections/{id}/meeting-times` | `REGISTRAR` | Add meeting time |
| POST | `/api/v1/admin/windows` | `REGISTRAR` | Upsert registration window |
| POST | `/api/v1/admin/enrollments/override` | `REGISTRAR` | Enrollment override |
| GET | `/api/v1/admin/exams` | `REGISTRAR` | List exams (`semesterId` optional) |
| POST | `/api/v1/admin/exams` | `REGISTRAR` | Create exam session |
| PUT | `/api/v1/admin/exams/{id}` | `REGISTRAR` | Update exam session |
| DELETE | `/api/v1/admin/exams/{id}` | `REGISTRAR` | Delete exam session |
| GET | `/api/v1/admin/holds` | `FINANCE` | List active holds |
| POST | `/api/v1/admin/holds` | `FINANCE` | Create hold |
| POST | `/api/v1/admin/holds/{id}/remove` | `FINANCE` | Remove hold |
| POST | `/api/v1/admin/finance/invoices` | `FINANCE` | Create invoice |
| POST | `/api/v1/admin/finance/payments` | `FINANCE` | Register payment |
| GET | `/api/v1/admin/mobility` | `MOBILITY` | List mobility applications |
| POST | `/api/v1/admin/mobility/{id}/status` | `MOBILITY` | Update mobility status |
| GET | `/api/v1/admin/clearance` | `MOBILITY` | List clearance sheets |
| POST | `/api/v1/admin/clearance/checkpoints/{id}/review` | `MOBILITY` | Review clearance checkpoint |
| GET | `/api/v1/admin/surveys` | `CONTENT` | List surveys |
| POST | `/api/v1/admin/surveys` | `CONTENT` | Create survey |
| POST | `/api/v1/admin/surveys/{id}/close` | `CONTENT` | Close survey |
| GET | `/api/v1/admin/surveys/{id}/responses` | `CONTENT` | Export survey responses |
| GET | `/api/v1/admin/requests` | `SUPPORT` | List requests (paginated) |
| POST | `/api/v1/admin/requests/{id}/assign` | `SUPPORT` | Assign request |
| POST | `/api/v1/admin/requests/{id}/status` | `SUPPORT` | Update request status |
| GET | `/api/v1/admin/grade-change-requests` | `REGISTRAR` | List pending grade changes (paginated) |
| POST | `/api/v1/admin/grade-change-requests/{id}/review` | `REGISTRAR` | Review grade change request |
| POST | `/api/v1/admin/news` | `CONTENT` | Create news |
| GET | `/api/v1/admin/checklist-templates` | `REGISTRAR` | List checklist templates |
| POST | `/api/v1/admin/checklist-templates` | `REGISTRAR` | Create checklist template |
| POST | `/api/v1/admin/checklist/generate` | `REGISTRAR` | Generate checklist items |
| GET | `/api/v1/admin/audit` | `SUPER` | Audit log (paginated) |
| GET | `/api/v1/admin/stats` | `ADMIN role` | High-level statistics |

## 7. Files API (`/api/v1/files`)

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/v1/files/asset/{id}/link` | Bearer | Get signed download URL for file asset |
| GET | `/api/v1/files/material/{id}/link` | Bearer | Get signed download URL for material |
| GET | `/api/v1/files/download/asset/{id}?exp=...&sig=...` | No bearer | Download by signed link |
| GET | `/api/v1/files/download/material/{id}?exp=...&sig=...` | No bearer | Download by signed link |

Signed URLs are time-limited and validated by `exp` + `sig`.

## 8. Quick cURL Examples

### Login

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"a_mustafayev@kbtu.kz","password":"student123"}'
```

### Student profile

```bash
curl http://localhost:8080/api/v1/student/profile \
  -H "Authorization: Bearer <ACCESS_TOKEN>"
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

## 9. Notes for Mobile Team

- Use only `/api/v1/**`.
- Expect `X-API-Version: v1` in responses.
- Handle `401`, `403`, and `409` as functional states.
- Refresh access tokens via `/api/v1/auth/refresh` before expiration.
