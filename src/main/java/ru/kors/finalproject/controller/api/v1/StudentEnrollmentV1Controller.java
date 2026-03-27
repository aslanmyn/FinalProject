package ru.kors.finalproject.controller.api.v1;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import ru.kors.finalproject.entity.*;
import ru.kors.finalproject.repository.*;
import ru.kors.finalproject.service.*;
import ru.kors.finalproject.web.api.v1.CurrentUserHelper;

import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/v1/student")
@RequiredArgsConstructor
public class StudentEnrollmentV1Controller {

    private final CurrentUserHelper currentUserHelper;
    private final RegistrationRepository registrationRepository;
    private final HoldRepository holdRepository;
    private final RegistrationWindowRepository registrationWindowRepository;
    private final AddDropService addDropService;
    private final FxRegistrationService fxRegistrationService;
    private final WindowPolicyService windowPolicyService;
    private final FinancialService financialService;

    @GetMapping("/enrollments")
    public ResponseEntity<?> enrollments(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) Long semesterId) {
        Student student = currentUserHelper.requireStudent(user);
        return ResponseEntity.ok(registrationRepository.findByStudentIdWithDetails(student.getId()).stream()
                .filter(registration -> registration.getSubjectOffering() != null
                        && registration.getSubjectOffering().getSubject() != null)
                .filter(registration -> semesterId == null
                        || (registration.getSubjectOffering().getSemester() != null
                        && Objects.equals(registration.getSubjectOffering().getSemester().getId(), semesterId)))
                .sorted(Comparator
                        .comparing((Registration registration) -> {
                            Semester semester = registration.getSubjectOffering().getSemester();
                            return semester != null ? semester.getStartDate() : null;
                        }, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(registration -> registration.getSubjectOffering().getSubject().getCode(),
                                Comparator.nullsLast(String::compareTo))
                        .thenComparing(Registration::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toEnrollmentDto)
                .toList());
    }

    @GetMapping("/enrollments/options")
    public ResponseEntity<?> enrollmentOptions(@AuthenticationPrincipal User user) {
        Student student = currentUserHelper.requireStudent(user);
        List<SemesterOptionDto> semesters = buildEnrollmentSemesterOptions(student.getId());
        Long currentSemesterId = student.getCurrentSemester() != null
                ? student.getCurrentSemester().getId()
                : semesters.stream().findFirst().map(SemesterOptionDto::id).orElse(null);
        return ResponseEntity.ok(new EnrollmentOptionsDto(currentSemesterId, semesters));
    }

    @GetMapping("/course-registration/overview")
    @Transactional(readOnly = true)
    public ResponseEntity<?> registrationOverview(@AuthenticationPrincipal User user) {
        Student student = currentUserHelper.requireStudent(user);
        Semester semester = student.getCurrentSemester();
        List<Registration> currentRegistrations = registrationRepository.findActiveByStudentIdWithDetails(student.getId()).stream()
                .filter(registration -> registration.getSubjectOffering() != null
                        && registration.getSubjectOffering().getSemester() != null
                        && semester != null
                        && registration.getSubjectOffering().getSemester().getId().equals(semester.getId()))
                .toList();
        int currentCredits = currentRegistrations.stream()
                .mapToInt(registration -> registration.getSubjectOffering().getSubject().getCredits())
                .sum();
        List<Hold> activeHolds = holdRepository.findByStudentIdAndActiveTrue(student.getId());
        List<WindowStatusDto> windows = semester == null
                ? List.of()
                : registrationWindowRepository.findBySemesterIdOrderByStartDateAsc(semester.getId()).stream()
                .map(window -> new WindowStatusDto(
                        window.getId(),
                        window.getType(),
                        window.getStartDate(),
                        window.getEndDate(),
                        window.isActive(),
                        windowPolicyService.isWindowActive(semester.getId(), window.getType())
                ))
                .toList();
        List<RegistrationBoardItemDto> registrations = currentRegistrations.stream()
                .map(registration -> {
                    AddDropService.DropCheck dropCheck = addDropService.evaluateDrop(student, registration.getSubjectOffering().getId());
                    return new RegistrationBoardItemDto(
                            registration.getId(),
                            registration.getSubjectOffering().getId(),
                            registration.getSubjectOffering().getSubject().getCode(),
                            registration.getSubjectOffering().getSubject().getName(),
                            registration.getSubjectOffering().getTeacher() != null ? registration.getSubjectOffering().getTeacher().getName() : null,
                            registration.getSubjectOffering().getSubject().getCredits(),
                            registration.getStatus(),
                            dropCheck.allowed(),
                            dropCheck.reasons(),
                            buildMeetingSlots(registration.getSubjectOffering())
                    );
                })
                .toList();

        return ResponseEntity.ok(new RegistrationOverviewDto(
                semester != null ? semester.getId() : null,
                semester != null ? semester.getName() : null,
                currentCredits,
                student.getProgram() != null ? student.getProgram().getCreditLimit() : null,
                addDropService.hasActiveRegistrationHold(student),
                activeHolds.stream().map(this::toHoldDto).toList(),
                windows,
                registrations,
                fxRegistrationService.listEligible(student).size(),
                fxRegistrationService.listForStudent(student).size()
        ));
    }

    @GetMapping("/course-registration/catalog")
    @Transactional(readOnly = true)
    public ResponseEntity<?> registrationCatalog(@AuthenticationPrincipal User user) {
        Student student = currentUserHelper.requireStudent(user);
        return ResponseEntity.ok(addDropService.getCatalogForCurrentSemester(student).stream()
                .map(offering -> toCourseCatalogDto(student, offering))
                .toList());
    }

    @GetMapping("/course-registration/available")
    public ResponseEntity<?> availableCourses(@AuthenticationPrincipal User user) {
        Student student = currentUserHelper.requireStudent(user);
        return ResponseEntity.ok(addDropService.getAvailableForAdd(student).stream()
                .map(this::toAvailableCourseDto)
                .toList());
    }

    @PostMapping("/course-registration/submit")
    public ResponseEntity<?> submitRegistration(
            @AuthenticationPrincipal User user,
            @RequestBody CourseActionBody body) {
        Student student = currentUserHelper.requireStudent(user);
        return ResponseEntity.ok(addDropService.registerCourse(student, body.sectionId()));
    }

    @PostMapping("/add-drop/add")
    public ResponseEntity<?> addCourse(
            @AuthenticationPrincipal User user,
            @RequestBody CourseActionBody body) {
        Student student = currentUserHelper.requireStudent(user);
        return ResponseEntity.ok(addDropService.addCourse(student, body.sectionId()));
    }

    @PostMapping("/add-drop/drop")
    public ResponseEntity<?> dropCourse(
            @AuthenticationPrincipal User user,
            @RequestBody CourseActionBody body) {
        Student student = currentUserHelper.requireStudent(user);
        return ResponseEntity.ok(addDropService.dropCourse(student, body.sectionId()));
    }

    @GetMapping("/fx")
    public ResponseEntity<?> fx(@AuthenticationPrincipal User user) {
        Student student = currentUserHelper.requireStudent(user);
        Semester semester = student.getCurrentSemester();
        return ResponseEntity.ok(new FxOverviewDto(
                semester != null && windowPolicyService.isWindowActive(semester.getId(), RegistrationWindow.WindowType.FX),
                fxRegistrationService.listEligible(student).stream()
                        .map(eligible -> new FxEligibleCourseDto(
                                eligible.sectionId(),
                                eligible.subjectCode(),
                                eligible.subjectName(),
                                eligible.finalScore(),
                                eligible.alreadyRequested()
                        ))
                        .toList(),
                fxRegistrationService.listForStudent(student).stream()
                        .map(this::toFxDto)
                        .toList()
        ));
    }

    @PostMapping("/fx")
    public ResponseEntity<?> createFx(
            @AuthenticationPrincipal User user,
            @RequestBody CourseActionBody body) {
        Student student = currentUserHelper.requireStudent(user);
        return ResponseEntity.ok(toFxDto(fxRegistrationService.submit(student, body.sectionId())));
    }
    @GetMapping("/financial")
    public ResponseEntity<?> financial(@AuthenticationPrincipal User user) {
        Student student = currentUserHelper.requireStudent(user);
        List<Charge> charges = financialService.getCharges(student);
        List<Payment> payments = financialService.getPayments(student);
        return ResponseEntity.ok(new StudentFinancialDto(
                charges.stream().map(charge -> new StudentChargeDto(
                        charge.getId(),
                        charge.getAmount(),
                        charge.getDescription(),
                        charge.getDueDate(),
                        charge.getStatus()
                )).toList(),
                payments.stream().map(payment -> new StudentPaymentDto(
                        payment.getId(),
                        payment.getAmount(),
                        payment.getDate()
                )).toList(),
                financialService.getBalance(student),
                financialService.hasRegistrationLock(student)
        ));
    }

    @GetMapping("/holds")
    public ResponseEntity<?> holds(@AuthenticationPrincipal User user) {
        Student student = currentUserHelper.requireStudent(user);
        List<Hold> activeHolds = holdRepository.findByStudentIdAndActiveTrue(student.getId());
        return ResponseEntity.ok(activeHolds.stream().map(h -> Map.of(
                "id", (Object) h.getId(), "type", h.getType(),
                "reason", h.getReason(), "createdAt", h.getCreatedAt().toString()
        )).toList());
    }

    private List<SemesterOptionDto> buildEnrollmentSemesterOptions(Long studentId) {
        return registrationRepository.findByStudentIdWithDetails(studentId).stream()
                .map(registration -> registration.getSubjectOffering() != null ? registration.getSubjectOffering().getSemester() : null)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toMap(
                        Semester::getId,
                        semester -> semester,
                        (left, right) -> left,
                        LinkedHashMap::new
                ))
                .values().stream()
                .sorted(Comparator.comparing(Semester::getStartDate, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .map(this::toSemesterOptionDto)
                .toList();
    }

    private StudentEnrollmentDto toEnrollmentDto(Registration registration) {
        SubjectOffering offering = registration.getSubjectOffering();
        Semester semester = offering != null ? offering.getSemester() : null;
        return new StudentEnrollmentDto(
                registration.getId(),
                offering != null ? offering.getId() : null,
                offering != null && offering.getSubject() != null ? offering.getSubject().getCode() : null,
                offering != null && offering.getSubject() != null ? offering.getSubject().getName() : null,
                offering != null && offering.getTeacher() != null ? offering.getTeacher().getName() : null,
                offering != null && offering.getSubject() != null ? offering.getSubject().getCredits() : null,
                semester != null ? semester.getId() : null,
                semester != null ? semester.getName() : null,
                semester != null ? extractAcademicYear(semester.getName()) : null,
                semester != null ? extractSeason(semester.getName()) : null,
                registration.getStatus(),
                registration.getCreatedAt()
        );
    }

    private CourseCatalogItemDto toCourseCatalogDto(Student student, SubjectOffering offering) {
        AddDropService.EnrollmentCheck registrationCheck = addDropService.evaluateEnrollment(
                student, offering, RegistrationWindow.WindowType.REGISTRATION, false);
        AddDropService.EnrollmentCheck addDropCheck = addDropService.evaluateEnrollment(
                student, offering, RegistrationWindow.WindowType.ADD_DROP, false);
        Registration currentRegistration = registrationRepository.findByStudentIdAndSubjectOfferingIdWithDetails(student.getId(), offering.getId())
                .filter(registration -> registration.getStatus() != Registration.RegistrationStatus.DROPPED)
                .orElse(null);
        AddDropService.DropCheck dropCheck = currentRegistration != null
                ? addDropService.evaluateDrop(student, offering.getId())
                : new AddDropService.DropCheck(false, List.of(), false);
        Semester semester = offering.getSemester();
        return new CourseCatalogItemDto(
                offering.getId(),
                offering.getSubject().getCode(),
                offering.getSubject().getName(),
                offering.getSubject().getCredits(),
                semester != null ? semester.getId() : null,
                semester != null ? semester.getName() : null,
                semester != null ? extractAcademicYear(semester.getName()) : null,
                semester != null ? extractSeason(semester.getName()) : null,
                offering.getTeacher() != null ? offering.getTeacher().getId() : null,
                offering.getTeacher() != null ? offering.getTeacher().getName() : null,
                offering.getCapacity(),
                registrationCheck.occupiedSeats(),
                offering.getLessonType(),
                buildMeetingSlots(offering),
                currentRegistration != null ? currentRegistration.getStatus() : null,
                registrationCheck.allowed(),
                registrationCheck.reasons(),
                addDropCheck.allowed(),
                addDropCheck.reasons(),
                dropCheck.allowed(),
                dropCheck.reasons()
        );
    }

    private AvailableCourseDto toAvailableCourseDto(SubjectOffering offering) {
        return new AvailableCourseDto(
                offering.getId(),
                offering.getSubject() != null ? offering.getSubject().getCode() : null,
                offering.getSubject() != null ? offering.getSubject().getName() : null,
                offering.getSubject() != null ? offering.getSubject().getCredits() : 0,
                offering.getSemester() != null ? offering.getSemester().getId() : null,
                offering.getSemester() != null ? offering.getSemester().getName() : null,
                offering.getTeacher() != null ? offering.getTeacher().getId() : null,
                offering.getTeacher() != null ? offering.getTeacher().getName() : null,
                offering.getCapacity(),
                offering.getLessonType(),
                offering.getDayOfWeek(),
                offering.getStartTime(),
                offering.getEndTime(),
                offering.getRoom()
        );
    }

    private List<MeetingSlotDto> buildMeetingSlots(SubjectOffering offering) {
        if (offering == null) {
            return List.of();
        }
        if (offering.getMeetingTimes() != null && !offering.getMeetingTimes().isEmpty()) {
            return offering.getMeetingTimes().stream()
                    .sorted(Comparator.comparing(MeetingTime::getDayOfWeek).thenComparing(MeetingTime::getStartTime))
                    .map(slot -> new MeetingSlotDto(
                            slot.getDayOfWeek(),
                            slot.getStartTime(),
                            slot.getEndTime(),
                            slot.getRoom(),
                            slot.getLessonType()
                    ))
                    .toList();
        }
        if (offering.getDayOfWeek() == null || offering.getStartTime() == null || offering.getEndTime() == null) {
            return List.of();
        }
        return List.of(new MeetingSlotDto(
                offering.getDayOfWeek(),
                offering.getStartTime(),
                offering.getEndTime(),
                offering.getRoom(),
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

    private HoldDto toHoldDto(Hold hold) {
        return new HoldDto(hold.getId(), hold.getType(), hold.getReason(), hold.getCreatedAt());
    }

    private FxRegistrationDto toFxDto(FxRegistration fx) {
        SubjectOffering offering = fx.getSubjectOffering();
        return new FxRegistrationDto(
                fx.getId(),
                offering != null ? offering.getId() : null,
                offering != null && offering.getSubject() != null ? offering.getSubject().getCode() : null,
                offering != null && offering.getSubject() != null ? offering.getSubject().getName() : null,
                fx.getStatus(),
                fx.getCreatedAt()
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

    public record StudentEnrollmentDto(Long id, Long sectionId, String subjectCode, String subjectName,
                                       String teacherName, Integer credits, Long semesterId,
                                       String semesterName, String academicYear, String season,
                                       Registration.RegistrationStatus status, Instant createdAt) {}
    public record EnrollmentOptionsDto(Long currentSemesterId, List<SemesterOptionDto> semesters) {}
    public record SemesterOptionDto(Long id, String name, String academicYear, String season, boolean current) {}
    public record HoldDto(Long id, Hold.HoldType type, String reason, Instant createdAt) {}
    public record MeetingSlotDto(java.time.DayOfWeek dayOfWeek, java.time.LocalTime startTime,
                                 java.time.LocalTime endTime, String room, SubjectOffering.LessonType lessonType) {}
    public record RegistrationBoardItemDto(Long registrationId, Long sectionId, String subjectCode, String subjectName,
                                           String teacherName, int credits, Registration.RegistrationStatus status,
                                           boolean canDrop, List<String> dropBlockedReasons,
                                           List<MeetingSlotDto> meetingTimes) {}
    public record WindowStatusDto(Long id, RegistrationWindow.WindowType type, java.time.LocalDate startDate,
                                  java.time.LocalDate endDate, boolean active, boolean openNow) {}
    public record RegistrationOverviewDto(Long currentSemesterId, String currentSemesterName, int currentCredits,
                                          Integer creditLimit, boolean hasRegistrationHold, List<HoldDto> holds,
                                          List<WindowStatusDto> windows, List<RegistrationBoardItemDto> currentRegistrations,
                                          int eligibleFxCount, int fxRequestCount) {}
    public record CourseCatalogItemDto(Long sectionId, String subjectCode, String subjectName, int credits,
                                       Long semesterId, String semesterName, String academicYear, String season,
                                       Long teacherId, String teacherName, int capacity, long occupiedSeats,
                                       SubjectOffering.LessonType lessonType, List<MeetingSlotDto> meetingTimes,
                                       Registration.RegistrationStatus registrationStatus, boolean canRegister,
                                       List<String> registrationBlockedReasons, boolean canAddDrop,
                                       List<String> addDropBlockedReasons, boolean canDrop,
                                       List<String> dropBlockedReasons) {}
    public record AvailableCourseDto(Long sectionId, String subjectCode, String subjectName, int credits,
                                     Long semesterId, String semesterName, Long teacherId, String teacherName,
                                     int capacity, SubjectOffering.LessonType lessonType,
                                     java.time.DayOfWeek dayOfWeek, java.time.LocalTime startTime,
                                     java.time.LocalTime endTime, String room) {}
    public record FxEligibleCourseDto(Long sectionId, String subjectCode, String subjectName,
                                      double finalScore, boolean alreadyRequested) {}
    public record FxRegistrationDto(Long id, Long sectionId, String subjectCode, String subjectName,
                                    FxRegistration.FxStatus status, Instant createdAt) {}
    public record FxOverviewDto(boolean windowOpen, List<FxEligibleCourseDto> eligibleCourses,
                                List<FxRegistrationDto> registrations) {}
    public record StudentChargeDto(Long id, java.math.BigDecimal amount, String description,
                                   java.time.LocalDate dueDate, Charge.ChargeStatus status) {}
    public record StudentPaymentDto(Long id, java.math.BigDecimal amount, java.time.LocalDate date) {}
    public record StudentFinancialDto(List<StudentChargeDto> charges, List<StudentPaymentDto> payments,
                                      java.math.BigDecimal balance, boolean hasFinancialHold) {}
    public record CourseActionBody(Long sectionId) {}
}
