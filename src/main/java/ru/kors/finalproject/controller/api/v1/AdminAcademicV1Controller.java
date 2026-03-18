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

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminAcademicV1Controller {

    private final AdminAcademicService adminAcademicService;
    private final ExamScheduleService examScheduleService;
    private final GradeChangeService gradeChangeService;
    private final FxRegistrationService fxRegistrationService;
    private final SemesterRepository semesterRepository;
    private final RegistrationWindowRepository registrationWindowRepository;
    private final SubjectOfferingRepository subjectOfferingRepository;
    private final ExamScheduleRepository examScheduleRepository;
    private final GradeChangeRequestRepository gradeChangeRequestRepository;
    private final StudentRepository studentRepository;
    private final ApiPageableFactory apiPageableFactory;

    @PostMapping("/terms")
    @PreAuthorize("hasAnyAuthority('PERM_SUPER', 'PERM_REGISTRAR')")
    public ResponseEntity<?> createTerm(
            @AuthenticationPrincipal User admin,
            @RequestBody CreateTermBody body) {
        Semester saved = adminAcademicService.createTerm(
                body.name(), LocalDate.parse(body.startDate()), LocalDate.parse(body.endDate()), body.current(), admin);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/terms")
    @PreAuthorize("hasAnyAuthority('PERM_SUPER', 'PERM_REGISTRAR')")
    public ResponseEntity<?> listTerms(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(adminAcademicService.listTerms());
    }

    @PostMapping("/sections")
    @PreAuthorize("hasAnyAuthority('PERM_SUPER', 'PERM_REGISTRAR')")
    public ResponseEntity<?> createSection(
            @AuthenticationPrincipal User admin,
            @RequestBody CreateSectionBody body) {
        SubjectOffering saved = adminAcademicService.createSection(
                body.subjectId(), body.semesterId(), body.teacherId(), body.capacity(), body.lessonType(), admin);
        SubjectOffering detailed = subjectOfferingRepository.findByIdWithDetails(saved.getId()).orElse(saved);
        return ResponseEntity.ok(toSectionDto(detailed));
    }

    @GetMapping("/sections")
    @PreAuthorize("hasAnyAuthority('PERM_SUPER', 'PERM_REGISTRAR')")
    public ResponseEntity<?> listSections(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) Long semesterId) {
        return ResponseEntity.ok(adminAcademicService.listSections(semesterId).stream()
                .map(this::toSectionDto)
                .toList());
    }

    @PostMapping("/sections/{id}/assign-professor")
    @PreAuthorize("hasAnyAuthority('PERM_SUPER', 'PERM_REGISTRAR')")
    public ResponseEntity<?> assignProfessor(
            @AuthenticationPrincipal User admin,
            @PathVariable Long id,
            @RequestBody AssignProfessorBody body) {
        SubjectOffering saved = adminAcademicService.assignProfessor(id, body.teacherId(), admin);
        SubjectOffering detailed = subjectOfferingRepository.findByIdWithDetails(saved.getId()).orElse(saved);
        return ResponseEntity.ok(toSectionDto(detailed));
    }

    @PostMapping("/sections/{id}/meeting-times")
    @PreAuthorize("hasAnyAuthority('PERM_SUPER', 'PERM_REGISTRAR')")
    public ResponseEntity<?> addMeetingTime(
            @AuthenticationPrincipal User admin,
            @PathVariable Long id,
            @RequestBody MeetingTimeBody body) {
        return ResponseEntity.ok(toMeetingTimeDto(adminAcademicService.addMeetingTime(
                id, body.dayOfWeek(), LocalTime.parse(body.startTime()), LocalTime.parse(body.endTime()),
                body.room(), body.lessonType(), admin)));
    }

    @PostMapping("/windows")
    @PreAuthorize("hasAnyAuthority('PERM_SUPER', 'PERM_REGISTRAR')")
    public ResponseEntity<?> upsertWindow(
            @AuthenticationPrincipal User admin,
            @RequestBody WindowBody body) {
        return ResponseEntity.ok(toWindowDto(adminAcademicService.upsertWindow(
                body.semesterId(), body.type(), LocalDate.parse(body.startDate()),
                LocalDate.parse(body.endDate()), body.active(), admin)));
    }

    @GetMapping("/windows")
    @PreAuthorize("hasAnyAuthority('PERM_SUPER', 'PERM_REGISTRAR')")
    public ResponseEntity<?> listWindows(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(registrationWindowRepository.findAllWithSemesterOrderByStartDateDesc().stream()
                .map(this::toWindowDto)
                .toList());
    }

    @PostMapping("/enrollments/override")
    @PreAuthorize("hasAnyAuthority('PERM_SUPER', 'PERM_REGISTRAR')")
    public ResponseEntity<?> overrideEnrollment(
            @AuthenticationPrincipal User admin,
            @RequestBody EnrollmentOverrideBody body) {
        return ResponseEntity.ok(adminAcademicService.adminOverrideEnroll(
                body.studentId(), body.subjectOfferingId(), body.reason(), admin));
    }

    @GetMapping("/exams")
    @PreAuthorize("hasAnyAuthority('PERM_SUPER', 'PERM_REGISTRAR')")
    public ResponseEntity<?> listExams(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) Long semesterId) {
        Long semId = semesterId != null ? semesterId
                : semesterRepository.findByCurrentTrue().map(Semester::getId).orElse(null);
        if (semId == null) return ResponseEntity.ok(List.of());
        return ResponseEntity.ok(examScheduleService.listBySemester(semId).stream()
                .map(this::toExamDto)
                .toList());
    }

    @PostMapping("/exams")
    @PreAuthorize("hasAnyAuthority('PERM_SUPER', 'PERM_REGISTRAR')")
    public ResponseEntity<?> createExam(
            @AuthenticationPrincipal User admin,
            @RequestBody CreateExamBody body) {
        ExamSchedule saved = examScheduleService.createExamSession(
                body.sectionId(), LocalDate.parse(body.examDate()), LocalTime.parse(body.examTime()),
                body.room(), body.format(), admin);
        ExamSchedule detailed = examScheduleRepository.findByIdWithDetails(saved.getId()).orElse(saved);
        return ResponseEntity.ok(toExamDto(detailed));
    }

    @PutMapping("/exams/{id}")
    @PreAuthorize("hasAnyAuthority('PERM_SUPER', 'PERM_REGISTRAR')")
    public ResponseEntity<?> updateExam(
            @AuthenticationPrincipal User admin,
            @PathVariable Long id,
            @RequestBody CreateExamBody body) {
        ExamSchedule saved = examScheduleService.updateExamSession(
                id, LocalDate.parse(body.examDate()), LocalTime.parse(body.examTime()),
                body.room(), body.format(), admin);
        ExamSchedule detailed = examScheduleRepository.findByIdWithDetails(saved.getId()).orElse(saved);
        return ResponseEntity.ok(toExamDto(detailed));
    }

    @DeleteMapping("/exams/{id}")
    @PreAuthorize("hasAnyAuthority('PERM_SUPER', 'PERM_REGISTRAR')")
    public ResponseEntity<?> deleteExam(
            @AuthenticationPrincipal User admin,
            @PathVariable Long id) {
        examScheduleService.deleteExamSession(id, admin);
        return ResponseEntity.ok(Map.of("status", "deleted"));
    }

    @GetMapping("/fx")
    @PreAuthorize("hasAnyAuthority('PERM_SUPER', 'PERM_REGISTRAR')")
    public ResponseEntity<?> listFx(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(fxRegistrationService.listAll().stream()
                .map(this::toFxDto)
                .toList());
    }

    @PostMapping("/fx/{id}/status")
    @PreAuthorize("hasAnyAuthority('PERM_SUPER', 'PERM_REGISTRAR')")
    public ResponseEntity<?> updateFxStatus(
            @AuthenticationPrincipal User admin,
            @PathVariable Long id,
            @RequestBody FxStatusBody body) {
        return ResponseEntity.ok(toFxDto(fxRegistrationService.updateStatus(id, body.status(), admin)));
    }

    @GetMapping("/grade-change-requests")
    @PreAuthorize("hasAnyAuthority('PERM_SUPER', 'PERM_REGISTRAR')")
    public ResponseEntity<?> gradeChangeRequests(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "desc") String direction) {
        var pageable = apiPageableFactory.create(
                page, size, sort, direction, "createdAt",
                java.util.Set.of("createdAt", "status", "newValue", "oldValue"));
        var data = gradeChangeRequestRepository.findByStatus(
                        GradeChangeRequest.RequestStatus.SUBMITTED, pageable)
                .map(r -> new GradeChangeDto(r.getId(), r.getTeacher().getId(), r.getStudent().getId(),
                        r.getSubjectOffering().getId(), r.getOldValue(), r.getNewValue(),
                        r.getReason(), r.getStatus(), r.getCreatedAt()));
        return ResponseEntity.ok(ApiPageResponse.from(data));
    }

    @PostMapping("/grade-change-requests/{id}/review")
    @PreAuthorize("hasAnyAuthority('PERM_SUPER', 'PERM_REGISTRAR')")
    public ResponseEntity<?> reviewGradeChangeRequest(
            @AuthenticationPrincipal User admin,
            @PathVariable Long id,
            @RequestBody ReviewGradeChangeBody body) {
        GradeChangeRequest request = gradeChangeService.review(id, body.approve(), body.comment(), admin);
        return ResponseEntity.ok(new GradeChangeDto(
                request.getId(),
                request.getTeacher() != null ? request.getTeacher().getId() : null,
                request.getStudent() != null ? request.getStudent().getId() : null,
                request.getSubjectOffering() != null ? request.getSubjectOffering().getId() : null,
                request.getOldValue(), request.getNewValue(), request.getReason(),
                request.getStatus(), request.getCreatedAt()
        ));
    }

    @PostMapping("/students/{id}/status")
    @PreAuthorize("hasAnyAuthority('PERM_SUPER', 'PERM_REGISTRAR')")
    public ResponseEntity<?> updateStudentStatus(
            @AuthenticationPrincipal User admin,
            @PathVariable Long id,
            @RequestBody UpdateStudentStatusBody body) {
        if (body.status() == null) {
            throw new IllegalArgumentException("Student status is required");
        }
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));
        student.setStatus(body.status());
        studentRepository.save(student);
        return ResponseEntity.ok(Map.of("id", student.getId(), "status", student.getStatus()));
    }

    private SectionDto toSectionDto(SubjectOffering section) {
        return new SectionDto(
                section.getId(),
                section.getSubject() != null ? section.getSubject().getId() : null,
                section.getSubject() != null ? section.getSubject().getCode() : null,
                section.getSubject() != null ? section.getSubject().getName() : null,
                section.getSemester() != null ? section.getSemester().getId() : null,
                section.getSemester() != null ? section.getSemester().getName() : null,
                section.getTeacher() != null ? section.getTeacher().getId() : null,
                section.getTeacher() != null ? section.getTeacher().getName() : null,
                section.getCapacity(),
                section.getLessonType(),
                section.getDayOfWeek(),
                section.getStartTime(),
                section.getEndTime(),
                section.getRoom()
        );
    }

    private MeetingTimeDto toMeetingTimeDto(MeetingTime meetingTime) {
        return new MeetingTimeDto(
                meetingTime.getId(),
                meetingTime.getSubjectOffering() != null ? meetingTime.getSubjectOffering().getId() : null,
                meetingTime.getDayOfWeek(),
                meetingTime.getStartTime(),
                meetingTime.getEndTime(),
                meetingTime.getRoom(),
                meetingTime.getLessonType()
        );
    }

    private WindowDto toWindowDto(RegistrationWindow window) {
        return new WindowDto(
                window.getId(),
                window.getSemester() != null ? window.getSemester().getId() : null,
                window.getSemester() != null ? window.getSemester().getName() : null,
                window.getType(),
                window.getStartDate(),
                window.getEndDate(),
                window.isActive()
        );
    }

    private ExamDto toExamDto(ExamSchedule exam) {
        return new ExamDto(
                exam.getId(),
                exam.getSubjectOffering() != null ? exam.getSubjectOffering().getId() : null,
                exam.getSubjectOffering() != null && exam.getSubjectOffering().getSubject() != null
                        ? exam.getSubjectOffering().getSubject().getCode() : null,
                exam.getSubjectOffering() != null && exam.getSubjectOffering().getSubject() != null
                        ? exam.getSubjectOffering().getSubject().getName() : null,
                exam.getExamDate(),
                exam.getExamTime(),
                exam.getRoom(),
                exam.getFormat()
        );
    }

    private FxDto toFxDto(FxRegistration fx) {
        return new FxDto(
                fx.getId(),
                fx.getStudent() != null ? fx.getStudent().getId() : null,
                fx.getStudent() != null ? fx.getStudent().getName() : null,
                fx.getSubjectOffering() != null ? fx.getSubjectOffering().getId() : null,
                fx.getSubjectOffering() != null && fx.getSubjectOffering().getSubject() != null
                        ? fx.getSubjectOffering().getSubject().getCode() : null,
                fx.getSubjectOffering() != null && fx.getSubjectOffering().getSubject() != null
                        ? fx.getSubjectOffering().getSubject().getName() : null,
                fx.getStatus(),
                fx.getCreatedAt()
        );
    }

    public record CreateTermBody(String name, String startDate, String endDate, boolean current) {}
    public record SectionDto(Long id, Long subjectId, String subjectCode, String subjectName,
                             Long semesterId, String semesterName, Long teacherId, String teacherName,
                             int capacity, SubjectOffering.LessonType lessonType,
                             java.time.DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime, String room) {}
    public record CreateSectionBody(Long subjectId, Long semesterId, Long teacherId, int capacity,
                                    SubjectOffering.LessonType lessonType) {}
    public record AssignProfessorBody(Long teacherId) {}
    public record MeetingTimeDto(Long id, Long sectionId, java.time.DayOfWeek dayOfWeek,
                                 LocalTime startTime, LocalTime endTime, String room,
                                 SubjectOffering.LessonType lessonType) {}
    public record MeetingTimeBody(java.time.DayOfWeek dayOfWeek, String startTime, String endTime,
                                  String room, SubjectOffering.LessonType lessonType) {}
    public record WindowDto(Long id, Long semesterId, String semesterName,
                            RegistrationWindow.WindowType type, LocalDate startDate,
                            LocalDate endDate, boolean active) {}
    public record WindowBody(Long semesterId, RegistrationWindow.WindowType type, String startDate,
                             String endDate, boolean active) {}
    public record EnrollmentOverrideBody(Long studentId, Long subjectOfferingId, String reason) {}
    public record ExamDto(Long id, Long sectionId, String subjectCode, String subjectName,
                          LocalDate examDate, LocalTime examTime, String room, String format) {}
    public record CreateExamBody(Long sectionId, String examDate, String examTime, String room, String format) {}
    public record FxDto(Long id, Long studentId, String studentName, Long sectionId,
                        String subjectCode, String subjectName, FxRegistration.FxStatus status,
                        Instant createdAt) {}
    public record FxStatusBody(FxRegistration.FxStatus status) {}
    public record GradeChangeDto(Long id, Long teacherId, Long studentId, Long sectionId,
                                 Double oldValue, Double newValue, String reason,
                                 GradeChangeRequest.RequestStatus status, Instant createdAt) {}
    public record ReviewGradeChangeBody(boolean approve, String comment) {}
    public record UpdateStudentStatusBody(Student.StudentStatus status) {}
}
