package ru.kors.finalproject.controller.api.v1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.kors.finalproject.entity.*;
import ru.kors.finalproject.repository.*;
import ru.kors.finalproject.service.AttendanceFlowService;
import ru.kors.finalproject.service.GpaCalculationService;
import ru.kors.finalproject.web.api.v1.CurrentUserHelper;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/student")
@RequiredArgsConstructor
@Tag(name = "Student Academic", description = "Schedule, journal, transcript, attendance, exams, and academic session actions for the current student.")
@SecurityRequirement(name = "Bearer")
public class StudentAcademicV1Controller {

    private final CurrentUserHelper currentUserHelper;
    private final RegistrationRepository registrationRepository;
    private final GradeRepository gradeRepository;
    private final FinalGradeRepository finalGradeRepository;
    private final AttendanceRepository attendanceRepository;
    private final ExamScheduleRepository examScheduleRepository;
    private final SemesterRepository semesterRepository;
    private final GpaCalculationService gpaCalculationService;
    private final AttendanceFlowService attendanceFlowService;

    @GetMapping("/schedule")
    @Operation(summary = "Get student schedule", description = "Returns weekly timetable items for the selected semester or the student's current semester.")
    public ResponseEntity<?> schedule(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) Long semesterId) {
        Student student = currentUserHelper.requireStudent(user);
        List<Registration> enrollments = registrationRepository.findByStudentIdWithDetails(student.getId()).stream()
                .filter(r -> r.getStatus() != Registration.RegistrationStatus.DROPPED)
                .toList();
        Long effectiveSemesterId = semesterId != null
                ? semesterId
                : student.getCurrentSemester() != null
                ? student.getCurrentSemester().getId()
                : semesterRepository.findByCurrentTrue().map(Semester::getId).orElse(null);
        List<ScheduleItemDto> items = enrollments.stream()
                .filter(r -> effectiveSemesterId == null
                        || (r.getSubjectOffering().getSemester() != null
                        && Objects.equals(r.getSubjectOffering().getSemester().getId(), effectiveSemesterId)))
                .map(this::toScheduleItemDto)
                .toList();
        return ResponseEntity.ok(items);
    }

    @GetMapping("/schedule/options")
    @Operation(summary = "Get schedule filter options", description = "Returns available semesters for the student's schedule page.")
    public ResponseEntity<?> scheduleOptions(@AuthenticationPrincipal User user) {
        Student student = currentUserHelper.requireStudent(user);
        List<SemesterOptionDto> semesters = registrationRepository.findByStudentIdWithDetails(student.getId()).stream()
                .filter(r -> r.getStatus() != Registration.RegistrationStatus.DROPPED)
                .map(r -> r.getSubjectOffering() != null ? r.getSubjectOffering().getSemester() : null)
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        Semester::getId,
                        semester -> semester,
                        (left, right) -> left,
                        LinkedHashMap::new
                ))
                .values().stream()
                .sorted(Comparator.comparing(Semester::getStartDate, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .map(this::toSemesterOptionDto)
                .toList();

        Long currentSemesterId = student.getCurrentSemester() != null
                ? student.getCurrentSemester().getId()
                : semesterRepository.findByCurrentTrue().map(Semester::getId).orElse(null);

        return ResponseEntity.ok(new ScheduleOptionsDto(currentSemesterId, semesters));
    }

    @GetMapping("/journal")
    @Operation(summary = "Get student journal", description = "Returns attestation, final, and total score rows for the selected semester.")
    public ResponseEntity<?> journal(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) Long semesterId) {
        Student student = currentUserHelper.requireStudent(user);
        List<Grade> grades = gradeRepository.findByStudentIdAndPublishedTrueWithDetails(student.getId());
        List<FinalGrade> finalGrades = finalGradeRepository.findByStudentIdAndPublishedTrueWithDetails(student.getId());
        Long effectiveSemesterId = semesterId != null
                ? semesterId
                : student.getCurrentSemester() != null
                ? student.getCurrentSemester().getId()
                : buildJournalSemesterOptions(student.getId()).stream()
                .findFirst()
                .map(SemesterOptionDto::id)
                .orElse(null);

        Map<Long, JournalCourseRowAccumulator> rows = new LinkedHashMap<>();

        grades.stream()
                .filter(g -> effectiveSemesterId == null
                        || (g.getSubjectOffering().getSemester() != null
                        && Objects.equals(g.getSubjectOffering().getSemester().getId(), effectiveSemesterId)))
                .forEach(grade -> {
                    SubjectOffering offering = grade.getSubjectOffering();
                    if (offering == null || offering.getSubject() == null) {
                        return;
                    }
                    Semester semester = offering.getSemester();
                    JournalCourseRowAccumulator row = rows.computeIfAbsent(offering.getId(), id -> new JournalCourseRowAccumulator(
                            offering.getId(),
                            offering.getSubject().getCode(),
                            offering.getSubject().getName(),
                            semester != null ? semester.getId() : null,
                            semester != null ? semester.getName() : null,
                            semester != null ? extractAcademicYear(semester.getName()) : null,
                            semester != null ? extractSeason(semester.getName()) : null
                    ));

                    String componentName = grade.getComponent() != null ? grade.getComponent().getName() : grade.getType().name();
                    if (isAttestationOne(componentName, grade)) {
                        row.attestation1 = grade.getGradeValue();
                        row.attestation1Max = grade.getMaxGradeValue();
                    } else if (isAttestationTwo(componentName, grade)) {
                        row.attestation2 = grade.getGradeValue();
                        row.attestation2Max = grade.getMaxGradeValue();
                    } else if (isFinalComponent(componentName, grade)) {
                        row.finalExam = grade.getGradeValue();
                        row.finalExamMax = grade.getMaxGradeValue();
                    }
                });

        finalGrades.stream()
                .filter(finalGrade -> effectiveSemesterId == null
                        || (finalGrade.getSubjectOffering().getSemester() != null
                        && Objects.equals(finalGrade.getSubjectOffering().getSemester().getId(), effectiveSemesterId)))
                .forEach(finalGrade -> {
                    SubjectOffering offering = finalGrade.getSubjectOffering();
                    if (offering == null || offering.getSubject() == null) {
                        return;
                    }
                    Semester semester = offering.getSemester();
                    JournalCourseRowAccumulator row = rows.computeIfAbsent(offering.getId(), id -> new JournalCourseRowAccumulator(
                            offering.getId(),
                            offering.getSubject().getCode(),
                            offering.getSubject().getName(),
                            semester != null ? semester.getId() : null,
                            semester != null ? semester.getName() : null,
                            semester != null ? extractAcademicYear(semester.getName()) : null,
                            semester != null ? extractSeason(semester.getName()) : null
                    ));
                    row.totalScore = finalGrade.getNumericValue();
                    row.letterValue = finalGrade.getLetterValue();
                });

        return ResponseEntity.ok(rows.values().stream()
                .sorted(Comparator.comparing(JournalCourseRowAccumulator::courseCode))
                .map(JournalCourseRowAccumulator::toDto)
                .toList());
    }

    @GetMapping("/journal/options")
    @Operation(summary = "Get journal filter options", description = "Returns available semesters for the student journal page.")
    public ResponseEntity<?> journalOptions(@AuthenticationPrincipal User user) {
        Student student = currentUserHelper.requireStudent(user);
        List<SemesterOptionDto> semesters = buildJournalSemesterOptions(student.getId());
        Long currentSemesterId = student.getCurrentSemester() != null
                ? student.getCurrentSemester().getId()
                : semesters.stream().findFirst().map(SemesterOptionDto::id).orElse(null);
        return ResponseEntity.ok(new JournalOptionsDto(currentSemesterId, semesters));
    }

    @GetMapping("/transcript")
    @Operation(summary = "Get transcript", description = "Returns published final grades and calculated GPA for the current student.")
    public ResponseEntity<?> transcript(@AuthenticationPrincipal User user) {
        Student student = currentUserHelper.requireStudent(user);
        List<FinalGrade> grades = finalGradeRepository.findByStudentIdAndPublishedTrueWithDetails(student.getId());
        double gpa = gpaCalculationService.calculatePublishedGpa(grades);
        return ResponseEntity.ok(Map.of(
                "studentId", student.getId(),
                "studentName", student.getName(),
                "gpa", gpa,
                "totalCredits", student.getCreditsEarned(),
                "finalGrades", grades.stream().map(g -> new TranscriptItemDto(
                        g.getId(), g.getSubjectOffering().getSubject().getCode(),
                        g.getSubjectOffering().getSubject().getName(),
                        g.getSubjectOffering().getSubject().getCredits(),
                        g.getNumericValue(), g.getLetterValue(), g.getPoints(), g.getStatus()
                )).toList()
        ));
    }

    @GetMapping("/attendance")
    @Operation(summary = "Get attendance dashboard", description = "Returns attendance history, active check-in sessions, and summary statistics.")
    public ResponseEntity<?> attendance(@AuthenticationPrincipal User user) {
        Student student = currentUserHelper.requireStudent(user);
        List<Attendance> items = attendanceRepository.findByStudentIdWithDetails(student.getId());
        List<AttendanceFlowService.StudentActiveAttendanceSessionView> activeSessions =
                attendanceFlowService.getActiveSessionsForStudent(student);
        long present = items.stream().filter(a -> a.getStatus() == Attendance.AttendanceStatus.PRESENT).count();
        long late = items.stream().filter(a -> a.getStatus() == Attendance.AttendanceStatus.LATE).count();
        long absent = items.stream().filter(a -> a.getStatus() == Attendance.AttendanceStatus.ABSENT).count();
        return ResponseEntity.ok(Map.of(
                "records", items.stream().map(a -> Map.of(
                        "date", a.getDate().toString(),
                        "subjectCode", a.getSubjectOffering().getSubject().getCode(),
                        "subjectName", a.getSubjectOffering().getSubject().getName(),
                        "status", a.getStatus(),
                        "reason", a.getReason() != null ? a.getReason() : ""
                )).toList(),
                "activeSessions", activeSessions.stream().map(this::toStudentActiveAttendanceDto).toList(),
                "summary", Map.of("present", present, "late", late, "absent", absent,
                        "total", items.size(),
                        "percentage", items.isEmpty() ? 0.0 : ((present + late) * 100.0 / items.size()))
        ));
    }

    @GetMapping("/attendance/active")
    @Operation(summary = "Get active attendance sessions", description = "Returns active self check-in attendance sessions available to the student right now.")
    public ResponseEntity<?> activeAttendance(@AuthenticationPrincipal User user) {
        Student student = currentUserHelper.requireStudent(user);
        return ResponseEntity.ok(attendanceFlowService.getActiveSessionsForStudent(student).stream()
                .map(this::toStudentActiveAttendanceDto)
                .toList());
    }

    @PostMapping("/attendance-sessions/{sessionId}/check-in")
    @Operation(summary = "Check in to attendance session", description = "Marks the student as present in an open attendance session. Code is required only for CODE mode.")
    public ResponseEntity<?> checkInAttendance(
            @AuthenticationPrincipal User user,
            @PathVariable Long sessionId,
            @RequestBody(required = false) StudentAttendanceCheckInBody body) {
        Student student = currentUserHelper.requireStudent(user);
        Attendance attendance = attendanceFlowService.checkIn(student, sessionId, body != null ? body.code() : null);
        return ResponseEntity.ok(Map.of(
                "sessionId", sessionId,
                "status", attendance.getStatus(),
                "markedBy", attendance.getMarkedBy(),
                "teacherConfirmed", attendance.isTeacherConfirmed(),
                "updatedAt", attendance.getUpdatedAt()
        ));
    }

    @GetMapping("/exam-schedule")
    @Operation(summary = "Get exam schedule", description = "Returns current semester exams for the student's active registrations.")
    public ResponseEntity<?> examSchedule(@AuthenticationPrincipal User user) {
        Student student = currentUserHelper.requireStudent(user);
        List<Registration> enrollments = registrationRepository.findActiveByStudentIdWithDetails(student.getId());
        List<Long> sectionIds = enrollments.stream().map(r -> r.getSubjectOffering().getId()).toList();
        Long semId = semesterRepository.findByCurrentTrue().map(Semester::getId).orElse(null);
        if (semId == null) return ResponseEntity.ok(List.of());
        List<ExamSchedule> exams = examScheduleRepository.findBySemesterIdWithDetails(semId)
                .stream().filter(e -> sectionIds.contains(e.getSubjectOffering().getId())).toList();
        return ResponseEntity.ok(exams.stream().map(e -> Map.of(
                "id", (Object) e.getId(),
                "subjectCode", e.getSubjectOffering().getSubject().getCode(),
                "subjectName", e.getSubjectOffering().getSubject().getName(),
                "examDate", e.getExamDate().toString(),
                "examTime", e.getExamTime().toString(),
                "room", e.getRoom() != null ? e.getRoom() : "",
                "format", e.getFormat() != null ? e.getFormat() : ""
        )).toList());
    }

    private List<SemesterOptionDto> buildJournalSemesterOptions(Long studentId) {
        Map<Long, Semester> semesters = new LinkedHashMap<>();

        gradeRepository.findByStudentIdAndPublishedTrueWithDetails(studentId).stream()
                .map(grade -> grade.getSubjectOffering() != null ? grade.getSubjectOffering().getSemester() : null)
                .filter(Objects::nonNull)
                .forEach(semester -> semesters.putIfAbsent(semester.getId(), semester));

        finalGradeRepository.findByStudentIdAndPublishedTrueWithDetails(studentId).stream()
                .map(finalGrade -> finalGrade.getSubjectOffering() != null ? finalGrade.getSubjectOffering().getSemester() : null)
                .filter(Objects::nonNull)
                .forEach(semester -> semesters.putIfAbsent(semester.getId(), semester));

        return semesters.values().stream()
                .sorted(Comparator.comparing(Semester::getStartDate, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .map(this::toSemesterOptionDto)
                .toList();
    }

    private boolean isAttestationOne(String componentName, Grade grade) {
        String normalized = normalizeComponent(componentName, grade);
        return normalized.contains("attestation 1");
    }

    private boolean isAttestationTwo(String componentName, Grade grade) {
        String normalized = normalizeComponent(componentName, grade);
        return normalized.contains("attestation 2");
    }

    private boolean isFinalComponent(String componentName, Grade grade) {
        String normalized = normalizeComponent(componentName, grade);
        return normalized.contains("final") || grade.getType() == Grade.GradeType.FINAL;
    }

    private String normalizeComponent(String componentName, Grade grade) {
        List<String> parts = new ArrayList<>();
        if (componentName != null) {
            parts.add(componentName.toLowerCase());
        }
        if (grade.getComment() != null) {
            parts.add(grade.getComment().toLowerCase());
        }
        return String.join(" ", parts);
    }

    private ScheduleItemDto toScheduleItemDto(Registration registration) {
        SubjectOffering offering = registration.getSubjectOffering();
        Semester semester = offering != null ? offering.getSemester() : null;
        return new ScheduleItemDto(
                offering != null ? offering.getId() : null,
                offering != null && offering.getSubject() != null ? offering.getSubject().getCode() : null,
                offering != null && offering.getSubject() != null ? offering.getSubject().getName() : null,
                offering != null ? offering.getDayOfWeek() : null,
                offering != null ? offering.getStartTime() : null,
                offering != null ? offering.getEndTime() : null,
                offering != null ? offering.getRoom() : null,
                offering != null && offering.getTeacher() != null ? offering.getTeacher().getName() : null,
                registration.getStatus(),
                semester != null ? semester.getId() : null,
                semester != null ? semester.getName() : null,
                semester != null ? extractAcademicYear(semester.getName()) : null,
                semester != null ? extractSeason(semester.getName()) : null,
                offering != null ? offering.getLessonType() : null
        );
    }

    private SemesterOptionDto toSemesterOptionDto(Semester semester) {
        return new SemesterOptionDto(
                semester.getId(),
                semester.getName(),
                extractAcademicYear(semester.getName()),
                extractSeason(semester.getName()),
                semester.isCurrent()
        );
    }

    private String extractAcademicYear(String semesterName) {
        if (semesterName == null || semesterName.isBlank()) {
            return "";
        }
        int separatorIndex = semesterName.lastIndexOf(' ');
        return separatorIndex > 0 ? semesterName.substring(0, separatorIndex).trim() : semesterName;
    }

    private String extractSeason(String semesterName) {
        if (semesterName == null || semesterName.isBlank()) {
            return "";
        }
        int separatorIndex = semesterName.lastIndexOf(' ');
        return separatorIndex > 0 ? semesterName.substring(separatorIndex + 1).trim() : semesterName;
    }

    public record ScheduleItemDto(Long sectionId, String courseCode, String courseName,
                                  java.time.DayOfWeek dayOfWeek, java.time.LocalTime startTime,
                                  java.time.LocalTime endTime, String room, String teacherName,
                                  Registration.RegistrationStatus status, Long semesterId,
                                  String semesterName, String academicYear, String season,
                                  SubjectOffering.LessonType lessonType) {}
    public record ScheduleOptionsDto(Long currentSemesterId, List<SemesterOptionDto> semesters) {}
    public record SemesterOptionDto(Long id, String name, String academicYear, String season, boolean current) {}
    public record JournalCourseRowDto(Long sectionId, String courseCode, String courseName,
                                     Long semesterId, String semesterName, String academicYear,
                                     String season, Double attestation1, Double attestation1Max,
                                     Double attestation2, Double attestation2Max, Double finalExam,
                                     Double finalExamMax, Double totalScore, String letterValue) {}
    public record JournalOptionsDto(Long currentSemesterId, List<SemesterOptionDto> semesters) {}
    public record TranscriptItemDto(Long id, String courseCode, String courseName, int credits,
                                    double numericValue, String letterValue, double points,
                                    FinalGrade.FinalGradeStatus status) {}
    public record StudentAttendanceActiveSessionDto(
            Long sessionId,
            Long sectionId,
            String subjectCode,
            String subjectName,
            String teacherName,
            String classDate,
            String attendanceCloseAt,
            AttendanceSession.CheckInMode checkInMode,
            Attendance.AttendanceStatus currentStatus,
            Attendance.MarkedBy markedBy,
            boolean teacherConfirmed,
            Registration.RegistrationStatus registrationStatus
    ) {}
    public record StudentAttendanceCheckInBody(String code) {}

    private static class JournalCourseRowAccumulator {
        private final Long sectionId;
        private final String courseCode;
        private final String courseName;
        private final Long semesterId;
        private final String semesterName;
        private final String academicYear;
        private final String season;
        private Double attestation1;
        private Double attestation1Max;
        private Double attestation2;
        private Double attestation2Max;
        private Double finalExam;
        private Double finalExamMax;
        private Double totalScore;
        private String letterValue;

        private JournalCourseRowAccumulator(Long sectionId, String courseCode, String courseName,
                                            Long semesterId, String semesterName, String academicYear,
                                            String season) {
            this.sectionId = sectionId;
            this.courseCode = courseCode;
            this.courseName = courseName;
            this.semesterId = semesterId;
            this.semesterName = semesterName;
            this.academicYear = academicYear;
            this.season = season;
        }

        private String courseCode() {
            return courseCode;
        }

        private JournalCourseRowDto toDto() {
            return new JournalCourseRowDto(
                    sectionId, courseCode, courseName, semesterId, semesterName, academicYear, season,
                    attestation1, attestation1Max, attestation2, attestation2Max,
                    finalExam, finalExamMax, totalScore, letterValue
            );
        }
    }

    private StudentAttendanceActiveSessionDto toStudentActiveAttendanceDto(
            AttendanceFlowService.StudentActiveAttendanceSessionView session
    ) {
        return new StudentAttendanceActiveSessionDto(
                session.sessionId(),
                session.sectionId(),
                session.subjectCode(),
                session.subjectName(),
                session.teacherName(),
                session.classDate() != null ? session.classDate().toString() : null,
                session.attendanceCloseAt() != null ? session.attendanceCloseAt().toString() : null,
                session.checkInMode(),
                session.currentStatus(),
                session.markedBy(),
                session.teacherConfirmed(),
                session.registrationStatus()
        );
    }
}
