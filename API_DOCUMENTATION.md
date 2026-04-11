# API Documentation - KBTU Portal (v1)

Base URL: `http://localhost:8080`
Swagger UI: `http://localhost:8080/swagger-ui/index.html`
OpenAPI JSON: `http://localhost:8080/v3/api-docs`

This document summarizes the current API surface under `/api/v1/**`.
For exact schemas and field-level DTO details, Swagger is the source of truth.

## 1. API Roots

- `/api/v1/auth`
- `/api/v1/public`
- `/api/v1/student`
- `/api/v1/student/assistant`
- `/api/v1/student/dorm`
- `/api/v1/student/food`
- `/api/v1/student/campus-map`
- `/api/v1/student/laundry`
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
  "email": "a_testov@kbtu.kz",
  "password": "student123",
  "confirmPassword": "student123",
  "fullName": "Aslan Testov"
}
```

Role detection rules:
- student: `a_surname@kbtu.kz`
- professor: `a.surname@kbtu.kz`
- admin: admin-like emails such as `admin@kbtu.kz`

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
  "timestamp": "2026-04-06T...Z"
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

### 5.1 Profile

| Method | Path | Description |
|---|---|---|
| GET | `/api/v1/student/profile` | Student profile |
| PUT | `/api/v1/student/profile` | Update student-managed contact fields such as phone, address, emergency contact |
| POST | `/api/v1/student/profile-photo` | Upload student profile photo |

Update profile body:
```json
{
  "phone": "+77010000000",
  "address": "Almaty, Satpayev street 22",
  "emergencyContact": "Ainur Mustafayeva, +77020000000"
}
```

### 5.2 Academic Data

| Method | Path | Description |
|---|---|---|
| GET | `/api/v1/student/schedule` | Weekly schedule |
| GET | `/api/v1/student/schedule/options` | Schedule semester filters |
| GET | `/api/v1/student/journal` | Journal table |
| GET | `/api/v1/student/journal/options` | Journal semester filters |
| GET | `/api/v1/student/transcript` | Transcript and GPA summary |
| GET | `/api/v1/student/attendance` | Attendance summary and records |
| GET | `/api/v1/student/attendance/active` | Active self check-in attendance sessions |
| POST | `/api/v1/student/attendance-sessions/{sessionId}/check-in` | Self check-in to attendance session |
| GET | `/api/v1/student/exam-schedule` | Exam schedule |

Attendance self check-in body:
```json
{
  "code": "OPTIONAL_IF_MODE_IS_CODE"
}
```

### 5.3 Registration, Enrollments, FX, Finance

| Method | Path | Description |
|---|---|---|
| GET | `/api/v1/student/enrollments` | Enrollments list |
| GET | `/api/v1/student/enrollments/options` | Enrollment semester filters |
| GET | `/api/v1/student/course-registration/overview` | Registration dashboard state |
| GET | `/api/v1/student/course-registration/catalog` | Full current-semester catalog |
| GET | `/api/v1/student/course-registration/available` | Sections available for registration/add |
| POST | `/api/v1/student/course-registration/submit` | Submit registration for section |
| POST | `/api/v1/student/add-drop/add` | Add section during add/drop |
| POST | `/api/v1/student/add-drop/drop` | Drop section during add/drop |
| GET | `/api/v1/student/fx` | FX overview |
| POST | `/api/v1/student/fx` | Create FX request |
| GET | `/api/v1/student/financial` | Charges, payments, balance, hold flag |
| GET | `/api/v1/student/holds` | Active holds |

Body for registration/add/drop/FX:
```json
{
  "sectionId": 8
}
```

### 5.4 Services and Communication

| Method | Path | Description |
|---|---|---|
| GET | `/api/v1/student/checklist` | Checklist items |
| GET | `/api/v1/student/mobility` | Mobility applications |
| GET | `/api/v1/student/clearance` | Clearance sheet |
| GET | `/api/v1/student/requests` | Requests page |
| POST | `/api/v1/student/requests` | Create request |
| GET | `/api/v1/student/requests/{id}/messages` | Request thread |
| POST | `/api/v1/student/requests/{id}/messages` | Add message to request |
| GET | `/api/v1/student/news` | Student news feed |
| GET | `/api/v1/student/announcements` | Course announcements |
| GET | `/api/v1/student/notifications` | Notification center data |
| POST | `/api/v1/student/notifications/{id}/read` | Mark notification as read |
| POST | `/api/v1/student/notifications/read-all` | Mark all notifications as read |
| GET | `/api/v1/student/files` | Student files with signed links |
| GET | `/api/v1/student/materials/{sectionId}` | Published section materials |

Create request body:
```json
{
  "category": "FINANCE",
  "description": "Need an invoice copy"
}
```

Add request message body:
```json
{
  "message": "Please update status"
}
```

### 5.5 Analytics and AI

| Method | Path | Description |
|---|---|---|
| GET | `/api/v1/student/analytics/risk` | Risk dashboard |
| GET | `/api/v1/student/planner` | GPA/final planner dashboard |
| POST | `/api/v1/student/planner/simulate` | Simulate GPA/planner outcome |
| GET | `/api/v1/student/workflows` | Student workflow overview |
| POST | `/api/v1/student/assistant/chat` | Student AI assistant |

Student assistant request:
```json
{
  "message": "How many points do I need on the final for Calculus II?"
}
```

Planner simulation request:
```json
{
  "projectedFinals": [
    { "sectionId": 8, "projectedFinalScore": 35.0 },
    { "sectionId": 9, "projectedFinalScore": 32.0 }
  ]
}
```

### 5.6 Campus Life

| Method | Path | Description |
|---|---|---|
| GET | `/api/v1/student/dorm/buildings` | Dorm buildings |
| GET | `/api/v1/student/dorm/rooms` | Available dorm rooms |
| GET | `/api/v1/student/dorm/buildings/{buildingId}/rooms` | Rooms by dorm building |
| GET | `/api/v1/student/dorm/applications` | Student dorm applications |
| GET | `/api/v1/student/dorm/applications/{id}` | Dorm application details |
| POST | `/api/v1/student/dorm/applications` | Create dorm application |
| PUT | `/api/v1/student/dorm/applications/{id}/step1` | Update dorm application step 1 |
| PUT | `/api/v1/student/dorm/applications/{id}/step2` | Update dorm application step 2 |
| PUT | `/api/v1/student/dorm/applications/{id}/step3` | Update dorm application step 3 |
| POST | `/api/v1/student/dorm/applications/{id}/submit` | Submit dorm application |
| POST | `/api/v1/student/dorm/applications/{id}/cancel` | Cancel dorm application |
| GET | `/api/v1/student/food/categories` | Food categories |
| GET | `/api/v1/student/food/items` | Food menu items |
| GET | `/api/v1/student/food/items/popular` | Popular food items |
| GET | `/api/v1/student/food/orders` | Student food orders |
| GET | `/api/v1/student/food/orders/{id}` | Food order details |
| POST | `/api/v1/student/food/orders` | Create food order |
| POST | `/api/v1/student/food/orders/{id}/cancel` | Cancel food order |
| GET | `/api/v1/student/campus-map/buildings` | Campus buildings |
| GET | `/api/v1/student/campus-map/buildings/{id}` | Building details |
| GET | `/api/v1/student/campus-map/buildings/search` | Search buildings |
| GET | `/api/v1/student/campus-map/buildings/{buildingId}/rooms` | Rooms by building |
| GET | `/api/v1/student/campus-map/rooms/{id}` | Room details |
| GET | `/api/v1/student/campus-map/rooms/search` | Search rooms |
| GET | `/api/v1/student/campus-map/navigate` | Navigation route |
| GET | `/api/v1/student/laundry/rooms` | Laundry rooms |
| GET | `/api/v1/student/laundry/rooms/{roomId}/availability` | Laundry room availability |
| GET | `/api/v1/student/laundry/rooms/{roomId}/machines` | Laundry machines |
| GET | `/api/v1/student/laundry/bookings` | Student laundry bookings |
| POST | `/api/v1/student/laundry/bookings` | Book laundry machine |
| POST | `/api/v1/student/laundry/bookings/{id}/cancel` | Cancel laundry booking |

Food order request example:
```json
{
  "items": [
    { "itemId": 1, "quantity": 2 },
    { "itemId": 4, "quantity": 1 }
  ],
  "note": "No onions",
  "pickupTime": "12:30"
}
```

Laundry booking request example:
```json
{
  "machineId": 1,
  "startTime": "2026-04-06T18:00:00",
  "durationMinutes": 90
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
| GET | `/api/v1/teacher/analytics/risk` | Teacher risk dashboard |
| POST | `/api/v1/teacher/assistant/chat` | Teacher AI assistant |

### 6.2 Attendance

| Method | Path | Description |
|---|---|---|
| POST | `/api/v1/teacher/sections/{sectionId}/attendance` | Save manual attendance snapshot |
| GET | `/api/v1/teacher/sections/{sectionId}/attendance/active` | Get active attendance session |
| POST | `/api/v1/teacher/sections/{sectionId}/attendance/open` | Open attendance session |
| POST | `/api/v1/teacher/attendance-sessions/{sessionId}/close` | Close attendance session |
| GET | `/api/v1/teacher/attendance-sessions/{sessionId}/records` | Attendance roster for session |
| PUT | `/api/v1/teacher/attendance-sessions/{sessionId}/students/{studentId}` | Override attendance status |

### 6.3 Grades, Components, Materials

| Method | Path | Description |
|---|---|---|
| GET | `/api/v1/teacher/sections/{sectionId}/components` | Assessment components |
| POST | `/api/v1/teacher/sections/{sectionId}/components` | Create assessment component |
| POST | `/api/v1/teacher/sections/{sectionId}/components/{componentId}/publish?published=true` | Publish/unpublish component |
| POST | `/api/v1/teacher/sections/{sectionId}/components/{componentId}/lock?locked=true` | Lock/unlock component |
| POST | `/api/v1/teacher/sections/{sectionId}/grades` | Save component grade |
| POST | `/api/v1/teacher/sections/{sectionId}/final-grades` | Save final grade |
| POST | `/api/v1/teacher/sections/{sectionId}/final-grades/{studentId}/publish` | Publish final grade |
| GET | `/api/v1/teacher/sections/{sectionId}/grades/export` | Export grades as XLSX |
| GET | `/api/v1/teacher/sections/{sectionId}/announcements` | Section announcements |
| POST | `/api/v1/teacher/sections/{sectionId}/announcements` | Create announcement |
| GET | `/api/v1/teacher/sections/{sectionId}/materials` | List materials |
| POST | `/api/v1/teacher/sections/{sectionId}/materials` | Upload material |
| POST | `/api/v1/teacher/materials/{materialId}/visibility?published=true` | Publish/hide material |
| DELETE | `/api/v1/teacher/materials/{materialId}` | Delete material |
| GET | `/api/v1/teacher/sections/{sectionId}/student-notes` | Student notes |
| POST | `/api/v1/teacher/sections/{sectionId}/student-notes` | Upsert student note |
| POST | `/api/v1/teacher/sections/{sectionId}/student-files` | Upload file to student files |
| GET | `/api/v1/teacher/grade-change-requests` | Teacher grade change requests |
| POST | `/api/v1/teacher/sections/{sectionId}/grade-change-requests` | Create grade change request |
| GET | `/api/v1/teacher/notifications` | Notification center |
| POST | `/api/v1/teacher/notifications/{id}/read` | Mark notification as read |
| POST | `/api/v1/teacher/notifications/read-all` | Mark all notifications as read |

## 7. Admin API

Auth: `ADMIN`

Admin permission model:
- `SUPER`
- `REGISTRAR`
- `FINANCE`
- `SUPPORT`
- `CONTENT`
- `MOBILITY`

### 7.1 Core Admin Operations

| Method | Path | Permission | Description |
|---|---|---|---|
| GET | `/api/v1/admin/users` | `SUPER` | List users |
| POST | `/api/v1/admin/users/{id}/permissions` | `SUPER` | Update admin permissions |
| GET | `/api/v1/admin/subjects` | `ADMIN role` | List subjects with program/faculty context |
| GET | `/api/v1/admin/teachers` | `ADMIN role` | List teachers |
| GET | `/api/v1/admin/students` | `ADMIN role` | List students |
| GET | `/api/v1/admin/stats` | `ADMIN role` | Dashboard statistics |
| GET | `/api/v1/admin/notifications` | `ADMIN role` | Notifications |
| POST | `/api/v1/admin/notifications/{id}/read` | `ADMIN role` | Mark notification as read |
| POST | `/api/v1/admin/notifications/read-all` | `ADMIN role` | Mark all notifications as read |

### 7.2 Academic, Subject, Teacher, and Student Management

| Method | Path | Permission | Description |
|---|---|---|---|
| POST | `/api/v1/admin/terms` | `REGISTRAR` | Create term |
| GET | `/api/v1/admin/terms` | `REGISTRAR` | List terms |
| GET | `/api/v1/admin/subjects/{id}` | `REGISTRAR` | Get subject details |
| POST | `/api/v1/admin/subjects` | `REGISTRAR` | Create subject |
| PUT | `/api/v1/admin/subjects/{id}` | `REGISTRAR` | Update subject |
| GET | `/api/v1/admin/teachers/{id}` | `REGISTRAR` | Get teacher details |
| POST | `/api/v1/admin/teachers` | `REGISTRAR` | Create user + teacher profile |
| PUT | `/api/v1/admin/teachers/{id}` | `REGISTRAR` | Update user + teacher profile |
| POST | `/api/v1/admin/sections` | `REGISTRAR` | Create section |
| GET | `/api/v1/admin/sections` | `REGISTRAR` | List sections |
| POST | `/api/v1/admin/sections/{id}/assign-professor` | `REGISTRAR` | Assign professor |
| POST | `/api/v1/admin/sections/{id}/meeting-times` | `REGISTRAR` | Add meeting time |
| POST | `/api/v1/admin/windows` | `REGISTRAR` | Upsert registration window |
| GET | `/api/v1/admin/windows` | `REGISTRAR` | List windows |
| POST | `/api/v1/admin/enrollments/override` | `REGISTRAR` | Manual enrollment override |
| GET | `/api/v1/admin/fx` | `REGISTRAR` | FX queue |
| POST | `/api/v1/admin/fx/{id}/status` | `REGISTRAR` | Update FX status |
| GET | `/api/v1/admin/exams` | `REGISTRAR` | List exam sessions |
| POST | `/api/v1/admin/exams` | `REGISTRAR` | Create exam session |
| PUT | `/api/v1/admin/exams/{id}` | `REGISTRAR` | Update exam session |
| DELETE | `/api/v1/admin/exams/{id}` | `REGISTRAR` | Delete exam session |
| POST | `/api/v1/admin/grade-change-requests/{id}/review` | `REGISTRAR` | Review grade change request |
| GET | `/api/v1/admin/grade-change-requests` | `REGISTRAR` | Grade change queue |
| POST | `/api/v1/admin/students` | `REGISTRAR` | Create user + student profile |
| PUT | `/api/v1/admin/students/{id}` | `REGISTRAR` | Update user + student profile |
| POST | `/api/v1/admin/students/{id}/status` | `REGISTRAR` | Update student status only |

Create subject request example:
```json
{
  "code": "CSCI2104",
  "name": "Databases",
  "credits": 4,
  "programId": 1
}
```

Update subject request example:
```json
{
  "code": "CSCI2104",
  "name": "Advanced Databases",
  "credits": 5,
  "programId": 1
}
```

Create teacher request example:
```json
{
  "email": "a.testov@kbtu.kz",
  "password": "prof123",
  "fullName": "Askar Testov",
  "facultyId": 1,
  "department": "Information Systems",
  "positionTitle": "Senior Lecturer",
  "publicEmail": "askar.testov@kbtu.kz",
  "officeRoom": "417",
  "bio": "Teaches software engineering and distributed systems.",
  "officeHours": "Mon 10:00-12:00",
  "teacherRole": "TEACHER",
  "enabled": true
}
```

Update teacher request example:
```json
{
  "email": "a.testov@kbtu.kz",
  "password": "",
  "fullName": "Askar Testov",
  "facultyId": 1,
  "department": "Information Systems",
  "positionTitle": "Associate Professor",
  "publicEmail": "askar.testov@kbtu.kz",
  "officeRoom": "419",
  "bio": "Leads database systems and backend engineering courses.",
  "officeHours": "Wed 14:00-16:00",
  "teacherRole": "TEACHER",
  "enabled": true
}
```

Create student request example:
```json
{
  "email": "a_testov@kbtu.kz",
  "password": "student123",
  "fullName": "Aslan Testov",
  "facultyId": 1,
  "programId": 1,
  "currentSemesterId": 8,
  "course": 2,
  "groupName": "TBD",
  "status": "ACTIVE",
  "creditsEarned": 36,
  "passportNumber": "N1234567",
  "address": "Almaty",
  "phone": "+77010000000",
  "emergencyContact": "+77020000000",
  "enabled": true
}
```

Update student request example:
```json
{
  "email": "a_testov@kbtu.kz",
  "password": "",
  "fullName": "Aslan Testov",
  "facultyId": 1,
  "programId": 1,
  "currentSemesterId": 8,
  "course": 3,
  "groupName": "TBD",
  "status": "ACTIVE",
  "creditsEarned": 72,
  "passportNumber": "N1234567",
  "address": "Almaty",
  "phone": "+77010000000",
  "emergencyContact": "+77020000000",
  "enabled": true
}
```

### 7.3 Finance, Requests, Content, Mobility

| Method | Path | Permission | Description |
|---|---|---|---|
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
| GET | `/api/v1/admin/requests` | `SUPPORT` | List request tickets |
| POST | `/api/v1/admin/requests/{id}/assign` | `SUPPORT` | Assign request |
| POST | `/api/v1/admin/requests/{id}/status` | `SUPPORT` | Update request status |
| POST | `/api/v1/admin/news` | `CONTENT` | Create news |
| GET | `/api/v1/admin/checklist-templates` | `REGISTRAR` | List checklist templates |
| POST | `/api/v1/admin/checklist-templates` | `REGISTRAR` | Create checklist template |
| POST | `/api/v1/admin/checklist/generate` | `REGISTRAR` | Generate checklist items |
| GET | `/api/v1/admin/audit` | `SUPER` | Audit log |

### 7.4 Analytics and Admin Assistant

| Method | Path | Permission | Description |
|---|---|---|---|
| GET | `/api/v1/admin/analytics` | `SUPER` | Global analytics dashboard |
| GET | `/api/v1/admin/workflows` | `SUPER` | Workflow overview |
| GET | `/api/v1/admin/workflows/{type}/{id}/timeline` | `SUPER` | Workflow timeline |
| POST | `/api/v1/admin/assistant/chat` | `SUPER` | Admin AI assistant |

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

## 10. Real-Time Features

WebSocket endpoint:
- `/ws`

Used by frontend:
- notifications: `/user/queue/notifications`
- attendance personal events: `/user/queue/attendance`
- attendance section events: `/topic/attendance/section/{sectionId}`
- chat topics and app destinations for room messaging

Current behavior:
- unread notification counters update live
- notification pages refresh live
- attendance open/check-in/close events update teacher and student screens in real time
- chat uses WebSocket/STOMP for room messaging

## 11. AI Assistant Notes

The AI assistant does not query PostgreSQL directly.

Flow:
1. frontend sends a request to backend
2. backend authenticates the user
3. backend loads context from repositories and services
4. backend sends context plus the question to Gemini
5. Gemini returns text
6. backend sends the answer back to the client

So:
- DB access is backend-only
- assistants are read-only
- some student planner answers are deterministic and calculated in backend first

## 12. Demo Data Note

The repository does not contain your local PostgreSQL data.

So if another person clones the repo:
- they will not automatically get your current 100 students
- they will not get your local passwords file
- they will not get your exact local files or DB state

To generate demo data on a fresh database, they must explicitly enable the guarded seed:

```env
APP_SEED_ENABLED=true
APP_SEED_CONFIRM_TOKEN=DEMO_ONLY_RESET
```

If they need your exact current data, they need a PostgreSQL dump.

## 13. Quick cURL Examples

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

### Admin create student

```bash
curl -X POST http://localhost:8080/api/v1/admin/students \
  -H "Authorization: Bearer <ACCESS_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"email":"a_testov@kbtu.kz","password":"student123","fullName":"Aslan Testov","facultyId":1,"programId":1,"currentSemesterId":8,"course":2,"groupName":"TBD","status":"ACTIVE","creditsEarned":36,"enabled":true}'
```

### Admin create subject

```bash
curl -X POST http://localhost:8080/api/v1/admin/subjects \
  -H "Authorization: Bearer <ACCESS_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"code":"CSCI2104","name":"Databases","credits":4,"programId":1}'
```

### Admin create teacher

```bash
curl -X POST http://localhost:8080/api/v1/admin/teachers \
  -H "Authorization: Bearer <ACCESS_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"email":"a.testov@kbtu.kz","password":"prof123","fullName":"Askar Testov","facultyId":1,"department":"Information Systems","positionTitle":"Senior Lecturer","publicEmail":"askar.testov@kbtu.kz","officeRoom":"417","bio":"Teaches software engineering and distributed systems.","officeHours":"Mon 10:00-12:00","teacherRole":"TEACHER","enabled":true}'
```

### Teacher open attendance session

```bash
curl -X POST http://localhost:8080/api/v1/teacher/sections/8/attendance/open \
  -H "Authorization: Bearer <ACCESS_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"classDate":"2026-04-06","attendanceCloseAt":"2026-04-06T10:15:00","checkInMode":"ONE_CLICK","allowTeacherOverride":true}'
```
