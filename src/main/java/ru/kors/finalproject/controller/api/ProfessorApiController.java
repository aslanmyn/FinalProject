package ru.kors.finalproject.controller.api;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.kors.finalproject.entity.*;
import ru.kors.finalproject.repository.*;
import ru.kors.finalproject.service.AnnouncementService;
import ru.kors.finalproject.service.GradeChangeService;
import ru.kors.finalproject.service.RequestService;
import ru.kors.finalproject.service.SessionService;
import ru.kors.finalproject.service.TeacherAcademicService;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@RestController
@RequestMapping("/api/professor")
@RequiredArgsConstructor
public class ProfessorApiController {

    private final SessionService sessionService;
    private final AttendanceRepository attendanceRepository;
    private final StudentRequestRepository studentRequestRepository;
    private final AnnouncementService announcementService;
    private final GradeChangeService gradeChangeService;
    private final RequestService requestService;
    private final TeacherAcademicService teacherAcademicService;

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(HttpSession session) {
        var teacher = sessionService.getCurrentTeacher(session);
        if (teacher.isEmpty()) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(teacher.get());
    }

    @GetMapping("/courses")
    public ResponseEntity<?> getCourses(HttpSession session) {
        var teacher = sessionService.getCurrentTeacher(session);
        if (teacher.isEmpty()) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(teacherAcademicService.getMySections(teacher.get()));
    }

    @GetMapping("/sections/{offeringId}/roster")
    public ResponseEntity<?> getRoster(
            @PathVariable Long offeringId,
            HttpSession session) {
        var teacher = sessionService.getCurrentTeacher(session);
        if (teacher.isEmpty()) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(teacherAcademicService.getRoster(teacher.get(), offeringId));
    }

    @PostMapping("/sections/{offeringId}/attendance/session")
    public ResponseEntity<?> createAttendanceSession(
            @PathVariable Long offeringId,
            @RequestParam String classDate,
            HttpSession session) {
        var teacher = sessionService.getCurrentTeacher(session);
        if (teacher.isEmpty()) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(
                teacherAcademicService.createOrGetAttendanceSession(
                        teacher.get(),
                        offeringId,
                        LocalDate.parse(classDate))
        );
    }

    @PostMapping("/sections/{offeringId}/attendance/mark")
    public ResponseEntity<?> markAttendance(
            @PathVariable Long offeringId,
            @RequestParam String classDate,
            @RequestBody List<TeacherAcademicService.AttendanceMarkInput> marks,
            HttpSession session) {
        var teacher = sessionService.getCurrentTeacher(session);
        if (teacher.isEmpty()) return ResponseEntity.status(401).build();
        teacherAcademicService.markAttendance(teacher.get(), offeringId, LocalDate.parse(classDate), marks);
        return ResponseEntity.ok("Attendance saved");
    }

    @GetMapping("/sections/{offeringId}/components")
    public ResponseEntity<?> components(@PathVariable Long offeringId, HttpSession session) {
        var teacher = sessionService.getCurrentTeacher(session);
        if (teacher.isEmpty()) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(teacherAcademicService.componentsForOffering(teacher.get(), offeringId));
    }

    @GetMapping("/sections/{offeringId}/announcements")
    public ResponseEntity<?> sectionAnnouncements(@PathVariable Long offeringId, HttpSession session) {
        var teacher = sessionService.getCurrentTeacher(session);
        if (teacher.isEmpty()) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(announcementService.listForSection(teacher.get(), offeringId));
    }

    @PostMapping("/sections/{offeringId}/announcements")
    public ResponseEntity<?> createAnnouncement(
            @PathVariable Long offeringId,
            @RequestParam String title,
            @RequestParam String content,
            @RequestParam(defaultValue = "false") boolean publicVisible,
            @RequestParam(defaultValue = "false") boolean pinned,
            @RequestParam(required = false) String scheduledAt,
            HttpSession session) {
        var teacher = sessionService.getCurrentTeacher(session);
        if (teacher.isEmpty()) return ResponseEntity.status(401).build();
        Instant instant = null;
        if (scheduledAt != null && !scheduledAt.isBlank()) {
            try {
                instant = Instant.parse(scheduledAt);
            } catch (Exception ex) {
                LocalDateTime localDateTime = LocalDateTime.parse(scheduledAt);
                instant = localDateTime.atZone(ZoneId.systemDefault()).toInstant();
            }
        }
        return ResponseEntity.ok(announcementService.createAnnouncement(
                teacher.get(), offeringId, title, content, publicVisible, pinned, instant
        ));
    }

    @PostMapping("/sections/{offeringId}/components")
    public ResponseEntity<?> createComponent(
            @PathVariable Long offeringId,
            @RequestParam String name,
            @RequestParam AssessmentComponent.ComponentType type,
            @RequestParam double weightPercent,
            HttpSession session) {
        var teacher = sessionService.getCurrentTeacher(session);
        if (teacher.isEmpty()) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(
                teacherAcademicService.createComponent(teacher.get(), offeringId, name, type, weightPercent)
        );
    }

    @PostMapping("/sections/{offeringId}/components/{componentId}/publish")
    public ResponseEntity<?> publishComponent(
            @PathVariable Long offeringId,
            @PathVariable Long componentId,
            @RequestParam boolean published,
            HttpSession session) {
        var teacher = sessionService.getCurrentTeacher(session);
        if (teacher.isEmpty()) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(
                teacherAcademicService.setComponentPublishState(teacher.get(), offeringId, componentId, published)
        );
    }

    @PostMapping("/sections/{offeringId}/components/{componentId}/lock")
    public ResponseEntity<?> lockComponent(
            @PathVariable Long offeringId,
            @PathVariable Long componentId,
            @RequestParam boolean locked,
            HttpSession session) {
        var teacher = sessionService.getCurrentTeacher(session);
        if (teacher.isEmpty()) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(
                teacherAcademicService.lockComponent(teacher.get(), offeringId, componentId, locked)
        );
    }

    @PostMapping("/sections/{offeringId}/grades")
    public ResponseEntity<?> submitGrade(
            @PathVariable Long offeringId,
            @RequestParam Long studentId,
            @RequestParam Long componentId,
            @RequestParam double gradeValue,
            @RequestParam double maxGradeValue,
            @RequestParam(required = false) String comment,
            HttpSession session) {
        var teacher = sessionService.getCurrentTeacher(session);
        if (teacher.isEmpty()) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(teacherAcademicService.saveGrade(
                teacher.get(), offeringId, studentId, componentId, gradeValue, maxGradeValue, comment
        ));
    }

    @PostMapping("/sections/{offeringId}/final-grades")
    public ResponseEntity<?> upsertFinalGrade(
            @PathVariable Long offeringId,
            @RequestParam Long studentId,
            @RequestParam double numericValue,
            @RequestParam(required = false) String letterValue,
            @RequestParam double points,
            HttpSession session) {
        var teacher = sessionService.getCurrentTeacher(session);
        if (teacher.isEmpty()) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(teacherAcademicService.upsertFinalGrade(
                teacher.get(), offeringId, studentId, numericValue, letterValue, points
        ));
    }

    @PostMapping("/sections/{offeringId}/final-grades/{studentId}/publish")
    public ResponseEntity<?> publishFinalGrade(
            @PathVariable Long offeringId,
            @PathVariable Long studentId,
            HttpSession session) {
        var teacher = sessionService.getCurrentTeacher(session);
        if (teacher.isEmpty()) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(teacherAcademicService.publishFinalGrade(teacher.get(), offeringId, studentId));
    }

    @PostMapping("/sections/{offeringId}/student-notes")
    public ResponseEntity<?> upsertStudentNote(
            @PathVariable Long offeringId,
            @RequestParam Long studentId,
            @RequestParam String note,
            @RequestParam TeacherStudentNote.RiskFlag riskFlag,
            HttpSession session) {
        var teacher = sessionService.getCurrentTeacher(session);
        if (teacher.isEmpty()) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(teacherAcademicService.upsertStudentNote(teacher.get(), offeringId, studentId, note, riskFlag));
    }

    @GetMapping("/sections/{offeringId}/student-notes")
    public ResponseEntity<?> listStudentNotes(@PathVariable Long offeringId, HttpSession session) {
        var teacher = sessionService.getCurrentTeacher(session);
        if (teacher.isEmpty()) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(teacherAcademicService.notesForSection(teacher.get(), offeringId));
    }

    @PostMapping("/sections/{offeringId}/grade-change-requests")
    public ResponseEntity<?> createGradeChangeRequest(
            @PathVariable Long offeringId,
            @RequestParam Long gradeId,
            @RequestParam double newValue,
            @RequestParam String reason,
            HttpSession session) {
        var teacher = sessionService.getCurrentTeacher(session);
        if (teacher.isEmpty()) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(gradeChangeService.createForComponentGrade(teacher.get(), offeringId, gradeId, newValue, reason));
    }

    @GetMapping("/grade-change-requests")
    public ResponseEntity<?> listGradeChangeRequests(HttpSession session) {
        var teacher = sessionService.getCurrentTeacher(session);
        if (teacher.isEmpty()) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(gradeChangeService.listForTeacher(teacher.get()));
    }

    @PostMapping("/sections/{offeringId}/student-files")
    public ResponseEntity<?> uploadStudentFile(
            @PathVariable Long offeringId,
            @RequestParam Long studentId,
            @RequestParam String originalName,
            @RequestParam String storagePath,
            @RequestParam(defaultValue = "application/octet-stream") String contentType,
            @RequestParam(defaultValue = "0") long sizeBytes,
            HttpSession session) {
        var teacher = sessionService.getCurrentTeacher(session);
        if (teacher.isEmpty()) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(teacherAcademicService.uploadStudentFile(
                teacher.get(),
                offeringId,
                studentId,
                originalName,
                storagePath,
                contentType,
                sizeBytes
        ));
    }

    @GetMapping("/student-requests")
    public ResponseEntity<?> getStudentRequests(HttpSession session) {
        var teacher = sessionService.getCurrentTeacher(session);
        if (teacher.isEmpty()) return ResponseEntity.status(401).build();

        var requests = studentRequestRepository.findAll();
        return ResponseEntity.ok(requests);
    }

    @PostMapping("/approve-request")
    public ResponseEntity<?> approveRequest(
            @RequestParam Long requestId,
            HttpSession session) {
        var teacher = sessionService.getCurrentTeacher(session);
        var user = sessionService.getCurrentUser(session);
        if (teacher.isEmpty() || user.isEmpty()) return ResponseEntity.status(401).build();

        return ResponseEntity.ok(
                requestService.updateStatus(requestId, StudentRequest.RequestStatus.APPROVED, user.get())
        );
    }

    @PostMapping("/request-message")
    public ResponseEntity<?> addRequestMessage(
            @RequestParam Long requestId,
            @RequestParam String message,
            HttpSession session) {
        var teacher = sessionService.getCurrentTeacher(session);
        var user = sessionService.getCurrentUser(session);
        if (teacher.isEmpty() || user.isEmpty()) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(requestService.addMessage(requestId, user.get(), message));
    }
}
