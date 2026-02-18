package ru.kors.finalproject.controller.api.v1;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.kors.finalproject.entity.*;
import ru.kors.finalproject.repository.*;
import ru.kors.finalproject.service.*;
import ru.kors.finalproject.web.api.v1.ApiPageResponse;
import ru.kors.finalproject.web.api.v1.ApiPageableFactory;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminV1Controller {
    private final MobileApiAuthService mobileApiAuthService;
    private final AdminAcademicService adminAcademicService;
    private final HoldService holdService;
    private final ExamScheduleService examScheduleService;
    private final MobilityService mobilityService;
    private final ClearanceService clearanceService;
    private final SurveyService surveyService;
    private final ChecklistService checklistService;
    private final FinancialService financialService;
    private final GradeChangeService gradeChangeService;
    private final RequestService requestService;
    private final AddDropService addDropService;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final SemesterRepository semesterRepository;
    private final SubjectOfferingRepository subjectOfferingRepository;
    private final StudentRequestRepository studentRequestRepository;
    private final NewsRepository newsRepository;
    private final GradeChangeRequestRepository gradeChangeRequestRepository;
    private final AuditLogRepository auditLogRepository;
    private final ApiPageableFactory apiPageableFactory;

    @GetMapping("/users")
    public ResponseEntity<?> users(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "desc") String direction) {
        mobileApiAuthService.requireAdminPermission(authHeader, User.AdminPermission.SUPER);
        var pageable = apiPageableFactory.create(
                page, size, sort, direction, "id",
                Set.of("id", "email", "fullName", "role", "enabled"));
        var data = userRepository.findAll(pageable).map(u -> new UserDto(
                u.getId(), u.getEmail(), u.getFullName(), u.getRole(), u.getAdminPermissions(), u.isEnabled()
        ));
        return ResponseEntity.ok(ApiPageResponse.from(data));
    }

    @PostMapping("/users/{id}/permissions")
    public ResponseEntity<?> setPermissions(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id,
            @RequestBody PermissionBody body) {
        mobileApiAuthService.requireAdminPermission(authHeader, User.AdminPermission.SUPER);
        User user = userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (user.getRole() != User.UserRole.ADMIN) {
            throw new IllegalArgumentException("User is not admin");
        }
        if (body.permissions() == null || body.permissions().isEmpty()) {
            user.setAdminPermissions(EnumSet.noneOf(User.AdminPermission.class));
        } else {
            user.setAdminPermissions(EnumSet.copyOf(body.permissions()));
        }
        userRepository.save(user);
        return ResponseEntity.ok(new UserDto(
                user.getId(), user.getEmail(), user.getFullName(), user.getRole(), user.getAdminPermissions(), user.isEnabled()
        ));
    }

    @PostMapping("/terms")
    public ResponseEntity<?> createTerm(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody CreateTermBody body) {
        User admin = mobileApiAuthService.requireAdminPermission(authHeader, User.AdminPermission.REGISTRAR);
        Semester saved = adminAcademicService.createTerm(
                body.name(), LocalDate.parse(body.startDate()), LocalDate.parse(body.endDate()), body.current(), admin);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/terms")
    public ResponseEntity<?> listTerms(@RequestHeader("Authorization") String authHeader) {
        mobileApiAuthService.requireAdminPermission(authHeader, User.AdminPermission.REGISTRAR);
        return ResponseEntity.ok(adminAcademicService.listTerms());
    }

    @PostMapping("/sections")
    public ResponseEntity<?> createSection(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody CreateSectionBody body) {
        User admin = mobileApiAuthService.requireAdminPermission(authHeader, User.AdminPermission.REGISTRAR);
        SubjectOffering saved = adminAcademicService.createSection(
                body.subjectId(), body.semesterId(), body.teacherId(), body.capacity(), body.lessonType(), admin);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/sections")
    public ResponseEntity<?> listSections(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(required = false) Long semesterId) {
        mobileApiAuthService.requireAdminPermission(authHeader, User.AdminPermission.REGISTRAR);
        return ResponseEntity.ok(adminAcademicService.listSections(semesterId));
    }

    @PostMapping("/sections/{id}/assign-professor")
    public ResponseEntity<?> assignProfessor(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id,
            @RequestBody AssignProfessorBody body) {
        User admin = mobileApiAuthService.requireAdminPermission(authHeader, User.AdminPermission.REGISTRAR);
        return ResponseEntity.ok(adminAcademicService.assignProfessor(id, body.teacherId(), admin));
    }

    @PostMapping("/sections/{id}/meeting-times")
    public ResponseEntity<?> addMeetingTime(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id,
            @RequestBody MeetingTimeBody body) {
        User admin = mobileApiAuthService.requireAdminPermission(authHeader, User.AdminPermission.REGISTRAR);
        return ResponseEntity.ok(adminAcademicService.addMeetingTime(
                id, body.dayOfWeek(), LocalTime.parse(body.startTime()), LocalTime.parse(body.endTime()),
                body.room(), body.lessonType(), admin));
    }

    @PostMapping("/windows")
    public ResponseEntity<?> upsertWindow(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody WindowBody body) {
        User admin = mobileApiAuthService.requireAdminPermission(authHeader, User.AdminPermission.REGISTRAR);
        return ResponseEntity.ok(adminAcademicService.upsertWindow(
                body.semesterId(), body.type(), LocalDate.parse(body.startDate()),
                LocalDate.parse(body.endDate()), body.active(), admin));
    }

    @PostMapping("/enrollments/override")
    public ResponseEntity<?> overrideEnrollment(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody EnrollmentOverrideBody body) {
        User admin = mobileApiAuthService.requireAdminPermission(authHeader, User.AdminPermission.REGISTRAR);
        return ResponseEntity.ok(adminAcademicService.adminOverrideEnroll(
                body.studentId(), body.subjectOfferingId(), body.reason(), admin));
    }

    @GetMapping("/exams")
    public ResponseEntity<?> listExams(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(required = false) Long semesterId) {
        mobileApiAuthService.requireAdminPermission(authHeader, User.AdminPermission.REGISTRAR);
        Long semId = semesterId != null ? semesterId
                : semesterRepository.findByCurrentTrue().map(Semester::getId).orElse(null);
        if (semId == null) return ResponseEntity.ok(List.of());
        return ResponseEntity.ok(examScheduleService.listBySemester(semId));
    }

    @PostMapping("/exams")
    public ResponseEntity<?> createExam(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody CreateExamBody body) {
        User admin = mobileApiAuthService.requireAdminPermission(authHeader, User.AdminPermission.REGISTRAR);
        return ResponseEntity.ok(examScheduleService.createExamSession(
                body.sectionId(), LocalDate.parse(body.examDate()), LocalTime.parse(body.examTime()),
                body.room(), body.format(), admin));
    }

    @PutMapping("/exams/{id}")
    public ResponseEntity<?> updateExam(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id,
            @RequestBody CreateExamBody body) {
        User admin = mobileApiAuthService.requireAdminPermission(authHeader, User.AdminPermission.REGISTRAR);
        return ResponseEntity.ok(examScheduleService.updateExamSession(
                id, LocalDate.parse(body.examDate()), LocalTime.parse(body.examTime()),
                body.room(), body.format(), admin));
    }

    @DeleteMapping("/exams/{id}")
    public ResponseEntity<?> deleteExam(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {
        User admin = mobileApiAuthService.requireAdminPermission(authHeader, User.AdminPermission.REGISTRAR);
        examScheduleService.deleteExamSession(id, admin);
        return ResponseEntity.ok(Map.of("status", "deleted"));
    }

    @GetMapping("/holds")
    public ResponseEntity<?> listHolds(@RequestHeader("Authorization") String authHeader) {
        mobileApiAuthService.requireAdminPermission(authHeader, User.AdminPermission.FINANCE);
        return ResponseEntity.ok(holdService.listAllActiveHolds().stream().map(h -> new HoldDto(
                h.getId(), h.getStudent().getId(), h.getStudent().getName(),
                h.getType(), h.getReason(), h.getCreatedAt()
        )).toList());
    }

    @PostMapping("/holds")
    public ResponseEntity<?> createHold(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody CreateHoldBody body) {
        User admin = mobileApiAuthService.requireAdminPermission(authHeader, User.AdminPermission.FINANCE);
        Hold saved = holdService.createHold(body.studentId(), body.type(), body.reason(), admin);
        return ResponseEntity.ok(new HoldDto(saved.getId(), saved.getStudent().getId(),
                saved.getStudent().getName(), saved.getType(), saved.getReason(), saved.getCreatedAt()));
    }

    @PostMapping("/holds/{id}/remove")
    public ResponseEntity<?> removeHold(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id,
            @RequestBody RemoveHoldBody body) {
        User admin = mobileApiAuthService.requireAdminPermission(authHeader, User.AdminPermission.FINANCE);
        holdService.removeHold(id, body.removalReason(), admin);
        return ResponseEntity.ok(Map.of("status", "removed"));
    }

    @PostMapping("/finance/invoices")
    public ResponseEntity<?> createInvoice(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody InvoiceBody body) {
        mobileApiAuthService.requireAdminPermission(authHeader, User.AdminPermission.FINANCE);
        Student student = studentRepository.findById(body.studentId()).orElseThrow(() -> new IllegalArgumentException("Student not found"));
        return ResponseEntity.ok(financialService.createInvoice(
                student, body.amount(), body.description(), LocalDate.parse(body.dueDate())));
    }

    @PostMapping("/finance/payments")
    public ResponseEntity<?> registerPayment(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody PaymentBody body) {
        mobileApiAuthService.requireAdminPermission(authHeader, User.AdminPermission.FINANCE);
        Student student = studentRepository.findById(body.studentId()).orElseThrow(() -> new IllegalArgumentException("Student not found"));
        return ResponseEntity.ok(financialService.registerPayment(
                student, body.chargeId(), body.amount(),
                body.date() != null ? LocalDate.parse(body.date()) : LocalDate.now()));
    }

    @GetMapping("/mobility")
    public ResponseEntity<?> listMobility(@RequestHeader("Authorization") String authHeader) {
        mobileApiAuthService.requireAdminPermission(authHeader, User.AdminPermission.MOBILITY);
        return ResponseEntity.ok(mobilityService.listAll().stream().map(a -> new MobilityDto(
                a.getId(), a.getStudent().getId(), a.getStudent().getName(),
                a.getUniversityName(), a.getStatus(), a.getCreatedAt()
        )).toList());
    }

    @PostMapping("/mobility/{id}/status")
    public ResponseEntity<?> updateMobilityStatus(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id,
            @RequestBody UpdateStatusBody body) {
        User admin = mobileApiAuthService.requireAdminPermission(authHeader, User.AdminPermission.MOBILITY);
        MobilityApplication updated = mobilityService.updateStatus(id, body.status(), admin);
        return ResponseEntity.ok(new MobilityDto(updated.getId(), updated.getStudent().getId(),
                updated.getStudent().getName(), updated.getUniversityName(), updated.getStatus(), updated.getCreatedAt()));
    }

    @GetMapping("/clearance")
    public ResponseEntity<?> listClearance(@RequestHeader("Authorization") String authHeader) {
        mobileApiAuthService.requireAdminPermission(authHeader, User.AdminPermission.MOBILITY);
        return ResponseEntity.ok(clearanceService.listAll());
    }

    @PostMapping("/clearance/checkpoints/{id}/review")
    public ResponseEntity<?> reviewCheckpoint(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id,
            @RequestBody ReviewCheckpointBody body) {
        User admin = mobileApiAuthService.requireAdminPermission(authHeader, User.AdminPermission.MOBILITY);
        return ResponseEntity.ok(clearanceService.reviewCheckpoint(id, body.approve(), body.comment(), admin));
    }

    @GetMapping("/surveys")
    public ResponseEntity<?> listSurveys(@RequestHeader("Authorization") String authHeader) {
        mobileApiAuthService.requireAdminPermission(authHeader, User.AdminPermission.CONTENT);
        return ResponseEntity.ok(surveyService.listAll());
    }

    @PostMapping("/surveys")
    public ResponseEntity<?> createSurvey(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody CreateSurveyBody body) {
        User admin = mobileApiAuthService.requireAdminPermission(authHeader, User.AdminPermission.CONTENT);
        return ResponseEntity.ok(surveyService.create(
                body.title(), LocalDate.parse(body.startDate()), LocalDate.parse(body.endDate()),
                body.anonymous(), body.semesterId(), body.questions(), admin));
    }

    @PostMapping("/surveys/{id}/close")
    public ResponseEntity<?> closeSurvey(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {
        User admin = mobileApiAuthService.requireAdminPermission(authHeader, User.AdminPermission.CONTENT);
        return ResponseEntity.ok(surveyService.closeSurvey(id, admin));
    }

    @GetMapping("/surveys/{id}/responses")
    public ResponseEntity<?> exportSurveyResponses(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {
        mobileApiAuthService.requireAdminPermission(authHeader, User.AdminPermission.CONTENT);
        return ResponseEntity.ok(Map.of(
                "surveyId", id,
                "count", surveyService.responseCount(id),
                "responses", surveyService.exportResponses(id)
        ));
    }

    @GetMapping("/requests")
    public ResponseEntity<?> requests(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "desc") String direction) {
        mobileApiAuthService.requireAdminPermission(authHeader, User.AdminPermission.SUPPORT);
        var pageable = apiPageableFactory.create(
                page, size, sort, direction, "createdAt",
                Set.of("createdAt", "updatedAt", "category", "status"));
        var data = studentRequestRepository.findAll(pageable)
                .map(r -> new RequestDto(r.getId(), r.getCategory(), r.getStatus(), r.getCreatedAt(),
                        r.getUpdatedAt(), r.getAssignedTo() != null ? r.getAssignedTo().getId() : null));
        return ResponseEntity.ok(ApiPageResponse.from(data));
    }

    @PostMapping("/requests/{id}/assign")
    public ResponseEntity<?> assignRequest(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id,
            @RequestBody AssignBody body) {
        User actor = mobileApiAuthService.requireAdminPermission(authHeader, User.AdminPermission.SUPPORT);
        return ResponseEntity.ok(requestService.assign(id, body.userId(), actor));
    }

    @PostMapping("/requests/{id}/status")
    public ResponseEntity<?> updateRequestStatus(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id,
            @RequestBody RequestStatusBody body) {
        User actor = mobileApiAuthService.requireAdminPermission(authHeader, User.AdminPermission.SUPPORT);
        return ResponseEntity.ok(requestService.updateStatus(id, body.status(), actor));
    }

    @GetMapping("/grade-change-requests")
    public ResponseEntity<?> gradeChangeRequests(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "desc") String direction) {
        mobileApiAuthService.requireAdminPermission(authHeader, User.AdminPermission.REGISTRAR);
        var pageable = apiPageableFactory.create(
                page, size, sort, direction, "createdAt",
                Set.of("createdAt", "status", "newValue", "oldValue"));
        var data = gradeChangeRequestRepository.findByStatus(
                        GradeChangeRequest.RequestStatus.SUBMITTED, pageable)
                .map(r -> new GradeChangeDto(r.getId(), r.getTeacher().getId(), r.getStudent().getId(),
                        r.getSubjectOffering().getId(), r.getOldValue(), r.getNewValue(),
                        r.getReason(), r.getStatus(), r.getCreatedAt()));
        return ResponseEntity.ok(ApiPageResponse.from(data));
    }

    @PostMapping("/grade-change-requests/{id}/review")
    public ResponseEntity<?> reviewGradeChangeRequest(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id,
            @RequestBody ReviewGradeChangeBody body) {
        User admin = mobileApiAuthService.requireAdminPermission(authHeader, User.AdminPermission.REGISTRAR);
        return ResponseEntity.ok(gradeChangeService.review(id, body.approve(), body.comment(), admin));
    }

    @PostMapping("/news")
    public ResponseEntity<?> createNews(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody NewsBody body) {
        mobileApiAuthService.requireAdminPermission(authHeader, User.AdminPermission.CONTENT);
        News news = News.builder()
                .title(body.title())
                .content(body.content())
                .category(body.category())
                .createdAt(Instant.now())
                .build();
        return ResponseEntity.ok(newsRepository.save(news));
    }

    @GetMapping("/checklist-templates")
    public ResponseEntity<?> checklistTemplates(@RequestHeader("Authorization") String authHeader) {
        mobileApiAuthService.requireAdminPermission(authHeader, User.AdminPermission.REGISTRAR);
        return ResponseEntity.ok(checklistService.listTemplates());
    }

    @PostMapping("/checklist-templates")
    public ResponseEntity<?> createChecklistTemplate(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody CreateChecklistTemplateBody body) {
        User admin = mobileApiAuthService.requireAdminPermission(authHeader, User.AdminPermission.REGISTRAR);
        return ResponseEntity.ok(checklistService.createTemplate(
                body.title(), body.linkToSection(), body.triggerEvent(), body.offsetDays(), admin));
    }

    @PostMapping("/checklist/generate")
    public ResponseEntity<?> generateChecklists(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody GenerateChecklistBody body) {
        mobileApiAuthService.requireAdminPermission(authHeader, User.AdminPermission.REGISTRAR);
        if (body.studentId() != null) {
            checklistService.generateForStudent(body.studentId(), body.trigger(), LocalDate.parse(body.baseDate()));
        } else {
            checklistService.generateForAllStudents(body.trigger(), LocalDate.parse(body.baseDate()));
        }
        return ResponseEntity.ok(Map.of("status", "generated"));
    }

    @GetMapping("/audit")
    public ResponseEntity<?> auditLogs(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "desc") String direction) {
        mobileApiAuthService.requireAdminPermission(authHeader, User.AdminPermission.SUPER);
        var pageable = apiPageableFactory.create(
                page, Math.min(size, 200), sort, direction, "createdAt",
                Set.of("createdAt", "action", "actorEmail", "entityType"));
        return ResponseEntity.ok(ApiPageResponse.from(auditLogRepository.findAll(pageable)));
    }

    @GetMapping("/stats")
    public ResponseEntity<?> stats(@RequestHeader("Authorization") String authHeader) {
        User admin = mobileApiAuthService.requireRole(authHeader, User.UserRole.ADMIN);
        return ResponseEntity.ok(Map.of(
                "adminId", admin.getId(),
                "students", studentRepository.count(),
                "teachers", teacherRepository.count(),
                "sections", subjectOfferingRepository.count(),
                "requests", studentRequestRepository.count(),
                "activeHolds", holdService.listAllActiveHolds().size()
        ));
    }

    public record UserDto(Long id, String email, String fullName, User.UserRole role,
                           java.util.Set<User.AdminPermission> permissions, boolean enabled) {}
    public record PermissionBody(java.util.Set<User.AdminPermission> permissions) {}
    public record CreateTermBody(String name, String startDate, String endDate, boolean current) {}
    public record CreateSectionBody(Long subjectId, Long semesterId, Long teacherId, int capacity,
                                     SubjectOffering.LessonType lessonType) {}
    public record AssignProfessorBody(Long teacherId) {}
    public record MeetingTimeBody(java.time.DayOfWeek dayOfWeek, String startTime, String endTime,
                                   String room, SubjectOffering.LessonType lessonType) {}
    public record WindowBody(Long semesterId, RegistrationWindow.WindowType type, String startDate,
                              String endDate, boolean active) {}
    public record EnrollmentOverrideBody(Long studentId, Long subjectOfferingId, String reason) {}
    public record CreateExamBody(Long sectionId, String examDate, String examTime, String room, String format) {}
    public record HoldDto(Long id, Long studentId, String studentName, Hold.HoldType type,
                           String reason, Instant createdAt) {}
    public record CreateHoldBody(Long studentId, Hold.HoldType type, String reason) {}
    public record RemoveHoldBody(String removalReason) {}
    public record InvoiceBody(Long studentId, BigDecimal amount, String description, String dueDate) {}
    public record PaymentBody(Long studentId, Long chargeId, BigDecimal amount, String date) {}
    public record MobilityDto(Long id, Long studentId, String studentName, String university,
                               MobilityApplication.MobilityStatus status, Instant createdAt) {}
    public record UpdateStatusBody(MobilityApplication.MobilityStatus status) {}
    public record ReviewCheckpointBody(boolean approve, String comment) {}
    public record CreateSurveyBody(String title, String startDate, String endDate, boolean anonymous,
                                    Long semesterId, List<SurveyService.QuestionInput> questions) {}
    public record RequestDto(Long id, String category, StudentRequest.RequestStatus status,
                              Instant createdAt, Instant updatedAt, Long assignedToUserId) {}
    public record AssignBody(Long userId) {}
    public record RequestStatusBody(StudentRequest.RequestStatus status) {}
    public record GradeChangeDto(Long id, Long teacherId, Long studentId, Long sectionId,
                                  Double oldValue, Double newValue, String reason,
                                  GradeChangeRequest.RequestStatus status, Instant createdAt) {}
    public record ReviewGradeChangeBody(boolean approve, String comment) {}
    public record NewsBody(String title, String content, String category) {}
    public record CreateChecklistTemplateBody(String title, String linkToSection,
                                               ChecklistTemplate.TriggerEvent triggerEvent, int offsetDays) {}
    public record GenerateChecklistBody(Long studentId, ChecklistTemplate.TriggerEvent trigger, String baseDate) {}
}
