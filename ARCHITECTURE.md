# Monolithic Student Information System — Architecture

## Overview

The system is implemented as a **modular monolithic application**: a single Spring Boot app with a single database, logically divided into independent domain modules.

## Domain Modules

### 1. User & Student Management (`entity.user`)
- **User** — Authentication, links to StudentProfile for students
- **Role** — RBAC (ROLE_STUDENT, ROLE_TEACHER, ROLE_COORDINATOR, ROLE_ADMIN)
- **StudentProfile** — Student master data (program, faculty, term, credits, status)
- **StudentHold** — Blocks actions when active (FINANCIAL, ACADEMIC, DOCUMENT)

**Rules:** StudentHolds must be checked before registration, FX, and exams.

### 2. Academic Structure (`entity.academic`)
- **Faculty**, **Program**
- **AcademicTerm** — Semesters
- **Course**, **CoursePrerequisite**
- **CourseSection** — Course offering in a term (references Course, AcademicTerm, User instructor)
- **MeetingTime** — Schedule slots (day, time, room, type: LECTURE/PRACTICE/LAB)
- **Enrollment** — Unique per student, section, term
- **RegistrationWindow** — Add/drop period per term

**Rules:** A student cannot register outside an active RegistrationWindow. Holds block registration.

### 3. Schedule Management
Uses: **CourseSection**, **MeetingTime**, **Enrollment**
- Schedule from Enrollment → CourseSection → MeetingTime
- Conflict detection based on MeetingTime overlap

### 4. Assessments (`entity.assessment`)
- **AssessmentComponent** — Grading structure per course section
- **StudentGrade** — Points per component
- **FinalGrade** — Calculated from StudentGrade
- **AttendanceSession**, **AttendanceRecord**
- **ExamSession**
- **FXApplication** — May require payment before approval

**Rules:** FinalGrade calculated from StudentGrade; attendance impacts exam eligibility.

### 5. Surveys (`entity.survey`)
- **Survey**, **SurveyQuestion**, **SurveyOption**
- **SurveyResponse**, **SurveyAnswer**

**Rules:** One submission per student; supports anonymous surveys.

### 6. Student Requests (`entity.request`)
- **RequestTicket** — Category, description, status
- **RequestMessage** — Thread of messages

### 7. News (`entity.news`)
- **NewsPost** — Title, content, category, publishedAt

### 8. Shared (`entity.shared`)
- **FileAsset** — Shared storage (entityType, entityId for polymorphic association)

### 9. Academic Mobility (`entity.mobility`)
- **MobilityApplication** — Host university, status
- **MobilityCourseMapping** — External course → internal Course mapping

### 10. Finance (`entity.finance`)
- **Invoice** — Charges
- **Payment** — Linked to Invoice
- **ScholarshipOrDiscount**

**Rules:** Unpaid invoices block academic actions.

### 11. Clearance (`entity.clearance`)
- **ClearanceProcess**, **ClearanceItem**
- **ChecklistItemTemplate** — Template for mandatory tasks
- **StudentChecklistItem** — Per-student checklist

## Key System Rules

1. **Student schedule** = Enrollment → CourseSection → MeetingTime
2. **Transcript** = FinalGrade across all academic terms
3. **StudentHolds** block registration, FX, and exams
4. **Financial debt** blocks registration and clearance
5. **FileAsset** is shared across modules

## Implementation Status

- [x] Phase 1 — User, Role, StudentProfile, StudentHold entities
- [x] Phase 2 — Academic structure entities
- [x] Phase 3 — Assessment entities
- [x] Phase 4 — Surveys, Requests, News, FileAsset, Mobility
- [x] Phase 5 — Finance, Clearance, Checklist entities

**Next:** Repositories, services, DataInitializer migration, controller updates to use new entities.
