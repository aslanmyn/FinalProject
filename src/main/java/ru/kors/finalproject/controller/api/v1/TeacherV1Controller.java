package ru.kors.finalproject.controller.api.v1;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.kors.finalproject.entity.*;
import ru.kors.finalproject.repository.TeacherRepository;
import ru.kors.finalproject.service.*;

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
    private final TeacherAcademicService teacherAcademicService;
    private final AnnouncementService announcementService;
    private final GradeChangeService gradeChangeService;
    private final CourseMaterialService courseMaterialService;
    private final RequestService requestService;

    @GetMapping("/profile")
    public ResponseEntity<?> profile(@RequestHeader("Authorization") String authHeader) {
        User user = mobileApiAuthService.requireRole(authHeader, User.UserRole.PROFESSOR);
        Teacher teacher = getTeacher(user);
        return ResponseEntity.ok(Map.of(
                "id", teacher.getId(),
                "name", teacher.getName(),
                "email", teacher.getEmail(),
                "department", teacher.getDepartment() != null ? teacher.getDepartment() : "",
                "position", teacher.getPositionTitle() != null ? teacher.getPositionTitle() : "",
                "officeHours", teacher.getOfficeHours() != null ? teacher.getOfficeHours() : "",
                "officeRoom", teacher.getOfficeRoom() != null ? teacher.getOfficeRoom() : "",
                "teacherRole", teacher.getRole()
        ));
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
        return ResponseEntity.ok(teacherAcademicService.createComponent(
                teacher, sectionId, body.name(), body.type(), body.weightPercent()));
    }

    @PostMapping("/sections/{sectionId}/components/{componentId}/publish")
    public ResponseEntity<?> publishComponent(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long sectionId,
            @PathVariable Long componentId,
            @RequestParam boolean published) {
        User user = mobileApiAuthService.requireRole(authHeader, User.UserRole.PROFESSOR);
        Teacher teacher = getTeacher(user);
        return ResponseEntity.ok(teacherAcademicService.setComponentPublishState(teacher, sectionId, componentId, published));
    }

    @PostMapping("/sections/{sectionId}/components/{componentId}/lock")
    public ResponseEntity<?> lockComponent(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long sectionId,
            @PathVariable Long componentId,
            @RequestParam boolean locked) {
        User user = mobileApiAuthService.requireRole(authHeader, User.UserRole.PROFESSOR);
        Teacher teacher = getTeacher(user);
        return ResponseEntity.ok(teacherAcademicService.lockComponent(teacher, sectionId, componentId, locked));
    }

    @PostMapping("/sections/{sectionId}/grades")
    public ResponseEntity<?> saveGrade(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long sectionId,
            @RequestBody SaveGradeBody body) {
        User user = mobileApiAuthService.requireRole(authHeader, User.UserRole.PROFESSOR);
        Teacher teacher = getTeacher(user);
        return ResponseEntity.ok(teacherAcademicService.saveGrade(
                teacher, sectionId, body.studentId(), body.componentId(),
                body.gradeValue(), body.maxGradeValue(), body.comment()));
    }

    @PostMapping("/sections/{sectionId}/final-grades")
    public ResponseEntity<?> saveFinalGrade(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long sectionId,
            @RequestBody SaveFinalGradeBody body) {
        User user = mobileApiAuthService.requireRole(authHeader, User.UserRole.PROFESSOR);
        Teacher teacher = getTeacher(user);
        return ResponseEntity.ok(teacherAcademicService.upsertFinalGrade(
                teacher, sectionId, body.studentId(), body.numericValue(), body.letterValue(), body.points()));
    }

    @PostMapping("/sections/{sectionId}/final-grades/{studentId}/publish")
    public ResponseEntity<?> publishFinalGrade(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long sectionId,
            @PathVariable Long studentId) {
        User user = mobileApiAuthService.requireRole(authHeader, User.UserRole.PROFESSOR);
        Teacher teacher = getTeacher(user);
        return ResponseEntity.ok(teacherAcademicService.publishFinalGrade(teacher, sectionId, studentId));
    }

    @GetMapping("/sections/{sectionId}/announcements")
    public ResponseEntity<?> announcements(@RequestHeader("Authorization") String authHeader, @PathVariable Long sectionId) {
        User user = mobileApiAuthService.requireRole(authHeader, User.UserRole.PROFESSOR);
        Teacher teacher = getTeacher(user);
        return ResponseEntity.ok(announcementService.listForSection(teacher, sectionId));
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
        return ResponseEntity.ok(announcementService.createAnnouncement(
                teacher, sectionId, body.title(), body.content(), body.publicVisible(), body.pinned(), scheduledAt));
    }

    @GetMapping("/sections/{sectionId}/materials")
    public ResponseEntity<?> materials(@RequestHeader("Authorization") String authHeader, @PathVariable Long sectionId) {
        User user = mobileApiAuthService.requireRole(authHeader, User.UserRole.PROFESSOR);
        Teacher teacher = getTeacher(user);
        return ResponseEntity.ok(courseMaterialService.listForSection(teacher, sectionId).stream().map(m -> new MaterialDto(
                m.getId(), m.getTitle(), m.getDescription(), m.getOriginalFileName(),
                m.getContentType(), m.getSizeBytes(), m.getVisibility(), m.isPublished(), m.getCreatedAt()
        )).toList());
    }

    @PostMapping("/sections/{sectionId}/materials")
    public ResponseEntity<?> uploadMaterial(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long sectionId,
            @RequestBody UploadMaterialBody body) {
        User user = mobileApiAuthService.requireRole(authHeader, User.UserRole.PROFESSOR);
        Teacher teacher = getTeacher(user);
        CourseMaterial saved = courseMaterialService.upload(
                teacher, sectionId, body.title(), body.description(),
                body.originalFileName(), body.storagePath(), body.contentType(),
                body.sizeBytes(), body.visibility());
        return ResponseEntity.ok(new MaterialDto(saved.getId(), saved.getTitle(), saved.getDescription(),
                saved.getOriginalFileName(), saved.getContentType(), saved.getSizeBytes(),
                saved.getVisibility(), saved.isPublished(), saved.getCreatedAt()));
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
                updated.getVisibility(), updated.isPublished(), updated.getCreatedAt()));
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

    @GetMapping("/sections/{sectionId}/student-notes")
    public ResponseEntity<?> notes(@RequestHeader("Authorization") String authHeader, @PathVariable Long sectionId) {
        User user = mobileApiAuthService.requireRole(authHeader, User.UserRole.PROFESSOR);
        Teacher teacher = getTeacher(user);
        return ResponseEntity.ok(teacherAcademicService.notesForSection(teacher, sectionId));
    }

    @PostMapping("/sections/{sectionId}/student-notes")
    public ResponseEntity<?> upsertNote(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long sectionId,
            @RequestBody NoteBody body) {
        User user = mobileApiAuthService.requireRole(authHeader, User.UserRole.PROFESSOR);
        Teacher teacher = getTeacher(user);
        return ResponseEntity.ok(teacherAcademicService.upsertStudentNote(
                teacher, sectionId, body.studentId(), body.note(), body.riskFlag()));
    }

    @GetMapping("/grade-change-requests")
    public ResponseEntity<?> gradeChangeRequests(@RequestHeader("Authorization") String authHeader) {
        User user = mobileApiAuthService.requireRole(authHeader, User.UserRole.PROFESSOR);
        Teacher teacher = getTeacher(user);
        return ResponseEntity.ok(gradeChangeService.listForTeacher(teacher));
    }

    @PostMapping("/sections/{sectionId}/grade-change-requests")
    public ResponseEntity<?> createGradeChangeRequest(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long sectionId,
            @RequestBody GradeChangeBody body) {
        User user = mobileApiAuthService.requireRole(authHeader, User.UserRole.PROFESSOR);
        Teacher teacher = getTeacher(user);
        return ResponseEntity.ok(gradeChangeService.createForComponentGrade(
                teacher, sectionId, body.gradeId(), body.newValue(), body.reason()));
    }

    private Teacher getTeacher(User user) {
        return teacherRepository.findByEmail(user.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Teacher profile not found"));
    }

    public record AttendanceBody(String classDate, List<TeacherAcademicService.AttendanceMarkInput> marks) {}
    public record CreateComponentBody(String name, AssessmentComponent.ComponentType type, double weightPercent) {}
    public record SaveGradeBody(Long studentId, Long componentId, double gradeValue, double maxGradeValue, String comment) {}
    public record SaveFinalGradeBody(Long studentId, double numericValue, String letterValue, double points) {}
    public record CreateAnnouncementBody(String title, String content, boolean publicVisible, boolean pinned, String scheduledAt) {}
    public record UploadMaterialBody(String title, String description, String originalFileName, String storagePath,
                                      String contentType, long sizeBytes, CourseMaterial.MaterialVisibility visibility) {}
    public record MaterialDto(Long id, String title, String description, String originalFileName,
                               String contentType, long sizeBytes, CourseMaterial.MaterialVisibility visibility,
                               boolean published, Instant createdAt) {}
    public record NoteBody(Long studentId, String note, TeacherStudentNote.RiskFlag riskFlag) {}
    public record GradeChangeBody(Long gradeId, double newValue, String reason) {}
}
