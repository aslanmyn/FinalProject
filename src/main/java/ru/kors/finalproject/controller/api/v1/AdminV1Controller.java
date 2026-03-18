package ru.kors.finalproject.controller.api.v1;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    private final HoldService holdService;
    private final MobilityService mobilityService;
    private final ClearanceService clearanceService;
    private final SurveyService surveyService;
    private final ChecklistService checklistService;
    private final FinancialService financialService;
    private final RequestService requestService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final SubjectRepository subjectRepository;
    private final SubjectOfferingRepository subjectOfferingRepository;
    private final StudentRequestRepository studentRequestRepository;
    private final NewsRepository newsRepository;
    private final AuditLogRepository auditLogRepository;
    private final ApiPageableFactory apiPageableFactory;

    @GetMapping("/users")
    @PreAuthorize("hasAuthority('PERM_SUPER')")
    public ResponseEntity<?> users(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "desc") String direction) {
        var pageable = apiPageableFactory.create(
                page, size, sort, direction, "id",
                Set.of("id", "email", "fullName", "role", "enabled"));
        var data = userRepository.findAll(pageable).map(u -> new UserDto(
                u.getId(), u.getEmail(), u.getFullName(), u.getRole(), u.getAdminPermissions(), u.isEnabled()
        ));
        return ResponseEntity.ok(ApiPageResponse.from(data));
    }

    @PostMapping("/users/{id}/permissions")
    @PreAuthorize("hasAuthority('PERM_SUPER')")
    public ResponseEntity<?> setPermissions(
            @AuthenticationPrincipal User admin,
            @PathVariable Long id,
            @RequestBody PermissionBody body) {
        User target = userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (target.getRole() != User.UserRole.ADMIN) {
            throw new IllegalArgumentException("User is not admin");
        }
        if (body.permissions() == null || body.permissions().isEmpty()) {
            target.setAdminPermissions(EnumSet.noneOf(User.AdminPermission.class));
        } else {
            target.setAdminPermissions(EnumSet.copyOf(body.permissions()));
        }
        userRepository.save(target);
        return ResponseEntity.ok(new UserDto(
                target.getId(), target.getEmail(), target.getFullName(), target.getRole(), target.getAdminPermissions(), target.isEnabled()
        ));
    }

    @GetMapping("/holds")
    @PreAuthorize("hasAnyAuthority('PERM_SUPER', 'PERM_FINANCE')")
    public ResponseEntity<?> listHolds(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(holdService.listAllActiveHolds().stream().map(h -> new HoldDto(
                h.getId(), h.getStudent().getId(), h.getStudent().getName(),
                h.getType(), h.getReason(), h.getCreatedAt()
        )).toList());
    }

    @PostMapping("/holds")
    @PreAuthorize("hasAnyAuthority('PERM_SUPER', 'PERM_FINANCE')")
    public ResponseEntity<?> createHold(
            @AuthenticationPrincipal User admin,
            @RequestBody CreateHoldBody body) {
        Hold saved = holdService.createHold(body.studentId(), body.type(), body.reason(), admin);
        return ResponseEntity.ok(new HoldDto(saved.getId(), saved.getStudent().getId(),
                saved.getStudent().getName(), saved.getType(), saved.getReason(), saved.getCreatedAt()));
    }

    @PostMapping("/holds/{id}/remove")
    @PreAuthorize("hasAnyAuthority('PERM_SUPER', 'PERM_FINANCE')")
    public ResponseEntity<?> removeHold(
            @AuthenticationPrincipal User admin,
            @PathVariable Long id,
            @RequestBody RemoveHoldBody body) {
        holdService.removeHold(id, body.removalReason(), admin);
        return ResponseEntity.ok(Map.of("status", "removed"));
    }

    @GetMapping("/notifications")
    public ResponseEntity<?> notifications(@AuthenticationPrincipal User admin) {
        return ResponseEntity.ok(Map.of(
                "notifications", notificationService.listForEmail(admin.getEmail()).stream()
                        .map(this::toNotificationDto)
                        .toList(),
                "unreadCount", notificationService.unreadCount(admin.getEmail())
        ));
    }

    @PostMapping("/notifications/{id}/read")
    public ResponseEntity<?> markNotificationRead(
            @AuthenticationPrincipal User admin,
            @PathVariable Long id) {
        notificationService.markReadForEmail(id, admin.getEmail());
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @PostMapping("/notifications/read-all")
    public ResponseEntity<?> markAllNotificationsRead(@AuthenticationPrincipal User admin) {
        notificationService.markAllReadForEmail(admin.getEmail());
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @PostMapping("/finance/invoices")
    @PreAuthorize("hasAnyAuthority('PERM_SUPER', 'PERM_FINANCE')")
    public ResponseEntity<?> createInvoice(
            @AuthenticationPrincipal User admin,
            @RequestBody InvoiceBody body) {
        Student student = studentRepository.findById(body.studentId()).orElseThrow(() -> new IllegalArgumentException("Student not found"));
        return ResponseEntity.ok(toInvoiceDto(financialService.createInvoice(
                student, body.amount(), body.description(), LocalDate.parse(body.dueDate()))));
    }

    @PostMapping("/finance/payments")
    @PreAuthorize("hasAnyAuthority('PERM_SUPER', 'PERM_FINANCE')")
    public ResponseEntity<?> registerPayment(
            @AuthenticationPrincipal User admin,
            @RequestBody PaymentBody body) {
        Student student = studentRepository.findById(body.studentId()).orElseThrow(() -> new IllegalArgumentException("Student not found"));
        return ResponseEntity.ok(toPaymentDto(financialService.registerPayment(
                student, body.chargeId(), body.amount(),
                body.date() != null ? LocalDate.parse(body.date()) : LocalDate.now())));
    }

    @GetMapping("/mobility")
    @PreAuthorize("hasAnyAuthority('PERM_SUPER', 'PERM_MOBILITY')")
    public ResponseEntity<?> listMobility(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(mobilityService.listAll().stream().map(a -> new MobilityDto(
                a.getId(), a.getStudent().getId(), a.getStudent().getName(),
                a.getUniversityName(), a.getStatus(), a.getCreatedAt()
        )).toList());
    }

    @PostMapping("/mobility/{id}/status")
    @PreAuthorize("hasAnyAuthority('PERM_SUPER', 'PERM_MOBILITY')")
    public ResponseEntity<?> updateMobilityStatus(
            @AuthenticationPrincipal User admin,
            @PathVariable Long id,
            @RequestBody UpdateStatusBody body) {
        MobilityApplication updated = mobilityService.updateStatus(id, body.status(), admin);
        return ResponseEntity.ok(new MobilityDto(updated.getId(), updated.getStudent().getId(),
                updated.getStudent().getName(), updated.getUniversityName(), updated.getStatus(), updated.getCreatedAt()));
    }

    @GetMapping("/clearance")
    @PreAuthorize("hasAnyAuthority('PERM_SUPER', 'PERM_MOBILITY')")
    public ResponseEntity<?> listClearance(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(clearanceService.listAll().stream()
                .map(this::toClearanceDto)
                .toList());
    }

    @PostMapping("/clearance/checkpoints/{id}/review")
    @PreAuthorize("hasAnyAuthority('PERM_SUPER', 'PERM_MOBILITY')")
    public ResponseEntity<?> reviewCheckpoint(
            @AuthenticationPrincipal User admin,
            @PathVariable Long id,
            @RequestBody ReviewCheckpointBody body) {
        return ResponseEntity.ok(toClearanceCheckpointDto(
                clearanceService.reviewCheckpoint(id, body.approve(), body.comment(), admin)));
    }

    @GetMapping("/surveys")
    @PreAuthorize("hasAnyAuthority('PERM_SUPER', 'PERM_CONTENT')")
    public ResponseEntity<?> listSurveys(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(surveyService.listAll().stream()
                .map(this::toSurveyDto)
                .toList());
    }

    @PostMapping("/surveys")
    @PreAuthorize("hasAnyAuthority('PERM_SUPER', 'PERM_CONTENT')")
    public ResponseEntity<?> createSurvey(
            @AuthenticationPrincipal User admin,
            @RequestBody CreateSurveyBody body) {
        return ResponseEntity.ok(toSurveyDto(surveyService.create(
                body.title(), LocalDate.parse(body.startDate()), LocalDate.parse(body.endDate()),
                body.anonymous(), body.semesterId(), body.questions(), admin)));
    }

    @PostMapping("/surveys/{id}/close")
    @PreAuthorize("hasAnyAuthority('PERM_SUPER', 'PERM_CONTENT')")
    public ResponseEntity<?> closeSurvey(
            @AuthenticationPrincipal User admin,
            @PathVariable Long id) {
        return ResponseEntity.ok(toSurveyDto(surveyService.closeSurvey(id, admin)));
    }

    @GetMapping("/surveys/{id}/responses")
    @PreAuthorize("hasAnyAuthority('PERM_SUPER', 'PERM_CONTENT')")
    public ResponseEntity<?> exportSurveyResponses(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        return ResponseEntity.ok(Map.of(
                "surveyId", id,
                "count", surveyService.responseCount(id),
                "responses", surveyService.exportResponses(id).stream()
                        .map(this::toSurveyResponseDto)
                        .toList()
        ));
    }

    @GetMapping("/requests")
    @PreAuthorize("hasAnyAuthority('PERM_SUPER', 'PERM_SUPPORT')")
    public ResponseEntity<?> requests(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "desc") String direction) {
        var pageable = apiPageableFactory.create(
                page, size, sort, direction, "createdAt",
                Set.of("createdAt", "updatedAt", "category", "status"));
        var data = studentRequestRepository.findAll(pageable)
                .map(r -> new RequestDto(r.getId(), r.getCategory(), r.getStatus(), r.getCreatedAt(),
                        r.getUpdatedAt(), r.getAssignedTo() != null ? r.getAssignedTo().getId() : null));
        return ResponseEntity.ok(ApiPageResponse.from(data));
    }

    @PostMapping("/requests/{id}/assign")
    @PreAuthorize("hasAnyAuthority('PERM_SUPER', 'PERM_SUPPORT')")
    public ResponseEntity<?> assignRequest(
            @AuthenticationPrincipal User admin,
            @PathVariable Long id,
            @RequestBody AssignBody body) {
        StudentRequest request = requestService.assign(id, body.userId(), admin);
        return ResponseEntity.ok(new RequestDto(request.getId(), request.getCategory(), request.getStatus(),
                request.getCreatedAt(), request.getUpdatedAt(),
                request.getAssignedTo() != null ? request.getAssignedTo().getId() : null));
    }

    @PostMapping("/requests/{id}/status")
    @PreAuthorize("hasAnyAuthority('PERM_SUPER', 'PERM_SUPPORT')")
    public ResponseEntity<?> updateRequestStatus(
            @AuthenticationPrincipal User admin,
            @PathVariable Long id,
            @RequestBody RequestStatusBody body) {
        StudentRequest request = requestService.updateStatus(id, body.status(), admin);
        return ResponseEntity.ok(new RequestDto(request.getId(), request.getCategory(), request.getStatus(),
                request.getCreatedAt(), request.getUpdatedAt(),
                request.getAssignedTo() != null ? request.getAssignedTo().getId() : null));
    }

    @PostMapping("/news")
    @PreAuthorize("hasAnyAuthority('PERM_SUPER', 'PERM_CONTENT')")
    public ResponseEntity<?> createNews(
            @AuthenticationPrincipal User admin,
            @RequestBody NewsBody body) {
        News news = News.builder()
                .title(body.title())
                .content(body.content())
                .category(body.category())
                .createdAt(Instant.now())
                .build();
        News saved = newsRepository.save(news);
        return ResponseEntity.ok(new NewsDto(saved.getId(), saved.getTitle(), saved.getContent(),
                saved.getCategory(), saved.getCreatedAt()));
    }

    @GetMapping("/checklist-templates")
    @PreAuthorize("hasAnyAuthority('PERM_SUPER', 'PERM_REGISTRAR')")
    public ResponseEntity<?> checklistTemplates(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(checklistService.listTemplates());
    }

    @PostMapping("/checklist-templates")
    @PreAuthorize("hasAnyAuthority('PERM_SUPER', 'PERM_REGISTRAR')")
    public ResponseEntity<?> createChecklistTemplate(
            @AuthenticationPrincipal User admin,
            @RequestBody CreateChecklistTemplateBody body) {
        return ResponseEntity.ok(checklistService.createTemplate(
                body.title(), body.linkToSection(), body.triggerEvent(), body.offsetDays(), admin));
    }

    @PostMapping("/checklist/generate")
    @PreAuthorize("hasAnyAuthority('PERM_SUPER', 'PERM_REGISTRAR')")
    public ResponseEntity<?> generateChecklists(
            @AuthenticationPrincipal User admin,
            @RequestBody GenerateChecklistBody body) {
        if (body.studentId() != null) {
            checklistService.generateForStudent(body.studentId(), body.trigger(), LocalDate.parse(body.baseDate()));
        } else {
            checklistService.generateForAllStudents(body.trigger(), LocalDate.parse(body.baseDate()));
        }
        return ResponseEntity.ok(Map.of("status", "generated"));
    }

    @GetMapping("/audit")
    @PreAuthorize("hasAuthority('PERM_SUPER')")
    public ResponseEntity<?> auditLogs(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "desc") String direction) {
        var pageable = apiPageableFactory.create(
                page, Math.min(size, 200), sort, direction, "createdAt",
                Set.of("createdAt", "action", "actorEmail", "entityType"));
        return ResponseEntity.ok(ApiPageResponse.from(auditLogRepository.findAll(pageable)));
    }

    @GetMapping("/stats")
    public ResponseEntity<?> stats(@AuthenticationPrincipal User admin) {
        return ResponseEntity.ok(Map.of(
                "adminId", admin.getId(),
                "students", studentRepository.count(),
                "teachers", teacherRepository.count(),
                "sections", subjectOfferingRepository.count(),
                "requests", studentRequestRepository.count(),
                "activeHolds", holdService.listAllActiveHolds().size()
        ));
    }

    @GetMapping("/subjects")
    public ResponseEntity<?> subjects(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(subjectRepository.findAll().stream().map(s -> Map.of(
                "id", (Object) s.getId(),
                "code", s.getCode() != null ? s.getCode() : "",
                "name", s.getName() != null ? s.getName() : "",
                "credits", s.getCredits()
        )).toList());
    }

    @GetMapping("/teachers")
    public ResponseEntity<?> teachers(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(teacherRepository.findAllByOrderByNameAsc().stream().map(t -> Map.of(
                "id", (Object) t.getId(),
                "name", t.getName() != null ? t.getName() : "",
                "email", t.getEmail() != null ? t.getEmail() : ""
        )).toList());
    }

    @GetMapping("/students")
    public ResponseEntity<?> students(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(studentRepository.findAllWithDetails().stream().map(s -> Map.of(
                "id", (Object) s.getId(),
                "name", s.getName() != null ? s.getName() : "",
                "email", s.getEmail() != null ? s.getEmail() : "",
                "status", s.getStatus() != null ? s.getStatus().name() : ""
        )).toList());
    }

    private InvoiceDto toInvoiceDto(Charge charge) {
        return new InvoiceDto(
                charge.getId(),
                charge.getStudent() != null ? charge.getStudent().getId() : null,
                charge.getAmount(),
                charge.getDescription(),
                charge.getDueDate(),
                charge.getStatus()
        );
    }

    private PaymentDto toPaymentDto(Payment payment) {
        return new PaymentDto(
                payment.getId(),
                payment.getStudent() != null ? payment.getStudent().getId() : null,
                payment.getCharge() != null ? payment.getCharge().getId() : null,
                payment.getAmount(),
                payment.getDate()
        );
    }

    private ClearanceDto toClearanceDto(ClearanceSheet sheet) {
        return new ClearanceDto(
                sheet.getId(),
                sheet.getStudent() != null ? sheet.getStudent().getId() : null,
                sheet.getStudent() != null ? sheet.getStudent().getName() : null,
                sheet.getStatus(),
                sheet.getCheckpoints().stream().map(this::toClearanceCheckpointDto).toList()
        );
    }

    private ClearanceCheckpointDto toClearanceCheckpointDto(ClearanceCheckpoint checkpoint) {
        return new ClearanceCheckpointDto(
                checkpoint.getId(),
                checkpoint.getClearanceSheet() != null ? checkpoint.getClearanceSheet().getId() : null,
                checkpoint.getDepartment(),
                checkpoint.getStatus(),
                checkpoint.getComment()
        );
    }

    private SurveyDto toSurveyDto(Survey survey) {
        return new SurveyDto(
                survey.getId(),
                survey.getTitle(),
                survey.getStartDate(),
                survey.getEndDate(),
                survey.isAnonymous(),
                survey.getSemester() != null ? survey.getSemester().getId() : null,
                survey.getSemester() != null ? survey.getSemester().getName() : null
        );
    }

    private SurveyResponseDto toSurveyResponseDto(SurveyResponse response) {
        return new SurveyResponseDto(
                response.getId(),
                response.getSurvey() != null ? response.getSurvey().getId() : null,
                response.getStudent() != null ? response.getStudent().getId() : null,
                response.getAnswersJson(),
                response.getSubmittedAt()
        );
    }

    private NotificationDto toNotificationDto(Notification notification) {
        return new NotificationDto(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getLink(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }

    public record UserDto(Long id, String email, String fullName, User.UserRole role,
                           java.util.Set<User.AdminPermission> permissions, boolean enabled) {}
    public record PermissionBody(java.util.Set<User.AdminPermission> permissions) {}
    public record HoldDto(Long id, Long studentId, String studentName, Hold.HoldType type,
                           String reason, Instant createdAt) {}
    public record CreateHoldBody(Long studentId, Hold.HoldType type, String reason) {}
    public record RemoveHoldBody(String removalReason) {}
    public record InvoiceDto(Long id, Long studentId, BigDecimal amount, String description,
                             LocalDate dueDate, Charge.ChargeStatus status) {}
    public record InvoiceBody(Long studentId, BigDecimal amount, String description, String dueDate) {}
    public record PaymentDto(Long id, Long studentId, Long chargeId, BigDecimal amount, LocalDate date) {}
    public record PaymentBody(Long studentId, Long chargeId, BigDecimal amount, String date) {}
    public record MobilityDto(Long id, Long studentId, String studentName, String university,
                               MobilityApplication.MobilityStatus status, Instant createdAt) {}
    public record UpdateStatusBody(MobilityApplication.MobilityStatus status) {}
    public record ClearanceDto(Long id, Long studentId, String studentName,
                               ClearanceSheet.ClearanceStatus status, List<ClearanceCheckpointDto> checkpoints) {}
    public record ClearanceCheckpointDto(Long id, Long clearanceSheetId, String department,
                                         ClearanceCheckpoint.CheckpointStatus status, String comment) {}
    public record ReviewCheckpointBody(boolean approve, String comment) {}
    public record SurveyDto(Long id, String title, LocalDate startDate, LocalDate endDate,
                            boolean anonymous, Long semesterId, String semesterName) {}
    public record CreateSurveyBody(String title, String startDate, String endDate, boolean anonymous,
                                    Long semesterId, List<SurveyService.QuestionInput> questions) {}
    public record SurveyResponseDto(Long id, Long surveyId, Long studentId, String answersJson, Instant submittedAt) {}
    public record RequestDto(Long id, String category, StudentRequest.RequestStatus status,
                              Instant createdAt, Instant updatedAt, Long assignedToUserId) {}
    public record AssignBody(Long userId) {}
    public record RequestStatusBody(StudentRequest.RequestStatus status) {}
    public record NewsDto(Long id, String title, String content, String category, Instant createdAt) {}
    public record NewsBody(String title, String content, String category) {}
    public record NotificationDto(Long id, Notification.NotificationType type, String title,
                                  String message, String link, boolean read, Instant createdAt) {}
    public record CreateChecklistTemplateBody(String title, String linkToSection,
                                               ChecklistTemplate.TriggerEvent triggerEvent, int offsetDays) {}
    public record GenerateChecklistBody(Long studentId, ChecklistTemplate.TriggerEvent trigger, String baseDate) {}
}
