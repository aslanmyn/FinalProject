package ru.kors.finalproject.controller.api.v1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import ru.kors.finalproject.entity.*;
import ru.kors.finalproject.repository.*;
import ru.kors.finalproject.service.AttendanceFlowService;
import ru.kors.finalproject.service.CourseMaterialService;
import ru.kors.finalproject.service.FileLinkService;
import ru.kors.finalproject.service.GpaCalculationService;
import ru.kors.finalproject.web.api.v1.CurrentUserHelper;

import java.time.Instant;
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
    private final AssessmentComponentRepository assessmentComponentRepository;
    private final CourseAnnouncementRepository courseAnnouncementRepository;
    private final ExamScheduleRepository examScheduleRepository;
    private final SemesterRepository semesterRepository;
    private final GpaCalculationService gpaCalculationService;
    private final AttendanceFlowService attendanceFlowService;
    private final CourseMaterialService courseMaterialService;
    private final FileLinkService fileLinkService;

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
                .flatMap(registration -> toScheduleItemDtos(registration).stream())
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

    @GetMapping("/sections/{sectionId}")
    @Transactional(readOnly = true)
    @Operation(summary = "Get student section detail", description = "Returns a complete section overview for the current student including grades, attendance, exam, announcements, and materials.")
    public ResponseEntity<?> sectionDetail(
            @AuthenticationPrincipal User user,
            @PathVariable Long sectionId) {
        Student student = currentUserHelper.requireStudent(user);
        Registration registration = registrationRepository.findByStudentIdAndSubjectOfferingIdWithDetails(student.getId(), sectionId)
                .orElseThrow(() -> new IllegalArgumentException("Section not found for current student"));
        SubjectOffering offering = registration.getSubjectOffering();
        if (offering == null || offering.getSubject() == null) {
            throw new IllegalArgumentException("Section details are incomplete");
        }

        List<Grade> publishedGrades = gradeRepository.findByStudentIdAndPublishedTrueWithDetails(student.getId()).stream()
                .filter(grade -> grade.getSubjectOffering() != null && Objects.equals(grade.getSubjectOffering().getId(), sectionId))
                .sorted(Comparator
                        .comparing((Grade grade) -> grade.getComponent() != null ? grade.getComponent().getCreatedAt() : grade.getCreatedAt(),
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Grade::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        List<AssessmentComponent> publishedComponents = assessmentComponentRepository
                .findBySubjectOfferingIdWithDetailsOrderByCreatedAtAsc(sectionId).stream()
                .filter(AssessmentComponent::isPublished)
                .toList();

        Map<Long, Grade> gradesByComponentId = publishedGrades.stream()
                .filter(grade -> grade.getComponent() != null)
                .collect(Collectors.toMap(
                        grade -> grade.getComponent().getId(),
                        grade -> grade,
                        (left, right) -> right,
                        LinkedHashMap::new
                ));

        Set<Long> publishedComponentIds = publishedComponents.stream()
                .map(AssessmentComponent::getId)
                .collect(Collectors.toSet());

        List<StudentSectionComponentGradeDto> componentGrades = new ArrayList<>();
        for (AssessmentComponent component : publishedComponents) {
            componentGrades.add(toComponentGradeDto(component, gradesByComponentId.get(component.getId())));
        }
        publishedGrades.stream()
                .filter(grade -> grade.getComponent() == null || !publishedComponentIds.contains(grade.getComponent().getId()))
                .map(grade -> toComponentGradeDto(null, grade))
                .forEach(componentGrades::add);

        FinalGrade finalGrade = finalGradeRepository.findByStudentIdAndPublishedTrueWithDetails(student.getId()).stream()
                .filter(item -> item.getSubjectOffering() != null && Objects.equals(item.getSubjectOffering().getId(), sectionId))
                .findFirst()
                .orElse(null);

        List<Attendance> attendanceItems = attendanceRepository.findByStudentIdWithDetails(student.getId()).stream()
                .filter(item -> item.getSubjectOffering() != null && Objects.equals(item.getSubjectOffering().getId(), sectionId))
                .sorted(Comparator.comparing(Attendance::getDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        long present = attendanceItems.stream().filter(item -> item.getStatus() == Attendance.AttendanceStatus.PRESENT).count();
        long late = attendanceItems.stream().filter(item -> item.getStatus() == Attendance.AttendanceStatus.LATE).count();
        long absent = attendanceItems.stream().filter(item -> item.getStatus() == Attendance.AttendanceStatus.ABSENT).count();

        boolean activeCourseAccess = registration.getStatus() != Registration.RegistrationStatus.DROPPED;

        List<StudentAttendanceActiveSessionDto> activeSessions = activeCourseAccess
                ? attendanceFlowService.getActiveSessionsForStudent(student).stream()
                .filter(session -> Objects.equals(session.sectionId(), sectionId))
                .map(this::toStudentActiveAttendanceDto)
                .toList()
                : List.of();

        List<StudentSectionAnnouncementDto> announcements = activeCourseAccess
                ? courseAnnouncementRepository.findPublishedBySubjectOfferingIdWithDetailsOrderByPinnedDescPublishedAtDesc(sectionId).stream()
                .map(announcement -> new StudentSectionAnnouncementDto(
                        announcement.getId(),
                        announcement.getTitle(),
                        announcement.getContent(),
                        announcement.getTeacher() != null ? announcement.getTeacher().getName() : null,
                        announcement.getPublishedAt(),
                        announcement.isPinned()
                ))
                .toList()
                : List.of();

        List<StudentSectionMaterialDto> materials = activeCourseAccess
                ? courseMaterialService.listPublishedForSection(sectionId).stream()
                .map(material -> new StudentSectionMaterialDto(
                        material.getId(),
                        material.getTitle(),
                        material.getDescription(),
                        material.getOriginalFileName(),
                        material.getContentType(),
                        material.getSizeBytes(),
                        material.getCreatedAt(),
                        fileLinkService.createMaterialDownloadUrl(material.getId())
                ))
                .toList()
                : List.of();

        ExamSchedule exam = examScheduleRepository.findBySubjectOfferingIdInWithDetails(List.of(sectionId)).stream()
                .findFirst()
                .orElse(null);

        long occupiedSeats = registrationRepository.countBySubjectOfferingIdAndStatusIn(
                sectionId,
                List.of(Registration.RegistrationStatus.CONFIRMED, Registration.RegistrationStatus.SUBMITTED)
        );

        Semester semester = offering.getSemester();
        return ResponseEntity.ok(new StudentSectionDetailDto(
                registration.getId(),
                offering.getId(),
                offering.getSubject().getId(),
                offering.getSubject().getCode(),
                offering.getSubject().getName(),
                offering.getSubject().getCredits(),
                registration.getStatus(),
                activeCourseAccess,
                activeCourseAccess ? null : "Course content is available only for active registrations.",
                semester != null ? semester.getId() : null,
                semester != null ? semester.getName() : null,
                semester != null ? extractAcademicYear(semester.getName()) : null,
                semester != null ? extractSeason(semester.getName()) : null,
                offering.getTeacher() != null
                        ? new StudentSectionTeacherDto(
                        offering.getTeacher().getId(),
                        offering.getTeacher().getName(),
                        offering.getTeacher().getEmail())
                        : null,
                offering.getCapacity(),
                occupiedSeats,
                offering.getLessonType(),
                buildSectionMeetingSlots(offering),
                buildScoreSummary(publishedGrades, finalGrade),
                componentGrades,
                finalGrade != null ? new StudentSectionFinalGradeDto(
                        finalGrade.getId(),
                        finalGrade.getNumericValue(),
                        finalGrade.getLetterValue(),
                        finalGrade.getPoints(),
                        finalGrade.getStatus(),
                        finalGrade.getPublishedAt(),
                        finalGrade.getUpdatedAt()
                ) : null,
                new StudentSectionAttendanceSummaryDto(
                        present,
                        late,
                        absent,
                        attendanceItems.size(),
                        attendanceItems.isEmpty() ? 0.0 : ((present + late) * 100.0 / attendanceItems.size())
                ),
                attendanceItems.stream().map(item -> new StudentSectionAttendanceRecordDto(
                        item.getId(),
                        item.getDate(),
                        item.getStatus(),
                        item.getReason(),
                        item.getMarkedBy(),
                        item.isTeacherConfirmed(),
                        item.getMarkedAt(),
                        item.getUpdatedAt(),
                        item.getSession() != null ? item.getSession().getId() : null,
                        item.getSession() != null ? item.getSession().getClassDate() : null
                )).toList(),
                activeSessions,
                exam != null ? new StudentSectionExamDto(
                        exam.getId(),
                        exam.getExamDate(),
                        exam.getExamTime(),
                        exam.getRoom(),
                        exam.getFormat()
                ) : null,
                announcements,
                materials
        ));
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

    private StudentSectionScoreSummaryDto buildScoreSummary(List<Grade> grades, FinalGrade finalGrade) {
        Double attestation1 = null;
        Double attestation1Max = null;
        Double attestation2 = null;
        Double attestation2Max = null;
        Double finalExam = null;
        Double finalExamMax = null;

        for (Grade grade : grades) {
            String componentName = grade.getComponent() != null ? grade.getComponent().getName() : grade.getType().name();
            if (isAttestationOne(componentName, grade)) {
                attestation1 = grade.getGradeValue();
                attestation1Max = grade.getMaxGradeValue();
            } else if (isAttestationTwo(componentName, grade)) {
                attestation2 = grade.getGradeValue();
                attestation2Max = grade.getMaxGradeValue();
            } else if (isFinalComponent(componentName, grade)) {
                finalExam = grade.getGradeValue();
                finalExamMax = grade.getMaxGradeValue();
            }
        }

        return new StudentSectionScoreSummaryDto(
                attestation1,
                attestation1Max,
                attestation2,
                attestation2Max,
                finalExam,
                finalExamMax,
                finalGrade != null ? finalGrade.getNumericValue() : null,
                finalGrade != null ? finalGrade.getLetterValue() : null,
                finalGrade != null ? finalGrade.getPoints() : null
        );
    }

    private StudentSectionComponentGradeDto toComponentGradeDto(AssessmentComponent component, Grade grade) {
        return new StudentSectionComponentGradeDto(
                component != null ? component.getId() : null,
                component != null ? component.getName() : null,
                component != null ? component.getType() : null,
                component != null ? component.getWeightPercent() : null,
                component != null && component.isPublished(),
                component != null && component.isLocked(),
                grade != null ? grade.getId() : null,
                grade != null ? grade.getType() : null,
                grade != null ? grade.getGradeValue() : null,
                grade != null ? grade.getMaxGradeValue() : null,
                grade != null ? grade.getComment() : null,
                grade != null ? grade.getCreatedAt() : null
        );
    }

    private List<StudentSectionMeetingSlotDto> buildSectionMeetingSlots(SubjectOffering offering) {
        List<MeetingTime> meetingTimes = offering.getMeetingTimes() == null
                ? List.of()
                : offering.getMeetingTimes().stream()
                .sorted(Comparator.comparing(MeetingTime::getDayOfWeek).thenComparing(MeetingTime::getStartTime))
                .toList();

        if (!meetingTimes.isEmpty()) {
            return meetingTimes.stream()
                    .map(meetingTime -> new StudentSectionMeetingSlotDto(
                            meetingTime.getDayOfWeek(),
                            meetingTime.getStartTime(),
                            meetingTime.getEndTime(),
                            meetingTime.getRoom(),
                            meetingTime.getLessonType() != null ? meetingTime.getLessonType() : offering.getLessonType()
                    ))
                    .toList();
        }

        return List.of(new StudentSectionMeetingSlotDto(
                offering.getDayOfWeek(),
                offering.getStartTime(),
                offering.getEndTime(),
                offering.getRoom(),
                offering.getLessonType()
        ));
    }

    private List<ScheduleItemDto> toScheduleItemDtos(Registration registration) {
        SubjectOffering offering = registration.getSubjectOffering();
        if (offering == null) {
            return List.of();
        }
        Semester semester = offering != null ? offering.getSemester() : null;
        List<MeetingTime> meetingTimes = offering.getMeetingTimes() == null
                ? List.of()
                : offering.getMeetingTimes().stream()
                .sorted(Comparator.comparing(MeetingTime::getDayOfWeek).thenComparing(MeetingTime::getStartTime))
                .toList();

        if (!meetingTimes.isEmpty()) {
            return meetingTimes.stream()
                    .map(meetingTime -> new ScheduleItemDto(
                            offering.getId(),
                            offering.getSubject() != null ? offering.getSubject().getCode() : null,
                            offering.getSubject() != null ? offering.getSubject().getName() : null,
                            meetingTime.getDayOfWeek(),
                            meetingTime.getStartTime(),
                            meetingTime.getEndTime(),
                            meetingTime.getRoom(),
                            offering.getTeacher() != null ? offering.getTeacher().getName() : null,
                            registration.getStatus(),
                            semester != null ? semester.getId() : null,
                            semester != null ? semester.getName() : null,
                            semester != null ? extractAcademicYear(semester.getName()) : null,
                            semester != null ? extractSeason(semester.getName()) : null,
                            meetingTime.getLessonType() != null ? meetingTime.getLessonType() : offering.getLessonType()
                    ))
                    .toList();
        }

        return List.of(new ScheduleItemDto(
                offering.getId(),
                offering.getSubject() != null ? offering.getSubject().getCode() : null,
                offering.getSubject() != null ? offering.getSubject().getName() : null,
                offering.getDayOfWeek(),
                offering.getStartTime(),
                offering.getEndTime(),
                offering.getRoom(),
                offering.getTeacher() != null ? offering.getTeacher().getName() : null,
                registration.getStatus(),
                semester != null ? semester.getId() : null,
                semester != null ? semester.getName() : null,
                semester != null ? extractAcademicYear(semester.getName()) : null,
                semester != null ? extractSeason(semester.getName()) : null,
                offering.getLessonType()
        ));
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
    public record StudentSectionDetailDto(
            Long registrationId,
            Long sectionId,
            Long subjectId,
            String subjectCode,
            String subjectName,
            Integer credits,
            Registration.RegistrationStatus registrationStatus,
            boolean activeCourseAccess,
            String contentBlockedReason,
            Long semesterId,
            String semesterName,
            String academicYear,
            String season,
            StudentSectionTeacherDto teacher,
            int capacity,
            long occupiedSeats,
            SubjectOffering.LessonType lessonType,
            List<StudentSectionMeetingSlotDto> meetingTimes,
            StudentSectionScoreSummaryDto scoreSummary,
            List<StudentSectionComponentGradeDto> componentGrades,
            StudentSectionFinalGradeDto finalGrade,
            StudentSectionAttendanceSummaryDto attendanceSummary,
            List<StudentSectionAttendanceRecordDto> attendanceRecords,
            List<StudentAttendanceActiveSessionDto> activeAttendanceSessions,
            StudentSectionExamDto exam,
            List<StudentSectionAnnouncementDto> announcements,
            List<StudentSectionMaterialDto> materials
    ) {}
    public record StudentSectionTeacherDto(Long teacherId, String teacherName, String teacherEmail) {}
    public record StudentSectionMeetingSlotDto(
            java.time.DayOfWeek dayOfWeek,
            java.time.LocalTime startTime,
            java.time.LocalTime endTime,
            String room,
            SubjectOffering.LessonType lessonType
    ) {}
    public record StudentSectionScoreSummaryDto(
            Double attestation1,
            Double attestation1Max,
            Double attestation2,
            Double attestation2Max,
            Double finalExam,
            Double finalExamMax,
            Double totalScore,
            String letterValue,
            Double points
    ) {}
    public record StudentSectionComponentGradeDto(
            Long componentId,
            String componentName,
            AssessmentComponent.ComponentType componentType,
            Double weightPercent,
            boolean componentPublished,
            boolean componentLocked,
            Long gradeId,
            Grade.GradeType gradeType,
            Double gradeValue,
            Double maxGradeValue,
            String comment,
            Instant createdAt
    ) {}
    public record StudentSectionFinalGradeDto(
            Long id,
            double numericValue,
            String letterValue,
            double points,
            FinalGrade.FinalGradeStatus status,
            Instant publishedAt,
            Instant updatedAt
    ) {}
    public record StudentSectionAttendanceSummaryDto(
            long present,
            long late,
            long absent,
            int total,
            double percentage
    ) {}
    public record StudentSectionAttendanceRecordDto(
            Long attendanceId,
            java.time.LocalDate date,
            Attendance.AttendanceStatus status,
            String reason,
            Attendance.MarkedBy markedBy,
            boolean teacherConfirmed,
            Instant markedAt,
            Instant updatedAt,
            Long sessionId,
            java.time.LocalDate sessionDate
    ) {}
    public record StudentSectionExamDto(Long id, java.time.LocalDate examDate, java.time.LocalTime examTime, String room, String format) {}
    public record StudentSectionAnnouncementDto(
            Long id,
            String title,
            String content,
            String teacherName,
            Instant publishedAt,
            boolean pinned
    ) {}
    public record StudentSectionMaterialDto(
            Long id,
            String title,
            String description,
            String originalFileName,
            String contentType,
            long sizeBytes,
            Instant createdAt,
            String downloadUrl
    ) {}
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
