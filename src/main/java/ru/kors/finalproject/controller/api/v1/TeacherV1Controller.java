package ru.kors.finalproject.controller.api.v1;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.kors.finalproject.entity.*;
import ru.kors.finalproject.repository.FileAssetRepository;
import ru.kors.finalproject.repository.SubjectOfferingRepository;
import ru.kors.finalproject.repository.TeacherRepository;
import ru.kors.finalproject.service.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/teacher")
@RequiredArgsConstructor
public class TeacherV1Controller {
    private final MobileApiAuthService mobileApiAuthService;
    private final TeacherRepository teacherRepository;
    private final SubjectOfferingRepository subjectOfferingRepository;
    private final TeacherAcademicService teacherAcademicService;
    private final AnnouncementService announcementService;
    private final GradeChangeService gradeChangeService;
    private final CourseMaterialService courseMaterialService;
    private final FileLinkService fileLinkService;
    private final FileAssetRepository fileAssetRepository;
    private final FileStorageService fileStorageService;

    @GetMapping("/profile")
    public ResponseEntity<?> profile(@RequestHeader("Authorization") String authHeader) {
        User user = mobileApiAuthService.requireRole(authHeader, User.UserRole.PROFESSOR);
        Teacher teacher = getTeacher(user);
        return ResponseEntity.ok(toTeacherProfileDto(teacher));
    }

    @PostMapping(value = "/profile-photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadProfilePhoto(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam("file") MultipartFile file) {
        User user = mobileApiAuthService.requireRole(authHeader, User.UserRole.PROFESSOR);
        Teacher teacher = getTeacher(user);
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
        teacherRepository.save(teacher);

        if (previousAsset != null) {
            fileStorageService.deleteSilently(previousAsset.getStoragePath());
            fileAssetRepository.delete(previousAsset);
        }

        return ResponseEntity.ok(toTeacherProfileDto(teacher));
    }

    @GetMapping("/sections")
    public ResponseEntity<?> sections(@RequestHeader("Authorization") String authHeader) {
        User user = mobileApiAuthService.requireRole(authHeader, User.UserRole.PROFESSOR);
        Teacher teacher = getTeacher(user);
        return ResponseEntity.ok(teacherAcademicService.getMySections(teacher).stream().map(s -> Map.of(
                "id", s.getId(),
                "subjectCode", s.getSubject().getCode(),
                "subjectName", s.getSubject().getName(),
                "semesterName", s.getSemester().getName(),
                "capacity", s.getCapacity(),
                "lessonType", s.getLessonType()
        )).toList());
    }

    @GetMapping("/sections/{sectionId}/roster")
    public ResponseEntity<?> roster(@RequestHeader("Authorization") String authHeader, @PathVariable Long sectionId) {
        User user = mobileApiAuthService.requireRole(authHeader, User.UserRole.PROFESSOR);
        Teacher teacher = getTeacher(user);
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
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long sectionId,
            @RequestBody AttendanceBody body) {
        User user = mobileApiAuthService.requireRole(authHeader, User.UserRole.PROFESSOR);
        Teacher teacher = getTeacher(user);
        teacherAcademicService.markAttendance(teacher, sectionId, LocalDate.parse(body.classDate()), body.marks());
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @GetMapping("/sections/{sectionId}/components")
    public ResponseEntity<?> components(@RequestHeader("Authorization") String authHeader, @PathVariable Long sectionId) {
        User user = mobileApiAuthService.requireRole(authHeader, User.UserRole.PROFESSOR);
        Teacher teacher = getTeacher(user);
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
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long sectionId,
            @RequestBody CreateComponentBody body) {
        User user = mobileApiAuthService.requireRole(authHeader, User.UserRole.PROFESSOR);
        Teacher teacher = getTeacher(user);
        return ResponseEntity.ok(toComponentDto(teacherAcademicService.createComponent(
                teacher, sectionId, body.name(), body.type(), body.weightPercent())));
    }

    @PostMapping("/sections/{sectionId}/components/{componentId}/publish")
    public ResponseEntity<?> publishComponent(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long sectionId,
            @PathVariable Long componentId,
            @RequestParam boolean published) {
        User user = mobileApiAuthService.requireRole(authHeader, User.UserRole.PROFESSOR);
        Teacher teacher = getTeacher(user);
        return ResponseEntity.ok(toComponentDto(
                teacherAcademicService.setComponentPublishState(teacher, sectionId, componentId, published)));
    }

    @PostMapping("/sections/{sectionId}/components/{componentId}/lock")
    public ResponseEntity<?> lockComponent(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long sectionId,
            @PathVariable Long componentId,
            @RequestParam boolean locked) {
        User user = mobileApiAuthService.requireRole(authHeader, User.UserRole.PROFESSOR);
        Teacher teacher = getTeacher(user);
        return ResponseEntity.ok(toComponentDto(
                teacherAcademicService.lockComponent(teacher, sectionId, componentId, locked)));
    }

    @PostMapping("/sections/{sectionId}/grades")
    public ResponseEntity<?> saveGrade(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long sectionId,
            @RequestBody SaveGradeBody body) {
        User user = mobileApiAuthService.requireRole(authHeader, User.UserRole.PROFESSOR);
        Teacher teacher = getTeacher(user);
        return ResponseEntity.ok(toGradeDto(teacherAcademicService.saveGrade(
                teacher, sectionId, body.studentId(), body.componentId(),
                body.gradeValue(), body.maxGradeValue(), body.comment())));
    }

    @PostMapping("/sections/{sectionId}/final-grades")
    public ResponseEntity<?> saveFinalGrade(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long sectionId,
            @RequestBody SaveFinalGradeBody body) {
        User user = mobileApiAuthService.requireRole(authHeader, User.UserRole.PROFESSOR);
        Teacher teacher = getTeacher(user);
        return ResponseEntity.ok(toFinalGradeDto(teacherAcademicService.upsertFinalGrade(
                teacher, sectionId, body.studentId(), body.numericValue(), body.letterValue(), body.points())));
    }

    @PostMapping("/sections/{sectionId}/final-grades/{studentId}/publish")
    public ResponseEntity<?> publishFinalGrade(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long sectionId,
            @PathVariable Long studentId) {
        User user = mobileApiAuthService.requireRole(authHeader, User.UserRole.PROFESSOR);
        Teacher teacher = getTeacher(user);
        return ResponseEntity.ok(toFinalGradeDto(
                teacherAcademicService.publishFinalGrade(teacher, sectionId, studentId)));
    }

    @GetMapping("/sections/{sectionId}/announcements")
    public ResponseEntity<?> announcements(@RequestHeader("Authorization") String authHeader, @PathVariable Long sectionId) {
        User user = mobileApiAuthService.requireRole(authHeader, User.UserRole.PROFESSOR);
        Teacher teacher = getTeacher(user);
        return ResponseEntity.ok(announcementService.listForSection(teacher, sectionId).stream()
                .map(this::toAnnouncementDto)
                .toList());
    }

    @PostMapping("/sections/{sectionId}/announcements")
    public ResponseEntity<?> createAnnouncement(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long sectionId,
            @RequestBody CreateAnnouncementBody body) {
        User user = mobileApiAuthService.requireRole(authHeader, User.UserRole.PROFESSOR);
        Teacher teacher = getTeacher(user);
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
    public ResponseEntity<?> materials(@RequestHeader("Authorization") String authHeader, @PathVariable Long sectionId) {
        User user = mobileApiAuthService.requireRole(authHeader, User.UserRole.PROFESSOR);
        Teacher teacher = getTeacher(user);
        return ResponseEntity.ok(courseMaterialService.listForSection(teacher, sectionId).stream().map(m -> new MaterialDto(
                m.getId(), m.getTitle(), m.getDescription(), m.getOriginalFileName(),
                m.getContentType(), m.getSizeBytes(), m.getVisibility(), m.isPublished(), m.getCreatedAt(),
                fileLinkService.createMaterialDownloadUrl(m.getId())
        )).toList());
    }

    @PostMapping(value = "/sections/{sectionId}/materials", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadMaterial(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long sectionId,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "ENROLLED_ONLY") CourseMaterial.MaterialVisibility visibility) {
        User user = mobileApiAuthService.requireRole(authHeader, User.UserRole.PROFESSOR);
        Teacher teacher = getTeacher(user);
        CourseMaterial saved = courseMaterialService.upload(
                teacher, sectionId, title, description, file, visibility);
        return ResponseEntity.ok(new MaterialDto(saved.getId(), saved.getTitle(), saved.getDescription(),
                saved.getOriginalFileName(), saved.getContentType(), saved.getSizeBytes(),
                saved.getVisibility(), saved.isPublished(), saved.getCreatedAt(),
                fileLinkService.createMaterialDownloadUrl(saved.getId())));
    }

    @PostMapping("/materials/{materialId}/visibility")
    public ResponseEntity<?> updateMaterialVisibility(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long materialId,
            @RequestParam boolean published) {
        User user = mobileApiAuthService.requireRole(authHeader, User.UserRole.PROFESSOR);
        Teacher teacher = getTeacher(user);
        CourseMaterial updated = courseMaterialService.updateVisibility(teacher, materialId, published);
        return ResponseEntity.ok(new MaterialDto(updated.getId(), updated.getTitle(), updated.getDescription(),
                updated.getOriginalFileName(), updated.getContentType(), updated.getSizeBytes(),
                updated.getVisibility(), updated.isPublished(), updated.getCreatedAt(),
                fileLinkService.createMaterialDownloadUrl(updated.getId())));
    }

    @DeleteMapping("/materials/{materialId}")
    public ResponseEntity<?> deleteMaterial(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long materialId) {
        User user = mobileApiAuthService.requireRole(authHeader, User.UserRole.PROFESSOR);
        Teacher teacher = getTeacher(user);
        courseMaterialService.delete(teacher, materialId);
        return ResponseEntity.ok(Map.of("status", "deleted"));
    }

    @GetMapping("/sections/{sectionId}/grades/export")
    public ResponseEntity<byte[]> exportGrades(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long sectionId) throws IOException {
        User user = mobileApiAuthService.requireRole(authHeader, User.UserRole.PROFESSOR);
        Teacher teacher = getTeacher(user);

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
    public ResponseEntity<?> notes(@RequestHeader("Authorization") String authHeader, @PathVariable Long sectionId) {
        User user = mobileApiAuthService.requireRole(authHeader, User.UserRole.PROFESSOR);
        Teacher teacher = getTeacher(user);
        return ResponseEntity.ok(teacherAcademicService.notesForSection(teacher, sectionId).stream()
                .map(this::toTeacherNoteDto)
                .toList());
    }

    @PostMapping("/sections/{sectionId}/student-notes")
    public ResponseEntity<?> upsertNote(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long sectionId,
            @RequestBody NoteBody body) {
        User user = mobileApiAuthService.requireRole(authHeader, User.UserRole.PROFESSOR);
        Teacher teacher = getTeacher(user);
        return ResponseEntity.ok(toTeacherNoteDto(teacherAcademicService.upsertStudentNote(
                teacher, sectionId, body.studentId(), body.note(), body.riskFlag())));
    }

    @GetMapping("/grade-change-requests")
    public ResponseEntity<?> gradeChangeRequests(@RequestHeader("Authorization") String authHeader) {
        User user = mobileApiAuthService.requireRole(authHeader, User.UserRole.PROFESSOR);
        Teacher teacher = getTeacher(user);
        return ResponseEntity.ok(gradeChangeService.listForTeacher(teacher).stream()
                .map(this::toGradeChangeRequestDto)
                .toList());
    }

    @PostMapping("/sections/{sectionId}/grade-change-requests")
    public ResponseEntity<?> createGradeChangeRequest(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long sectionId,
            @RequestBody GradeChangeBody body) {
        User user = mobileApiAuthService.requireRole(authHeader, User.UserRole.PROFESSOR);
        Teacher teacher = getTeacher(user);
        return ResponseEntity.ok(toGradeChangeRequestDto(gradeChangeService.createForComponentGrade(
                teacher, sectionId, body.gradeId(), body.newValue(), body.reason())));
    }

    @PostMapping(value = "/sections/{sectionId}/student-files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadStudentFile(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long sectionId,
            @RequestParam Long studentId,
            @RequestParam("file") MultipartFile file) {
        User user = mobileApiAuthService.requireRole(authHeader, User.UserRole.PROFESSOR);
        Teacher teacher = getTeacher(user);
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

    private Teacher getTeacher(User user) {
        return teacherRepository.findByEmailWithDetails(user.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Teacher profile not found"));
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

    public record AttendanceBody(String classDate, List<TeacherAcademicService.AttendanceMarkInput> marks) {}
    public record TeacherProfileDto(Long id, String name, String email, String department,
                                    String position, String officeHours, String officeRoom,
                                    Teacher.TeacherRole teacherRole, String faculty,
                                    String profilePhotoUrl) {}
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
