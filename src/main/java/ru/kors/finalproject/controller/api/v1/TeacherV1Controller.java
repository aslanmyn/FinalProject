package ru.kors.finalproject.controller.api.v1;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.kors.finalproject.entity.*;
import ru.kors.finalproject.repository.FileAssetRepository;
import ru.kors.finalproject.repository.RegistrationRepository;
import ru.kors.finalproject.repository.SubjectOfferingRepository;
import ru.kors.finalproject.service.*;
import ru.kors.finalproject.web.api.v1.CurrentUserHelper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/teacher")
@RequiredArgsConstructor
@Tag(name = "Teacher Teaching", description = "Teacher profile, sections, attendance control, gradebook, notes, materials, and section-level operations.")
@SecurityRequirement(name = "Bearer")
public class TeacherV1Controller {
    private final CurrentUserHelper currentUserHelper;
    private final SubjectOfferingRepository subjectOfferingRepository;
    private final RegistrationRepository registrationRepository;
    private final TeacherAcademicService teacherAcademicService;
    private final AttendanceFlowService attendanceFlowService;
    private final AnnouncementService announcementService;
    private final GradeChangeService gradeChangeService;
    private final CourseMaterialService courseMaterialService;
    private final FileLinkService fileLinkService;
    private final FileAssetRepository fileAssetRepository;
    private final FileStorageService fileStorageService;
    private final NotificationService notificationService;

    @GetMapping("/profile")
    public ResponseEntity<?> profile(@AuthenticationPrincipal User user) {
        Teacher teacher = currentUserHelper.requireTeacher(user);
        return ResponseEntity.ok(toTeacherProfileDto(teacher));
    }

    @PostMapping(value = "/profile-photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadProfilePhoto(
            @AuthenticationPrincipal User user,
            @RequestParam("file") MultipartFile file) {
        Teacher teacher = currentUserHelper.requireTeacher(user);
        if (file.getContentType() == null || !file.getContentType().toLowerCase().startsWith("image/")) {
            throw new IllegalArgumentException("Only image files are allowed for profile photo");
        }

        FileStorageService.StoredFile stored = fileStorageService.store(file, "profile-photos/teacher-" + teacher.getId());
        FileAsset previousAsset = teacher.getProfilePhotoAssetId() != null
                ? fileAssetRepository.findById(teacher.getProfilePhotoAssetId()).orElse(null)
                : null;

        FileAsset savedAsset = fileAssetRepository.save(FileAsset.builder()
                .originalName(stored.originalName())
                .storagePath(stored.storagePath())
                .contentType(stored.contentType())
                .sizeBytes(stored.sizeBytes())
                .category(FileAsset.FileCategory.OTHER)
                .linkedEntityType("TEACHER_PROFILE_PHOTO")
                .linkedEntityId(teacher.getId())
                .uploadedBy(user)
                .uploadedAt(Instant.now())
                .build());

        teacher.setProfilePhotoAssetId(savedAsset.getId());
        currentUserHelper.saveTeacher(teacher);

        if (previousAsset != null) {
            fileStorageService.deleteSilently(previousAsset.getStoragePath());
            fileAssetRepository.delete(previousAsset);
        }

        return ResponseEntity.ok(toTeacherProfileDto(teacher));
    }

    @GetMapping("/sections")
    public ResponseEntity<?> sections(@AuthenticationPrincipal User user) {
        Teacher teacher = currentUserHelper.requireTeacher(user);
        return ResponseEntity.ok(teacherAcademicService.getMySections(teacher).stream()
                .map(this::toTeacherSectionDto)
                .toList());
    }

    @GetMapping("/notifications")
    public ResponseEntity<?> notifications(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(Map.of(
                "notifications", notificationService.listForEmail(user.getEmail()).stream()
                        .map(this::toNotificationDto)
                        .toList(),
                "unreadCount", notificationService.unreadCount(user.getEmail())
        ));
    }

    @PostMapping("/notifications/{id}/read")
    public ResponseEntity<?> markNotificationRead(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        notificationService.markReadForEmail(id, user.getEmail());
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @PostMapping("/notifications/read-all")
    public ResponseEntity<?> markAllNotificationsRead(@AuthenticationPrincipal User user) {
        notificationService.markAllReadForEmail(user.getEmail());
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @GetMapping("/sections/{sectionId}/roster")
    public ResponseEntity<?> roster(@AuthenticationPrincipal User user, @PathVariable Long sectionId) {
        Teacher teacher = currentUserHelper.requireTeacher(user);
        return ResponseEntity.ok(teacherAcademicService.getRoster(teacher, sectionId).stream().map(r -> Map.of(
                "registrationId", r.getId(),
                "studentId", r.getStudent().getId(),
                "studentName", r.getStudent().getName(),
                "studentEmail", r.getStudent().getEmail(),
                "status", r.getStatus()
        )).toList());
    }

    @PostMapping("/sections/{sectionId}/attendance")
    public ResponseEntity<?> markAttendance(
            @AuthenticationPrincipal User user,
            @PathVariable Long sectionId,
            @RequestBody AttendanceBody body) {
        Teacher teacher = currentUserHelper.requireTeacher(user);
        teacherAcademicService.markAttendance(teacher, sectionId, LocalDate.parse(body.classDate()), body.marks());
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @GetMapping("/sections/{sectionId}/attendance/active")
    public ResponseEntity<?> activeAttendanceSession(
            @AuthenticationPrincipal User user,
            @PathVariable Long sectionId,
            @RequestParam(required = false) String classDate) {
        Teacher teacher = currentUserHelper.requireTeacher(user);
        LocalDate effectiveDate = classDate != null && !classDate.isBlank()
                ? LocalDate.parse(classDate)
                : LocalDate.now();
        AttendanceSession session = attendanceFlowService.getSectionSession(teacher, sectionId, effectiveDate);
        if (session == null) {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("session", null);
            payload.put("records", List.of());
            return ResponseEntity.ok(payload);
        }
        return ResponseEntity.ok(Map.of(
                "session", toAttendanceSessionDto(session),
                "records", attendanceFlowService.getSessionRosterView(teacher, session.getId()).stream()
                        .map(this::toTeacherAttendanceRecordDto)
                        .toList()
        ));
    }

    @PostMapping("/sections/{sectionId}/attendance/open")
    public ResponseEntity<?> openAttendanceSession(
            @AuthenticationPrincipal User user,
            @PathVariable Long sectionId,
            @RequestBody OpenAttendanceBody body) {
        Teacher teacher = currentUserHelper.requireTeacher(user);
        AttendanceSession session = attendanceFlowService.openSession(
                teacher,
                sectionId,
                LocalDate.parse(body.classDate()),
                attendanceFlowService.parseCloseAt(body.closeAt()),
                body.checkInMode() != null ? body.checkInMode() : AttendanceSession.CheckInMode.ONE_CLICK,
                body.checkInCode(),
                body.allowTeacherOverride()
        );
        return ResponseEntity.ok(Map.of(
                "session", toAttendanceSessionDto(session),
                "records", attendanceFlowService.getSessionRosterView(teacher, session.getId()).stream()
                        .map(this::toTeacherAttendanceRecordDto)
                        .toList()
        ));
    }

    @PostMapping("/attendance-sessions/{sessionId}/close")
    public ResponseEntity<?> closeAttendanceSession(
            @AuthenticationPrincipal User user,
            @PathVariable Long sessionId) {
        Teacher teacher = currentUserHelper.requireTeacher(user);
        AttendanceSession session = attendanceFlowService.closeSession(teacher, sessionId);
        return ResponseEntity.ok(Map.of(
                "session", toAttendanceSessionDto(session),
                "records", attendanceFlowService.getSessionRosterView(teacher, session.getId()).stream()
                        .map(this::toTeacherAttendanceRecordDto)
                        .toList()
        ));
    }

    @GetMapping("/attendance-sessions/{sessionId}/records")
    public ResponseEntity<?> attendanceSessionRecords(
            @AuthenticationPrincipal User user,
            @PathVariable Long sessionId) {
        Teacher teacher = currentUserHelper.requireTeacher(user);
        return ResponseEntity.ok(attendanceFlowService.getSessionRosterView(teacher, sessionId).stream()
                .map(this::toTeacherAttendanceRecordDto)
                .toList());
    }

    @PutMapping("/attendance-sessions/{sessionId}/students/{studentId}")
    public ResponseEntity<?> overrideAttendance(
            @AuthenticationPrincipal User user,
            @PathVariable Long sessionId,
            @PathVariable Long studentId,
            @RequestBody TeacherAttendanceOverrideBody body) {
        Teacher teacher = currentUserHelper.requireTeacher(user);
        attendanceFlowService.overrideAttendance(
                teacher,
                sessionId,
                studentId,
                body.status(),
                body.reason()
        );
        return ResponseEntity.ok(toTeacherAttendanceRecordDto(
                attendanceFlowService.getSessionRosterView(teacher, sessionId).stream()
                        .filter(item -> item.studentId().equals(studentId))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Attendance record not found"))
        ));
    }

    @GetMapping("/sections/{sectionId}/components")
    public ResponseEntity<?> components(@AuthenticationPrincipal User user, @PathVariable Long sectionId) {
        Teacher teacher = currentUserHelper.requireTeacher(user);
        return ResponseEntity.ok(teacherAcademicService.componentsForOffering(teacher, sectionId).stream().map(c -> Map.of(
                "id", (Object) c.getId(),
                "name", c.getName(),
                "type", c.getType(),
                "weightPercent", c.getWeightPercent(),
                "status", c.getStatus(),
                "published", c.isPublished(),
                "locked", c.isLocked()
        )).toList());
    }

    @PostMapping("/sections/{sectionId}/components")
    public ResponseEntity<?> createComponent(
            @AuthenticationPrincipal User user,
            @PathVariable Long sectionId,
            @RequestBody CreateComponentBody body) {
        Teacher teacher = currentUserHelper.requireTeacher(user);
        return ResponseEntity.ok(toComponentDto(teacherAcademicService.createComponent(
                teacher, sectionId, body.name(), body.type(), body.weightPercent())));
    }

    @PostMapping("/sections/{sectionId}/components/{componentId}/publish")
    public ResponseEntity<?> publishComponent(
            @AuthenticationPrincipal User user,
            @PathVariable Long sectionId,
            @PathVariable Long componentId,
            @RequestParam boolean published) {
        Teacher teacher = currentUserHelper.requireTeacher(user);
        return ResponseEntity.ok(toComponentDto(
                teacherAcademicService.setComponentPublishState(teacher, sectionId, componentId, published)));
    }

    @PostMapping("/sections/{sectionId}/components/{componentId}/lock")
    public ResponseEntity<?> lockComponent(
            @AuthenticationPrincipal User user,
            @PathVariable Long sectionId,
            @PathVariable Long componentId,
            @RequestParam boolean locked) {
        Teacher teacher = currentUserHelper.requireTeacher(user);
        return ResponseEntity.ok(toComponentDto(
                teacherAcademicService.lockComponent(teacher, sectionId, componentId, locked)));
    }

    @PostMapping("/sections/{sectionId}/grades")
    public ResponseEntity<?> saveGrade(
            @AuthenticationPrincipal User user,
            @PathVariable Long sectionId,
            @RequestBody SaveGradeBody body) {
        Teacher teacher = currentUserHelper.requireTeacher(user);
        return ResponseEntity.ok(toGradeDto(teacherAcademicService.saveGrade(
                teacher, sectionId, body.studentId(), body.componentId(),
                body.gradeValue(), body.maxGradeValue(), body.comment())));
    }

    @PostMapping("/sections/{sectionId}/final-grades")
    public ResponseEntity<?> saveFinalGrade(
            @AuthenticationPrincipal User user,
            @PathVariable Long sectionId,
            @RequestBody SaveFinalGradeBody body) {
        Teacher teacher = currentUserHelper.requireTeacher(user);
        return ResponseEntity.ok(toFinalGradeDto(teacherAcademicService.upsertFinalGrade(
                teacher, sectionId, body.studentId(), body.numericValue(), body.letterValue(), body.points())));
    }

    @PostMapping("/sections/{sectionId}/final-grades/{studentId}/publish")
    public ResponseEntity<?> publishFinalGrade(
            @AuthenticationPrincipal User user,
            @PathVariable Long sectionId,
            @PathVariable Long studentId) {
        Teacher teacher = currentUserHelper.requireTeacher(user);
        return ResponseEntity.ok(toFinalGradeDto(
                teacherAcademicService.publishFinalGrade(teacher, sectionId, studentId)));
    }

    @GetMapping("/sections/{sectionId}/announcements")
    public ResponseEntity<?> announcements(@AuthenticationPrincipal User user, @PathVariable Long sectionId) {
        Teacher teacher = currentUserHelper.requireTeacher(user);
        return ResponseEntity.ok(announcementService.listForSection(teacher, sectionId).stream()
                .map(this::toAnnouncementDto)
                .toList());
    }

    @PostMapping("/sections/{sectionId}/announcements")
    public ResponseEntity<?> createAnnouncement(
            @AuthenticationPrincipal User user,
            @PathVariable Long sectionId,
            @RequestBody CreateAnnouncementBody body) {
        Teacher teacher = currentUserHelper.requireTeacher(user);
        Instant scheduledAt = null;
        if (body.scheduledAt() != null && !body.scheduledAt().isBlank()) {
            try {
                scheduledAt = Instant.parse(body.scheduledAt());
            } catch (Exception ex) {
                scheduledAt = LocalDateTime.parse(body.scheduledAt()).atZone(ZoneId.systemDefault()).toInstant();
            }
        }
        return ResponseEntity.ok(toAnnouncementDto(announcementService.createAnnouncement(
                teacher, sectionId, body.title(), body.content(), body.publicVisible(), body.pinned(), scheduledAt)));
    }

    @GetMapping("/sections/{sectionId}/materials")
    public ResponseEntity<?> materials(@AuthenticationPrincipal User user, @PathVariable Long sectionId) {
        Teacher teacher = currentUserHelper.requireTeacher(user);
        return ResponseEntity.ok(courseMaterialService.listForSection(teacher, sectionId).stream().map(m -> new MaterialDto(
                m.getId(), m.getTitle(), m.getDescription(), m.getOriginalFileName(),
                m.getContentType(), m.getSizeBytes(), m.getVisibility(), m.isPublished(), m.getCreatedAt(),
                fileLinkService.createMaterialDownloadUrl(m.getId())
        )).toList());
    }

    @PostMapping(value = "/sections/{sectionId}/materials", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadMaterial(
            @AuthenticationPrincipal User user,
            @PathVariable Long sectionId,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "ENROLLED_ONLY") CourseMaterial.MaterialVisibility visibility) {
        Teacher teacher = currentUserHelper.requireTeacher(user);
        CourseMaterial saved = courseMaterialService.upload(
                teacher, sectionId, title, description, file, visibility);
        return ResponseEntity.ok(new MaterialDto(saved.getId(), saved.getTitle(), saved.getDescription(),
                saved.getOriginalFileName(), saved.getContentType(), saved.getSizeBytes(),
                saved.getVisibility(), saved.isPublished(), saved.getCreatedAt(),
                fileLinkService.createMaterialDownloadUrl(saved.getId())));
    }

    @PostMapping("/materials/{materialId}/visibility")
    public ResponseEntity<?> updateMaterialVisibility(
            @AuthenticationPrincipal User user,
            @PathVariable Long materialId,
            @RequestParam boolean published) {
        Teacher teacher = currentUserHelper.requireTeacher(user);
        CourseMaterial updated = courseMaterialService.updateVisibility(teacher, materialId, published);
        return ResponseEntity.ok(new MaterialDto(updated.getId(), updated.getTitle(), updated.getDescription(),
                updated.getOriginalFileName(), updated.getContentType(), updated.getSizeBytes(),
                updated.getVisibility(), updated.isPublished(), updated.getCreatedAt(),
                fileLinkService.createMaterialDownloadUrl(updated.getId())));
    }

    @DeleteMapping("/materials/{materialId}")
    public ResponseEntity<?> deleteMaterial(
            @AuthenticationPrincipal User user,
            @PathVariable Long materialId) {
        Teacher teacher = currentUserHelper.requireTeacher(user);
        courseMaterialService.delete(teacher, materialId);
        return ResponseEntity.ok(Map.of("status", "deleted"));
    }

    @GetMapping("/sections/{sectionId}/grades/export")
    public ResponseEntity<byte[]> exportGrades(
            @AuthenticationPrincipal User user,
            @PathVariable Long sectionId) throws IOException {
        Teacher teacher = currentUserHelper.requireTeacher(user);

        SubjectOffering offering = subjectOfferingRepository.findByIdWithDetails(sectionId)
                .orElseThrow(() -> new IllegalArgumentException("Section not found"));
        if (offering.getTeacher() == null || !offering.getTeacher().getId().equals(teacher.getId())) {
            throw new IllegalArgumentException("Section is not assigned to current teacher");
        }

        List<Grade> grades = teacherAcademicService.getGradesForSection(sectionId);
        String subjectCode = offering.getSubject() != null ? offering.getSubject().getCode() : "section_" + sectionId;

        byte[] content;
        try (var workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
             var outputStream = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Grades");
            var headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("Student ID");
            headerRow.createCell(1).setCellValue("Student Name");
            headerRow.createCell(2).setCellValue("Component");
            headerRow.createCell(3).setCellValue("Grade");
            headerRow.createCell(4).setCellValue("Max Grade");
            headerRow.createCell(5).setCellValue("Comment");

            int rowIdx = 1;
            for (Grade g : grades) {
                var row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(g.getStudent().getId());
                row.createCell(1).setCellValue(g.getStudent().getName());
                row.createCell(2).setCellValue(g.getComponent() != null
                        ? g.getComponent().getName()
                        : (g.getType() != null ? g.getType().name() : ""));
                row.createCell(3).setCellValue(g.getGradeValue());
                row.createCell(4).setCellValue(g.getMaxGradeValue());
                row.createCell(5).setCellValue(g.getComment() != null ? g.getComment() : "");
            }
            for (int i = 0; i < 6; i++) {
                sheet.autoSizeColumn(i);
            }
            workbook.write(outputStream);
            content = outputStream.toByteArray();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("grades_" + subjectCode + ".xlsx")
                .build());
        headers.setContentLength(content.length);
        return ResponseEntity.ok().headers(headers).body(content);
    }

    @GetMapping("/sections/{sectionId}/student-notes")
    public ResponseEntity<?> notes(@AuthenticationPrincipal User user, @PathVariable Long sectionId) {
        Teacher teacher = currentUserHelper.requireTeacher(user);
        return ResponseEntity.ok(teacherAcademicService.notesForSection(teacher, sectionId).stream()
                .map(this::toTeacherNoteDto)
                .toList());
    }

    @PostMapping("/sections/{sectionId}/student-notes")
    public ResponseEntity<?> upsertNote(
            @AuthenticationPrincipal User user,
            @PathVariable Long sectionId,
            @RequestBody NoteBody body) {
        Teacher teacher = currentUserHelper.requireTeacher(user);
        return ResponseEntity.ok(toTeacherNoteDto(teacherAcademicService.upsertStudentNote(
                teacher, sectionId, body.studentId(), body.note(), body.riskFlag())));
    }

    @GetMapping("/grade-change-requests")
    public ResponseEntity<?> gradeChangeRequests(@AuthenticationPrincipal User user) {
        Teacher teacher = currentUserHelper.requireTeacher(user);
        return ResponseEntity.ok(gradeChangeService.listForTeacher(teacher).stream()
                .map(this::toGradeChangeRequestDto)
                .toList());
    }

    @PostMapping("/sections/{sectionId}/grade-change-requests")
    public ResponseEntity<?> createGradeChangeRequest(
            @AuthenticationPrincipal User user,
            @PathVariable Long sectionId,
            @RequestBody GradeChangeBody body) {
        Teacher teacher = currentUserHelper.requireTeacher(user);
        return ResponseEntity.ok(toGradeChangeRequestDto(gradeChangeService.createForComponentGrade(
                teacher, sectionId, body.gradeId(), body.newValue(), body.reason())));
    }

    @PostMapping(value = "/sections/{sectionId}/student-files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadStudentFile(
            @AuthenticationPrincipal User user,
            @PathVariable Long sectionId,
            @RequestParam Long studentId,
            @RequestParam("file") MultipartFile file) {
        Teacher teacher = currentUserHelper.requireTeacher(user);
        FileAsset saved = teacherAcademicService.uploadStudentFile(teacher, sectionId, studentId, file);
        return ResponseEntity.ok(new StudentFileDto(
                saved.getId(),
                saved.getLinkedEntityId(),
                saved.getOriginalName(),
                saved.getContentType(),
                saved.getSizeBytes(),
                saved.getUploadedAt(),
                fileLinkService.createAssetDownloadUrl(saved.getId())
        ));
    }

    private TeacherProfileDto toTeacherProfileDto(Teacher teacher) {
        return new TeacherProfileDto(
                teacher.getId(),
                teacher.getName(),
                teacher.getEmail(),
                teacher.getDepartment() != null ? teacher.getDepartment() : "",
                teacher.getPositionTitle() != null ? teacher.getPositionTitle() : "",
                teacher.getOfficeHours() != null ? teacher.getOfficeHours() : "",
                teacher.getOfficeRoom() != null ? teacher.getOfficeRoom() : "",
                teacher.getRole(),
                teacher.getFaculty() != null ? teacher.getFaculty().getName() : "",
                buildTeacherProfilePhotoUrl(teacher)
        );
    }

    private TeacherSectionDto toTeacherSectionDto(SubjectOffering offering) {
        long enrolledCount = registrationRepository.countBySubjectOfferingIdAndStatusIn(
                offering.getId(),
                List.of(Registration.RegistrationStatus.CONFIRMED, Registration.RegistrationStatus.SUBMITTED)
        );

        List<TeacherSectionMeetingTimeDto> meetingTimes = offering.getMeetingTimes() == null
                ? List.of()
                : offering.getMeetingTimes().stream()
                .sorted(Comparator
                        .comparing(MeetingTime::getDayOfWeek)
                        .thenComparing(MeetingTime::getStartTime))
                .map(this::toTeacherSectionMeetingTimeDto)
                .toList();

        String programName = offering.getSubject() != null && offering.getSubject().getProgram() != null
                ? offering.getSubject().getProgram().getName()
                : null;
        String facultyName = offering.getSubject() != null
                && offering.getSubject().getProgram() != null
                && offering.getSubject().getProgram().getFaculty() != null
                ? offering.getSubject().getProgram().getFaculty().getName()
                : null;

        return new TeacherSectionDto(
                offering.getId(),
                offering.getSubject() != null ? offering.getSubject().getCode() : null,
                offering.getSubject() != null ? offering.getSubject().getName() : null,
                offering.getSubject() != null ? offering.getSubject().getCredits() : 0,
                programName,
                facultyName,
                offering.getSemester() != null ? offering.getSemester().getId() : null,
                offering.getSemester() != null ? offering.getSemester().getName() : null,
                offering.getSemester() != null && offering.getSemester().isCurrent(),
                offering.getCapacity(),
                enrolledCount,
                offering.getLessonType(),
                meetingTimes
        );
    }

    private TeacherSectionMeetingTimeDto toTeacherSectionMeetingTimeDto(MeetingTime meetingTime) {
        return new TeacherSectionMeetingTimeDto(
                meetingTime.getId(),
                meetingTime.getDayOfWeek(),
                meetingTime.getStartTime(),
                meetingTime.getEndTime(),
                meetingTime.getRoom(),
                meetingTime.getLessonType()
        );
    }

    private String buildTeacherProfilePhotoUrl(Teacher teacher) {
        if (teacher.getProfilePhotoAssetId() != null) {
            return fileAssetRepository.findById(teacher.getProfilePhotoAssetId())
                    .map(file -> fileLinkService.createAssetDownloadUrl(file.getId()))
                    .orElse(null);
        }
        return teacher.getPhotoUrl();
    }

    private ComponentDto toComponentDto(AssessmentComponent component) {
        return new ComponentDto(
                component.getId(),
                component.getSubjectOffering() != null ? component.getSubjectOffering().getId() : null,
                component.getName(),
                component.getType(),
                component.getWeightPercent(),
                component.getStatus(),
                component.isPublished(),
                component.isLocked(),
                component.getCreatedAt()
        );
    }

    private GradeDto toGradeDto(Grade grade) {
        return new GradeDto(
                grade.getId(),
                grade.getStudent() != null ? grade.getStudent().getId() : null,
                grade.getSubjectOffering() != null ? grade.getSubjectOffering().getId() : null,
                grade.getComponent() != null ? grade.getComponent().getId() : null,
                grade.getType(),
                grade.getGradeValue(),
                grade.getMaxGradeValue(),
                grade.getComment(),
                grade.isPublished(),
                grade.getCreatedAt()
        );
    }

    private FinalGradeDto toFinalGradeDto(FinalGrade finalGrade) {
        return new FinalGradeDto(
                finalGrade.getId(),
                finalGrade.getStudent() != null ? finalGrade.getStudent().getId() : null,
                finalGrade.getSubjectOffering() != null ? finalGrade.getSubjectOffering().getId() : null,
                finalGrade.getNumericValue(),
                finalGrade.getLetterValue(),
                finalGrade.getPoints(),
                finalGrade.getStatus(),
                finalGrade.isPublished(),
                finalGrade.getPublishedAt(),
                finalGrade.getCreatedAt(),
                finalGrade.getUpdatedAt()
        );
    }

    private TeacherAnnouncementDto toAnnouncementDto(CourseAnnouncement announcement) {
        return new TeacherAnnouncementDto(
                announcement.getId(),
                announcement.getSubjectOffering() != null ? announcement.getSubjectOffering().getId() : null,
                announcement.getTitle(),
                announcement.getContent(),
                announcement.isPublicVisible(),
                announcement.isPublished(),
                announcement.isPinned(),
                announcement.getScheduledAt(),
                announcement.getPublishedAt(),
                announcement.getCreatedAt(),
                announcement.getUpdatedAt()
        );
    }

    private TeacherNoteDto toTeacherNoteDto(TeacherStudentNote note) {
        return new TeacherNoteDto(
                note.getId(),
                note.getStudent() != null ? note.getStudent().getId() : null,
                note.getStudent() != null ? note.getStudent().getName() : null,
                note.getNote(),
                note.getRiskFlag(),
                note.getCreatedAt(),
                note.getUpdatedAt()
        );
    }

    private GradeChangeRequestDto toGradeChangeRequestDto(GradeChangeRequest request) {
        return new GradeChangeRequestDto(
                request.getId(),
                request.getTeacher() != null ? request.getTeacher().getId() : null,
                request.getStudent() != null ? request.getStudent().getId() : null,
                request.getSubjectOffering() != null ? request.getSubjectOffering().getId() : null,
                request.getGrade() != null ? request.getGrade().getId() : null,
                request.getOldValue(),
                request.getNewValue(),
                request.getReason(),
                request.getStatus(),
                request.getCreatedAt(),
                request.getReviewedAt(),
                request.getAppliedAt()
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

    private AttendanceSessionDto toAttendanceSessionDto(AttendanceSession session) {
        return new AttendanceSessionDto(
                session.getId(),
                session.getSubjectOffering().getId(),
                session.getSubjectOffering().getSubject().getCode(),
                session.getSubjectOffering().getSubject().getName(),
                session.getClassDate(),
                session.getStatus(),
                session.getCheckInMode(),
                session.isAllowTeacherOverride(),
                session.isLocked(),
                session.getAttendanceCloseAt(),
                session.getOpenedAt(),
                session.getClosedAt(),
                session.getCheckInMode() == AttendanceSession.CheckInMode.CODE ? session.getCheckInCode() : null
        );
    }

    private TeacherAttendanceRecordDto toTeacherAttendanceRecordDto(AttendanceFlowService.TeacherAttendanceRecordView record) {
        return new TeacherAttendanceRecordDto(
                record.studentId(),
                record.studentName(),
                record.studentEmail(),
                record.attendanceId(),
                record.status(),
                record.reason(),
                record.markedBy(),
                record.teacherConfirmed(),
                record.markedAt(),
                record.updatedAt()
        );
    }

    public record AttendanceBody(String classDate, List<TeacherAcademicService.AttendanceMarkInput> marks) {}
    public record OpenAttendanceBody(
            String classDate,
            String closeAt,
            AttendanceSession.CheckInMode checkInMode,
            String checkInCode,
            boolean allowTeacherOverride
    ) {}
    public record TeacherAttendanceOverrideBody(Attendance.AttendanceStatus status, String reason) {}
    public record AttendanceSessionDto(
            Long id,
            Long sectionId,
            String subjectCode,
            String subjectName,
            LocalDate classDate,
            AttendanceSession.SessionStatus status,
            AttendanceSession.CheckInMode checkInMode,
            boolean allowTeacherOverride,
            boolean locked,
            Instant attendanceCloseAt,
            Instant openedAt,
            Instant closedAt,
            String checkInCode
    ) {}
    public record TeacherAttendanceRecordDto(
            Long studentId,
            String studentName,
            String studentEmail,
            Long attendanceId,
            Attendance.AttendanceStatus status,
            String reason,
            Attendance.MarkedBy markedBy,
            boolean teacherConfirmed,
            Instant markedAt,
            Instant updatedAt
    ) {}
    public record TeacherProfileDto(Long id, String name, String email, String department,
                                    String position, String officeHours, String officeRoom,
                                    Teacher.TeacherRole teacherRole, String faculty,
                                    String profilePhotoUrl) {}
    public record NotificationDto(Long id, Notification.NotificationType type, String title,
                                  String message, String link, boolean read, Instant createdAt) {}
    public record TeacherSectionDto(Long id, String subjectCode, String subjectName, int credits,
                                    String programName, String facultyName,
                                    Long semesterId, String semesterName, boolean currentSemester,
                                    int capacity, long enrolledCount,
                                    SubjectOffering.LessonType lessonType,
                                    List<TeacherSectionMeetingTimeDto> meetingTimes) {}
    public record TeacherSectionMeetingTimeDto(Long id, java.time.DayOfWeek dayOfWeek,
                                               java.time.LocalTime startTime, java.time.LocalTime endTime,
                                               String room, SubjectOffering.LessonType lessonType) {}
    public record ComponentDto(Long id, Long sectionId, String name, AssessmentComponent.ComponentType type,
                               double weightPercent, AssessmentComponent.ComponentStatus status,
                               boolean published, boolean locked, Instant createdAt) {}
    public record CreateComponentBody(String name, AssessmentComponent.ComponentType type, double weightPercent) {}
    public record GradeDto(Long id, Long studentId, Long sectionId, Long componentId, Grade.GradeType type,
                           double gradeValue, double maxGradeValue, String comment,
                           boolean published, Instant createdAt) {}
    public record SaveGradeBody(Long studentId, Long componentId, double gradeValue, double maxGradeValue, String comment) {}
    public record FinalGradeDto(Long id, Long studentId, Long sectionId, double numericValue, String letterValue,
                                double points, FinalGrade.FinalGradeStatus status, boolean published,
                                Instant publishedAt, Instant createdAt, Instant updatedAt) {}
    public record SaveFinalGradeBody(Long studentId, double numericValue, String letterValue, double points) {}
    public record CreateAnnouncementBody(String title, String content, boolean publicVisible, boolean pinned, String scheduledAt) {}
    public record TeacherAnnouncementDto(Long id, Long sectionId, String title, String content,
                                         boolean publicVisible, boolean published, boolean pinned,
                                         Instant scheduledAt, Instant publishedAt,
                                         Instant createdAt, Instant updatedAt) {}
    public record MaterialDto(Long id, String title, String description, String originalFileName,
                               String contentType, long sizeBytes, CourseMaterial.MaterialVisibility visibility,
                               boolean published, Instant createdAt, String downloadUrl) {}
    public record StudentFileDto(Long id, Long studentId, String fileName, String contentType, long sizeBytes,
                                 Instant uploadedAt, String downloadUrl) {}
    public record TeacherNoteDto(Long id, Long studentId, String studentName, String note,
                                 TeacherStudentNote.RiskFlag riskFlag, Instant createdAt, Instant updatedAt) {}
    public record NoteBody(Long studentId, String note, TeacherStudentNote.RiskFlag riskFlag) {}
    public record GradeChangeRequestDto(Long id, Long teacherId, Long studentId, Long sectionId, Long gradeId,
                                        Double oldValue, Double newValue, String reason,
                                        GradeChangeRequest.RequestStatus status, Instant createdAt,
                                        Instant reviewedAt, Instant appliedAt) {}
    public record GradeChangeBody(Long gradeId, double newValue, String reason) {}
}
