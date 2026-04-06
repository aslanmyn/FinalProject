package ru.kors.finalproject.controller.api.v1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.crypto.password.PasswordEncoder;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import ru.kors.finalproject.entity.*;
import ru.kors.finalproject.repository.*;
import ru.kors.finalproject.service.*;
import ru.kors.finalproject.web.api.v1.ApiPageResponse;
import ru.kors.finalproject.web.api.v1.ApiPageableFactory;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Locale;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Admin Academic", description = "Academic operations for admins: terms, sections, windows, exams, FX, grade changes, and student creation/update.")
@SecurityRequirement(name = "Bearer")
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
    private final UserRepository userRepository;
    private final FacultyRepository facultyRepository;
    private final ProgramRepository programRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserRoleDetector userRoleDetector;
    private final ApiPageableFactory apiPageableFactory;

    @PostMapping("/terms")
    @PreAuthorize("hasAnyAuthority('PERM_SUPER', 'PERM_REGISTRAR')")
    @Operation(summary = "Create term", description = "Creates a new semester/term.")
    public ResponseEntity<?> createTerm(
            @AuthenticationPrincipal User admin,
            @RequestBody CreateTermBody body) {
        Semester saved = adminAcademicService.createTerm(
                body.name(), LocalDate.parse(body.startDate()), LocalDate.parse(body.endDate()), body.current(), admin);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/terms")
    @PreAuthorize("hasAnyAuthority('PERM_SUPER', 'PERM_REGISTRAR')")
    @Operation(summary = "List terms", description = "Returns all academic terms.")
    public ResponseEntity<?> listTerms(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(adminAcademicService.listTerms());
    }

    @PostMapping("/sections")
    @PreAuthorize("hasAnyAuthority('PERM_SUPER', 'PERM_REGISTRAR')")
    @Operation(summary = "Create section", description = "Creates a new subject offering for a semester.")
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
    @Operation(summary = "List sections", description = "Returns sections, optionally filtered by semester.")
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
    @Operation(summary = "Create or update registration window", description = "Creates or updates a registration/add-drop/FX window for a semester.")
    public ResponseEntity<?> upsertWindow(
            @AuthenticationPrincipal User admin,
            @RequestBody WindowBody body) {
        return ResponseEntity.ok(toWindowDto(adminAcademicService.upsertWindow(
                body.semesterId(), body.type(), LocalDate.parse(body.startDate()),
                LocalDate.parse(body.endDate()), body.active(), admin)));
    }

    @GetMapping("/windows")
    @PreAuthorize("hasAnyAuthority('PERM_SUPER', 'PERM_REGISTRAR')")
    @Operation(summary = "List registration windows", description = "Returns registration windows for all semesters.")
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

    @PostMapping("/students")
    @Transactional
    @PreAuthorize("hasAnyAuthority('PERM_SUPER', 'PERM_REGISTRAR')")
    @Operation(summary = "Create student", description = "Creates both the user account and the student profile in one request.")
    public ResponseEntity<?> createStudent(
            @AuthenticationPrincipal User admin,
            @RequestBody CreateStudentBody body) {
        String email = normalizeEmail(body.email());
        if (userRoleDetector.detectRole(email) != UserRole.STUDENT) {
            throw new IllegalArgumentException("Student email must use the a_surname@kbtu.kz format");
        }
        if (body.password() == null || body.password().isBlank() || body.password().length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters");
        }
        if (body.fullName() == null || body.fullName().isBlank()) {
            throw new IllegalArgumentException("Full name is required");
        }
        if (body.course() < 1) {
            throw new IllegalArgumentException("Course must be at least 1");
        }
        if (body.creditsEarned() < 0) {
            throw new IllegalArgumentException("Credits earned cannot be negative");
        }
        if (userRepository.existsByEmail(email) || studentRepository.findByEmail(email).isPresent()) {
            throw new IllegalStateException("Student with this email already exists");
        }

        Program program = programRepository.findById(body.programId())
                .orElseThrow(() -> new IllegalArgumentException("Program not found"));
        Faculty faculty = facultyRepository.findById(body.facultyId())
                .orElseThrow(() -> new IllegalArgumentException("Faculty not found"));
        Semester semester = semesterRepository.findById(body.currentSemesterId())
                .orElseThrow(() -> new IllegalArgumentException("Semester not found"));

        if (program.getFaculty() != null
                && program.getFaculty().getId() != null
                && !program.getFaculty().getId().equals(faculty.getId())) {
            throw new IllegalArgumentException("Program does not belong to the selected faculty");
        }

        User user = userRepository.save(User.builder()
                .email(email)
                .password(passwordEncoder.encode(body.password()))
                .fullName(body.fullName().trim())
                .role(User.UserRole.STUDENT)
                .enabled(body.enabled() == null || body.enabled())
                .build());

        Student student = studentRepository.save(Student.builder()
                .email(email)
                .name(body.fullName().trim())
                .course(body.course())
                .groupName(blankToNull(body.groupName()))
                .status(body.status() != null ? body.status() : Student.StudentStatus.ACTIVE)
                .faculty(faculty)
                .program(program)
                .currentSemester(semester)
                .creditsEarned(body.creditsEarned())
                .passportNumber(blankToNull(body.passportNumber()))
                .address(blankToNull(body.address()))
                .phone(blankToNull(body.phone()))
                .emergencyContact(blankToNull(body.emergencyContact()))
                .build());

        return ResponseEntity.ok(new CreatedStudentDto(
                user.getId(),
                student.getId(),
                user.getEmail(),
                user.getFullName(),
                student.getCourse(),
                student.getStatus(),
                faculty.getId(),
                faculty.getName(),
                program.getId(),
                program.getName(),
                semester.getId(),
                semester.getName(),
                user.isEnabled()
        ));
    }

    @PutMapping("/students/{id}")
    @Transactional
    @PreAuthorize("hasAnyAuthority('PERM_SUPER', 'PERM_REGISTRAR')")
    @Operation(summary = "Update student", description = "Updates an existing student's user account and profile fields in one request.")
    public ResponseEntity<?> updateStudent(
            @AuthenticationPrincipal User admin,
            @PathVariable Long id,
            @RequestBody UpdateStudentBody body) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));
        User user = userRepository.findByEmail(student.getEmail())
                .orElseThrow(() -> new IllegalStateException("User account for student not found"));

        String email = normalizeEmail(body.email());
        if (userRoleDetector.detectRole(email) != UserRole.STUDENT) {
            throw new IllegalArgumentException("Student email must use the a_surname@kbtu.kz format");
        }
        if (body.fullName() == null || body.fullName().isBlank()) {
            throw new IllegalArgumentException("Full name is required");
        }
        if (body.course() < 1) {
            throw new IllegalArgumentException("Course must be at least 1");
        }
        if (body.creditsEarned() < 0) {
            throw new IllegalArgumentException("Credits earned cannot be negative");
        }
        if (!email.equals(student.getEmail())) {
            if (userRepository.existsByEmail(email) || studentRepository.findByEmail(email).isPresent()) {
                throw new IllegalStateException("Student with this email already exists");
            }
        }

        Program program = programRepository.findById(body.programId())
                .orElseThrow(() -> new IllegalArgumentException("Program not found"));
        Faculty faculty = facultyRepository.findById(body.facultyId())
                .orElseThrow(() -> new IllegalArgumentException("Faculty not found"));
        Semester semester = semesterRepository.findById(body.currentSemesterId())
                .orElseThrow(() -> new IllegalArgumentException("Semester not found"));

        if (program.getFaculty() != null
                && program.getFaculty().getId() != null
                && !program.getFaculty().getId().equals(faculty.getId())) {
            throw new IllegalArgumentException("Program does not belong to the selected faculty");
        }

        if (body.password() != null && !body.password().isBlank()) {
            if (body.password().length() < 6) {
                throw new IllegalArgumentException("Password must be at least 6 characters");
            }
            user.setPassword(passwordEncoder.encode(body.password()));
        }

        user.setEmail(email);
        user.setFullName(body.fullName().trim());
        if (body.enabled() != null) {
            user.setEnabled(body.enabled());
        }
        userRepository.save(user);

        student.setEmail(email);
        student.setName(body.fullName().trim());
        student.setCourse(body.course());
        student.setGroupName(blankToNull(body.groupName()));
        student.setStatus(body.status() != null ? body.status() : student.getStatus());
        student.setFaculty(faculty);
        student.setProgram(program);
        student.setCurrentSemester(semester);
        student.setCreditsEarned(body.creditsEarned());
        student.setPassportNumber(blankToNull(body.passportNumber()));
        student.setAddress(blankToNull(body.address()));
        student.setPhone(blankToNull(body.phone()));
        student.setEmergencyContact(blankToNull(body.emergencyContact()));
        studentRepository.save(student);

        return ResponseEntity.ok(new CreatedStudentDto(
                user.getId(),
                student.getId(),
                user.getEmail(),
                user.getFullName(),
                student.getCourse(),
                student.getStatus(),
                faculty.getId(),
                faculty.getName(),
                program.getId(),
                program.getName(),
                semester.getId(),
                semester.getName(),
                user.isEnabled()
        ));
    }

    @PostMapping("/students/{id}/status")
    @PreAuthorize("hasAnyAuthority('PERM_SUPER', 'PERM_REGISTRAR')")
    @Operation(summary = "Update student status", description = "Changes only the status of an existing student.")
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

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
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
    public record CreateStudentBody(
            @Schema(example = "a_testov@kbtu.kz") String email,
            @Schema(example = "student123") String password,
            @Schema(example = "Aslan Testov") String fullName,
            @Schema(example = "1") Long facultyId,
            @Schema(example = "1") Long programId,
            @Schema(example = "8") Long currentSemesterId,
            @Schema(example = "2") int course,
            @Schema(example = "TBD") String groupName,
            Student.StudentStatus status,
            @Schema(example = "36") int creditsEarned,
            @Schema(example = "N1234567") String passportNumber,
            @Schema(example = "Almaty") String address,
            @Schema(example = "+77010000000") String phone,
            @Schema(example = "+77020000000") String emergencyContact,
            @Schema(example = "true") Boolean enabled) {}
    public record CreatedStudentDto(
            Long userId,
            Long studentId,
            String email,
            String fullName,
            int course,
            Student.StudentStatus status,
            Long facultyId,
            String facultyName,
            Long programId,
            String programName,
            Long currentSemesterId,
            String currentSemesterName,
            boolean enabled) {}
    public record UpdateStudentBody(
            @Schema(example = "a_testov@kbtu.kz") String email,
            @Schema(example = "student123") String password,
            @Schema(example = "Aslan Testov") String fullName,
            @Schema(example = "1") Long facultyId,
            @Schema(example = "1") Long programId,
            @Schema(example = "8") Long currentSemesterId,
            @Schema(example = "3") int course,
            @Schema(example = "TBD") String groupName,
            Student.StudentStatus status,
            @Schema(example = "72") int creditsEarned,
            @Schema(example = "N1234567") String passportNumber,
            @Schema(example = "Almaty") String address,
            @Schema(example = "+77010000000") String phone,
            @Schema(example = "+77020000000") String emergencyContact,
            @Schema(example = "true") Boolean enabled) {}
    public record UpdateStudentStatusBody(Student.StudentStatus status) {}
}
