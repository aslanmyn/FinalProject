package ru.kors.finalproject.controller.api.v1;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;
import ru.kors.finalproject.entity.*;
import ru.kors.finalproject.repository.*;
import ru.kors.finalproject.service.*;
import ru.kors.finalproject.web.api.v1.ApiPageResponse;
import ru.kors.finalproject.web.api.v1.ApiPageableFactory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/student")
@RequiredArgsConstructor
public class StudentV1Controller {
    private final MobileApiAuthService mobileApiAuthService;
    private final StudentRepository studentRepository;
    private final RegistrationRepository registrationRepository;
    private final GradeRepository gradeRepository;
    private final FinalGradeRepository finalGradeRepository;
    private final AttendanceRepository attendanceRepository;
    private final ChargeRepository chargeRepository;
    private final PaymentRepository paymentRepository;
    private final StudentRequestRepository studentRequestRepository;
    private final HoldRepository holdRepository;
    private final NewsRepository newsRepository;
    private final ChecklistItemRepository checklistItemRepository;
    private final MobilityApplicationRepository mobilityApplicationRepository;
    private final ClearanceSheetRepository clearanceSheetRepository;
    private final ExamScheduleRepository examScheduleRepository;
    private final SemesterRepository semesterRepository;
    private final AnnouncementService announcementService;
    private final RequestService requestService;
    private final NotificationService notificationService;
    private final CourseMaterialService courseMaterialService;
    private final AddDropService addDropService;
    private final ApiPageableFactory apiPageableFactory;
    private final FileLinkService fileLinkService;
    private final FileAssetRepository fileAssetRepository;
    private final FileStorageService fileStorageService;

    @GetMapping("/profile")
    public ResponseEntity<?> profile(@RequestHeader("Authorization") String authHeader) {
        User user = mobileApiAuthService.requireRole(authHeader, User.UserRole.STUDENT);
        Student student = getStudent(user);
        return ResponseEntity.ok(toStudentProfileDto(student));
    }

    @PostMapping(value = "/profile-photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadProfilePhoto(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam("file") MultipartFile file) {
        User user = mobileApiAuthService.requireRole(authHeader, User.UserRole.STUDENT);
        Student student = getStudent(user);
        if (file.getContentType() == null || !file.getContentType().toLowerCase().startsWith("image/")) {
            throw new IllegalArgumentException("Only image files are allowed for profile photo");
        }

        FileStorageService.StoredFile stored = fileStorageService.store(file, "profile-photos/student-" + student.getId());
        FileAsset previousAsset = student.getProfilePhotoAssetId() != null
                ? fileAssetRepository.findById(student.getProfilePhotoAssetId()).orElse(null)
                : null;

        FileAsset savedAsset = fileAssetRepository.save(FileAsset.builder()
                .originalName(stored.originalName())
                .storagePath(stored.storagePath())
                .contentType(stored.contentType())
                .sizeBytes(stored.sizeBytes())
                .category(FileAsset.FileCategory.OTHER)
                .linkedEntityType("STUDENT_PROFILE_PHOTO")
                .linkedEntityId(student.getId())
                .ownerStudent(student)
                .uploadedBy(user)
                .uploadedAt(Instant.now())
                .build());

        student.setProfilePhotoAssetId(savedAsset.getId());
        studentRepository.save(student);

        if (previousAsset != null) {
            fileStorageService.deleteSilently(previousAsset.getStoragePath());
            fileAssetRepository.delete(previousAsset);
        }

        return ResponseEntity.ok(toStudentProfileDto(student));
    }

    @GetMapping("/schedule")
    public ResponseEntity<?> schedule(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(required = false) Long semesterId) {
        User user = mobileApiAuthService.requireRole(authHeader, User.UserRole.STUDENT);
        Student student = getStudent(user);
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
    public ResponseEntity<?> scheduleOptions(@RequestHeader("Authorization") String authHeader) {
        User user = mobileApiAuthService.requireRole(authHeader, User.UserRole.STUDENT);
        Student student = getStudent(user);
        List<SemesterOptionDto> semesters = registrationRepository.findByStudentIdWithDetails(student.getId()).stream()
                .filter(r -> r.getStatus() != Registration.RegistrationStatus.DROPPED)
                .map(r -> r.getSubjectOffering() != null ? r.getSubjectOffering().getSemester() : null)
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

        Long currentSemesterId = student.getCurrentSemester() != null
                ? student.getCurrentSemester().getId()
                : semesterRepository.findByCurrentTrue().map(Semester::getId).orElse(null);

        return ResponseEntity.ok(new ScheduleOptionsDto(currentSemesterId, semesters));
    }

    @GetMapping("/journal")
    public ResponseEntity<?> journal(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(required = false) Long semesterId) {
        User user = mobileApiAuthService.requireRole(authHeader, User.UserRole.STUDENT);
        Student student = getStudent(user);
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
    public ResponseEntity<?> journalOptions(@RequestHeader("Authorization") String authHeader) {
        User user = mobileApiAuthService.requireRole(authHeader, User.UserRole.STUDENT);
        Student student = getStudent(user);
        List<SemesterOptionDto> semesters = buildJournalSemesterOptions(student.getId());
        Long currentSemesterId = student.getCurrentSemester() != null
                ? student.getCurrentSemester().getId()
                : semesters.stream().findFirst().map(SemesterOptionDto::id).orElse(null);
        return ResponseEntity.ok(new JournalOptionsDto(currentSemesterId, semesters));
    }

    @GetMapping("/transcript")
    public ResponseEntity<?> transcript(@RequestHeader("Authorization") String authHeader) {
        User user = mobileApiAuthService.requireRole(authHeader, User.UserRole.STUDENT);
        Student student = getStudent(user);
        List<FinalGrade> grades = finalGradeRepository.findByStudentIdAndPublishedTrueWithDetails(student.getId());
        double gpa = grades.isEmpty() ? 0 : grades.stream().mapToDouble(FinalGrade::getPoints).average().orElse(0.0);
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
    public ResponseEntity<?> attendance(@RequestHeader("Authorization") String authHeader) {
        User user = mobileApiAuthService.requireRole(authHeader, User.UserRole.STUDENT);
        Student student = getStudent(user);
        List<Attendance> items = attendanceRepository.findByStudentIdWithDetails(student.getId());
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
                "summary", Map.of("present", present, "late", late, "absent", absent,
                        "total", items.size(),
                        "percentage", items.isEmpty() ? 0.0 : ((present + late) * 100.0 / items.size()))
        ));
    }

    @GetMapping("/financial")
    public ResponseEntity<?> financial(@RequestHeader("Authorization") String authHeader) {
        User user = mobileApiAuthService.requireRole(authHeader, User.UserRole.STUDENT);
        Student student = getStudent(user);
        List<Charge> charges = chargeRepository.findByStudentIdOrderByDueDateDesc(student.getId());
        List<Payment> payments = paymentRepository.findByStudentIdOrderByDateDesc(student.getId());
        BigDecimal totalCharges = charges.stream().map(Charge::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPayments = payments.stream().map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        boolean hasFinancialHold = holdRepository.existsByStudentIdAndTypeAndActiveTrue(student.getId(), Hold.HoldType.FINANCIAL);
        return ResponseEntity.ok(Map.of(
                "charges", charges.stream().map(c -> Map.of(
                        "id", (Object) c.getId(), "amount", c.getAmount(),
                        "description", c.getDescription(), "dueDate", c.getDueDate().toString(),
                        "status", c.getStatus()
                )).toList(),
                "payments", payments.stream().map(p -> Map.of(
                        "id", (Object) p.getId(), "amount", p.getAmount(),
                        "date", p.getDate().toString()
                )).toList(),
                "balance", totalCharges.subtract(totalPayments),
                "hasFinancialHold", hasFinancialHold
        ));
    }

    @GetMapping("/holds")
    public ResponseEntity<?> holds(@RequestHeader("Authorization") String authHeader) {
        User user = mobileApiAuthService.requireRole(authHeader, User.UserRole.STUDENT);
        Student student = getStudent(user);
        List<Hold> activeHolds = holdRepository.findByStudentIdAndActiveTrue(student.getId());
        return ResponseEntity.ok(activeHolds.stream().map(h -> Map.of(
                "id", (Object) h.getId(), "type", h.getType(),
                "reason", h.getReason(), "createdAt", h.getCreatedAt().toString()
        )).toList());
    }

    @GetMapping("/exam-schedule")
    public ResponseEntity<?> examSchedule(@RequestHeader("Authorization") String authHeader) {
        User user = mobileApiAuthService.requireRole(authHeader, User.UserRole.STUDENT);
        Student student = getStudent(user);
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

    @GetMapping("/news")
    public ResponseEntity<?> news(@RequestHeader("Authorization") String authHeader) {
        mobileApiAuthService.requireRole(authHeader, User.UserRole.STUDENT);
        return ResponseEntity.ok(newsRepository.findByOrderByCreatedAtDesc().stream().map(n -> Map.of(
                "id", (Object) n.getId(),
                "title", n.getTitle(),
                "content", n.getContent(),
                "category", n.getCategory() != null ? n.getCategory() : "",
                "createdAt", n.getCreatedAt().toString()
        )).toList());
    }

    @GetMapping("/announcements")
    public ResponseEntity<?> announcements(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "desc") String direction) {
        User user = mobileApiAuthService.requireRole(authHeader, User.UserRole.STUDENT);
        Student student = getStudent(user);
        var pageable = apiPageableFactory.create(
                page, size, sort, direction, "publishedAt",
                Set.of("publishedAt", "createdAt", "title"));
        var data = announcementService.listForStudent(student, pageable).map(a -> new AnnouncementDto(
                a.getId(), a.getTitle(), a.getContent(),
                a.getSubjectOffering() != null ? a.getSubjectOffering().getId() : null,
                a.getSubjectOffering() != null ? a.getSubjectOffering().getSubject().getCode() : null,
                a.getPublishedAt(), a.isPinned()));
        return ResponseEntity.ok(ApiPageResponse.from(data));
    }

    @GetMapping("/notifications")
    public ResponseEntity<?> notifications(@RequestHeader("Authorization") String authHeader) {
        User user = mobileApiAuthService.requireRole(authHeader, User.UserRole.STUDENT);
        return ResponseEntity.ok(Map.of(
                "notifications", notificationService.listForEmail(user.getEmail()),
                "unreadCount", notificationService.unreadCount(user.getEmail())
        ));
    }

    @PostMapping("/notifications/{id}/read")
    public ResponseEntity<?> markNotificationRead(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {
        mobileApiAuthService.requireRole(authHeader, User.UserRole.STUDENT);
        notificationService.markRead(id);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @GetMapping("/materials/{sectionId}")
    public ResponseEntity<?> courseMaterials(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long sectionId) {
        User user = mobileApiAuthService.requireRole(authHeader, User.UserRole.STUDENT);
        Student student = getStudent(user);
        if (!courseMaterialService.isStudentEnrolled(student.getId(), sectionId)) {
            throw new IllegalArgumentException("Not enrolled in this section");
        }
        return ResponseEntity.ok(courseMaterialService.listPublishedForSection(sectionId).stream()
                .map(m -> new MaterialDto(
                        m.getId(),
                        m.getTitle(),
                        m.getDescription(),
                        m.getOriginalFileName(),
                        m.getContentType(),
                        m.getSizeBytes(),
                        m.getCreatedAt(),
                        fileLinkService.createMaterialDownloadUrl(m.getId())
                )).toList());
    }

    @GetMapping("/files")
    public ResponseEntity<?> files(@RequestHeader("Authorization") String authHeader) {
        User user = mobileApiAuthService.requireRole(authHeader, User.UserRole.STUDENT);
        Student student = getStudent(user);
        return ResponseEntity.ok(fileAssetRepository.findByOwnerStudentIdOrderByUploadedAtDesc(student.getId()).stream()
                .map(f -> new StudentFileDto(
                        f.getId(),
                        f.getOriginalName(),
                        f.getCategory(),
                        f.getContentType(),
                        f.getSizeBytes(),
                        f.getUploadedAt(),
                        fileLinkService.createAssetDownloadUrl(f.getId())
                )).toList());
    }

    @GetMapping("/checklist")
    public ResponseEntity<?> checklist(@RequestHeader("Authorization") String authHeader) {
        User user = mobileApiAuthService.requireRole(authHeader, User.UserRole.STUDENT);
        Student student = getStudent(user);
        return ResponseEntity.ok(checklistItemRepository.findByStudentIdOrderByDeadlineAsc(student.getId()).stream()
                .map(this::toChecklistItemDto)
                .toList());
    }

    @GetMapping("/mobility")
    public ResponseEntity<?> mobility(@RequestHeader("Authorization") String authHeader) {
        User user = mobileApiAuthService.requireRole(authHeader, User.UserRole.STUDENT);
        Student student = getStudent(user);
        return ResponseEntity.ok(mobilityApplicationRepository.findByStudentIdWithDetailsOrderByCreatedAtDesc(student.getId()).stream()
                .map(this::toMobilityDto)
                .toList());
    }

    @GetMapping("/clearance")
    public ResponseEntity<?> clearance(@RequestHeader("Authorization") String authHeader) {
        User user = mobileApiAuthService.requireRole(authHeader, User.UserRole.STUDENT);
        Student student = getStudent(user);
        ClearanceSheet sheet = clearanceSheetRepository.findByStudentIdWithDetails(student.getId()).orElse(null);
        return ResponseEntity.ok(sheet == null
                ? new ClearanceDto(null, student.getId(), student.getName(), null, List.of())
                : toClearanceDto(sheet));
    }

    @GetMapping("/enrollments")
    public ResponseEntity<?> enrollments(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(required = false) Long semesterId) {
        User user = mobileApiAuthService.requireRole(authHeader, User.UserRole.STUDENT);
        Student student = getStudent(user);
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
    public ResponseEntity<?> enrollmentOptions(@RequestHeader("Authorization") String authHeader) {
        User user = mobileApiAuthService.requireRole(authHeader, User.UserRole.STUDENT);
        Student student = getStudent(user);
        List<SemesterOptionDto> semesters = buildEnrollmentSemesterOptions(student.getId());
        Long currentSemesterId = student.getCurrentSemester() != null
                ? student.getCurrentSemester().getId()
                : semesters.stream().findFirst().map(SemesterOptionDto::id).orElse(null);
        return ResponseEntity.ok(new EnrollmentOptionsDto(currentSemesterId, semesters));
    }

    @GetMapping("/course-registration/available")
    public ResponseEntity<?> availableCourses(@RequestHeader("Authorization") String authHeader) {
        User user = mobileApiAuthService.requireRole(authHeader, User.UserRole.STUDENT);
        Student student = getStudent(user);
        return ResponseEntity.ok(addDropService.getAvailableForAdd(student).stream()
                .map(this::toAvailableCourseDto)
                .toList());
    }

    @PostMapping("/course-registration/submit")
    public ResponseEntity<?> submitRegistration(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody CourseActionBody body) {
        User user = mobileApiAuthService.requireRole(authHeader, User.UserRole.STUDENT);
        Student student = getStudent(user);
        return ResponseEntity.ok(addDropService.registerCourse(student, body.sectionId()));
    }

    @PostMapping("/add-drop/add")
    public ResponseEntity<?> addCourse(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody CourseActionBody body) {
        User user = mobileApiAuthService.requireRole(authHeader, User.UserRole.STUDENT);
        Student student = getStudent(user);
        return ResponseEntity.ok(addDropService.addCourse(student, body.sectionId()));
    }

    @PostMapping("/add-drop/drop")
    public ResponseEntity<?> dropCourse(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody CourseActionBody body) {
        User user = mobileApiAuthService.requireRole(authHeader, User.UserRole.STUDENT);
        Student student = getStudent(user);
        return ResponseEntity.ok(addDropService.dropCourse(student, body.sectionId()));
    }

    @GetMapping("/requests")
    public ResponseEntity<?> requests(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "desc") String direction) {
        User user = mobileApiAuthService.requireRole(authHeader, User.UserRole.STUDENT);
        Student student = getStudent(user);
        var pageable = apiPageableFactory.create(
                page, size, sort, direction, "createdAt",
                Set.of("createdAt", "updatedAt", "category", "status"));
        var data = studentRequestRepository.findByStudentId(student.getId(), pageable)
                .map(r -> new RequestDto(r.getId(), r.getCategory(), r.getDescription(),
                        r.getStatus(), r.getCreatedAt(), r.getUpdatedAt()));
        return ResponseEntity.ok(ApiPageResponse.from(data));
    }

    @PostMapping("/requests")
    public ResponseEntity<?> createRequest(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody CreateRequestBody body) {
        User user = mobileApiAuthService.requireRole(authHeader, User.UserRole.STUDENT);
        Student student = getStudent(user);
        StudentRequest request = requestService.createRequest(student, body.category(), body.description());
        return ResponseEntity.ok(new RequestDto(request.getId(), request.getCategory(), request.getDescription(),
                request.getStatus(), request.getCreatedAt(), request.getUpdatedAt()));
    }

    @GetMapping("/requests/{id}/messages")
    public ResponseEntity<?> requestMessages(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {
        User user = mobileApiAuthService.requireRole(authHeader, User.UserRole.STUDENT);
        Student student = getStudent(user);
        StudentRequest req = studentRequestRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("Request not found"));
        if (!req.getStudent().getId().equals(student.getId())) {
            throw new IllegalArgumentException("Access denied");
        }
        return ResponseEntity.ok(requestService.getMessages(id).stream()
                .map(this::toRequestMessageDto)
                .toList());
    }

    @PostMapping("/requests/{id}/messages")
    public ResponseEntity<?> addRequestMessage(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id,
            @RequestBody AddMessageBody body) {
        User user = mobileApiAuthService.requireRole(authHeader, User.UserRole.STUDENT);
        Student student = getStudent(user);
        StudentRequest req = studentRequestRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("Request not found"));
        if (!req.getStudent().getId().equals(student.getId())) {
            throw new IllegalArgumentException("Access denied");
        }
        return ResponseEntity.ok(toRequestMessageDto(requestService.addMessage(id, user, body.message())));
    }

    private Student getStudent(User user) {
        return studentRepository.findByEmailWithDetails(user.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Student profile not found"));
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

    private ChecklistItemDto toChecklistItemDto(ChecklistItem item) {
        return new ChecklistItemDto(
                item.getId(),
                item.getTitle(),
                item.getDeadline(),
                item.isCompleted(),
                item.getLinkToSection()
        );
    }

    private MobilityDto toMobilityDto(MobilityApplication application) {
        return new MobilityDto(
                application.getId(),
                application.getStudent() != null ? application.getStudent().getId() : null,
                application.getUniversityName(),
                application.getDisciplinesMapping(),
                application.getStatus(),
                application.getCreatedAt()
        );
    }

    private ClearanceDto toClearanceDto(ClearanceSheet sheet) {
        return new ClearanceDto(
                sheet.getId(),
                sheet.getStudent() != null ? sheet.getStudent().getId() : null,
                sheet.getStudent() != null ? sheet.getStudent().getName() : null,
                sheet.getStatus(),
                sheet.getCheckpoints().stream()
                        .map(cp -> new ClearanceCheckpointDto(cp.getId(), cp.getDepartment(), cp.getStatus(), cp.getComment()))
                        .toList()
        );
    }

    private RequestMessageDto toRequestMessageDto(RequestMessage message) {
        return new RequestMessageDto(
                message.getId(),
                message.getSender() != null ? message.getSender().getId() : null,
                message.getSender() != null ? message.getSender().getEmail() : null,
                message.getSender() != null ? message.getSender().getFullName() : null,
                message.getMessage(),
                message.getCreatedAt()
        );
    }

    private StudentProfileDto toStudentProfileDto(Student student) {
        return new StudentProfileDto(
                student.getId(),
                student.getName(),
                student.getEmail(),
                student.getCourse(),
                student.getGroupName(),
                student.getStatus(),
                student.getProgram() != null ? student.getProgram().getName() : null,
                student.getFaculty() != null ? student.getFaculty().getName() : null,
                student.getCreditsEarned(),
                student.getPhone(),
                buildProfilePhotoUrl(student.getProfilePhotoAssetId())
        );
    }

    private String buildProfilePhotoUrl(Long fileAssetId) {
        if (fileAssetId == null) {
            return null;
        }
        return fileAssetRepository.findById(fileAssetId)
                .map(file -> fileLinkService.createAssetDownloadUrl(file.getId()))
                .orElse(null);
    }

    public record StudentProfileDto(Long id, String name, String email, int course, String groupName,
                                     Student.StudentStatus status, String program, String faculty,
                                     int creditsEarned, String phone, String profilePhotoUrl) {}
    public record ScheduleItemDto(Long sectionId, String courseCode, String courseName,
                                   java.time.DayOfWeek dayOfWeek, java.time.LocalTime startTime,
                                   java.time.LocalTime endTime, String room, String teacherName,
                                   Registration.RegistrationStatus status, Long semesterId,
                                   String semesterName, String academicYear, String season,
                                   SubjectOffering.LessonType lessonType) {}
    public record ScheduleOptionsDto(Long currentSemesterId, List<SemesterOptionDto> semesters) {}
    public record SemesterOptionDto(Long id, String name, String academicYear, String season, boolean current) {}
    public record StudentEnrollmentDto(Long id, Long sectionId, String subjectCode, String subjectName,
                                       String teacherName, Integer credits, Long semesterId,
                                       String semesterName, String academicYear, String season,
                                       Registration.RegistrationStatus status, Instant createdAt) {}
    public record EnrollmentOptionsDto(Long currentSemesterId, List<SemesterOptionDto> semesters) {}
    public record JournalCourseRowDto(Long sectionId, String courseCode, String courseName,
                                      Long semesterId, String semesterName, String academicYear,
                                      String season, Double attestation1, Double attestation1Max,
                                      Double attestation2, Double attestation2Max, Double finalExam,
                                      Double finalExamMax, Double totalScore, String letterValue) {}
    public record JournalOptionsDto(Long currentSemesterId, List<SemesterOptionDto> semesters) {}
    public record TranscriptItemDto(Long id, String courseCode, String courseName, int credits,
                                     double numericValue, String letterValue, double points,
                                     FinalGrade.FinalGradeStatus status) {}
    public record AnnouncementDto(Long id, String title, String content, Long sectionId,
                                   String sectionCode, Instant publishedAt, boolean pinned) {}
    public record MaterialDto(Long id, String title, String description, String originalFileName,
                              String contentType, long sizeBytes, Instant createdAt, String downloadUrl) {}
    public record StudentFileDto(Long id, String fileName, FileAsset.FileCategory category,
                                 String contentType, long sizeBytes, Instant uploadedAt, String downloadUrl) {}
    public record ChecklistItemDto(Long id, String title, java.time.LocalDate deadline,
                                   boolean completed, String linkToSection) {}
    public record MobilityDto(Long id, Long studentId, String universityName,
                              String disciplinesMapping, MobilityApplication.MobilityStatus status,
                              Instant createdAt) {}
    public record ClearanceDto(Long id, Long studentId, String studentName,
                               ClearanceSheet.ClearanceStatus status, List<ClearanceCheckpointDto> checkpoints) {}
    public record ClearanceCheckpointDto(Long id, String department,
                                         ClearanceCheckpoint.CheckpointStatus status, String comment) {}
    public record AvailableCourseDto(Long sectionId, String subjectCode, String subjectName, int credits,
                                     Long semesterId, String semesterName, Long teacherId, String teacherName,
                                     int capacity, SubjectOffering.LessonType lessonType,
                                     java.time.DayOfWeek dayOfWeek, java.time.LocalTime startTime,
                                     java.time.LocalTime endTime, String room) {}
    public record RequestDto(Long id, String category, String description,
                              StudentRequest.RequestStatus status, Instant createdAt, Instant updatedAt) {}
    public record RequestMessageDto(Long id, Long senderUserId, String senderEmail,
                                    String senderName, String message, Instant createdAt) {}
    public record CreateRequestBody(String category, String description) {}
    public record AddMessageBody(String message) {}
    public record CourseActionBody(Long sectionId) {}

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
                    sectionId,
                    courseCode,
                    courseName,
                    semesterId,
                    semesterName,
                    academicYear,
                    season,
                    attestation1,
                    attestation1Max,
                    attestation2,
                    attestation2Max,
                    finalExam,
                    finalExamMax,
                    totalScore,
                    letterValue
            );
        }
    }
}
