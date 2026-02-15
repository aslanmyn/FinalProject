package ru.kors.finalproject.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.kors.finalproject.entity.TeacherStudentNote;
import ru.kors.finalproject.repository.*;
import ru.kors.finalproject.entity.CourseMaterial;
import ru.kors.finalproject.service.AnnouncementService;
import ru.kors.finalproject.service.CourseMaterialService;
import ru.kors.finalproject.service.GradeChangeService;
import ru.kors.finalproject.service.NotificationService;
import ru.kors.finalproject.service.TeacherAcademicService;
import ru.kors.finalproject.service.SessionService;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Controller
@RequestMapping("/professor")
@RequiredArgsConstructor
public class ProfessorController {

    private final SessionService sessionService;
    private final SubjectOfferingRepository subjectOfferingRepository;
    private final StudentRequestRepository studentRequestRepository;
    private final AttendanceRepository attendanceRepository;
    private final GradeRepository gradeRepository;
    private final FinalGradeRepository finalGradeRepository;
    private final TeacherAcademicService teacherAcademicService;
    private final AnnouncementService announcementService;
    private final CourseMaterialService courseMaterialService;
    private final GradeChangeService gradeChangeService;
    private final NotificationService notificationService;

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        if (!sessionService.isTeacher(session)) {
            return "redirect:/login";
        }
        
        var teacher = sessionService.getCurrentTeacher(session);
        if (teacher.isEmpty()) {
            return "redirect:/login";
        }
        
        model.addAttribute("userEmail", sessionService.getEmail(session));
        model.addAttribute("fullName", sessionService.getFullName(session));
        model.addAttribute("courses", teacherAcademicService.getMySections(teacher.get()));
        model.addAttribute("requests", studentRequestRepository.findAll());
        model.addAttribute("announcements", announcementService.listForTeacher(teacher.get()));
        model.addAttribute("gradeChangeRequests", gradeChangeService.listForTeacher(teacher.get()));
        model.addAttribute("notifications", notificationService.listForEmail(teacher.get().getEmail()).stream().limit(10).toList());
        model.addAttribute("teacher", teacher.get());
        
        return "professor/dashboard";
    }

    @GetMapping("/courses")
    public String courses(HttpSession session, Model model) {
        if (!sessionService.isTeacher(session)) {
            return "redirect:/login";
        }
        var teacher = sessionService.getCurrentTeacher(session);
        if (teacher.isEmpty()) {
            return "redirect:/login";
        }
        
        model.addAttribute("userEmail", sessionService.getEmail(session));
        model.addAttribute("courses", teacherAcademicService.getMySections(teacher.get()));
        
        return "professor/courses";
    }

    @GetMapping("/course/{id}")
    public String courseDetails(
            @PathVariable Long id,
            HttpSession session,
            Model model) {
        if (!sessionService.isTeacher(session)) {
            return "redirect:/login";
        }
        var teacher = sessionService.getCurrentTeacher(session);
        if (teacher.isEmpty()) {
            return "redirect:/login";
        }
        
        var course = subjectOfferingRepository.findById(id);
        if (course.isEmpty()) {
            return "redirect:/professor/courses";
        }
        if (course.get().getTeacher() == null || !course.get().getTeacher().getId().equals(teacher.get().getId())) {
            return "redirect:/professor/courses";
        }
        
        model.addAttribute("course", course.get());
        model.addAttribute("roster", teacherAcademicService.getRoster(teacher.get(), id));
        model.addAttribute("components", teacherAcademicService.componentsForOffering(teacher.get(), id));
        model.addAttribute("grades", gradeRepository.findBySubjectOfferingId(id));
        model.addAttribute("attendanceRecords", attendanceRepository.findBySubjectOfferingIdOrderByDateDesc(id));
        model.addAttribute("finalGrades", finalGradeRepository.findBySubjectOfferingId(id));
        model.addAttribute("notes", teacherAcademicService.notesForSection(teacher.get(), id));
        model.addAttribute("announcements", announcementService.listForSection(teacher.get(), id));
        model.addAttribute("materials", courseMaterialService.listForSection(teacher.get(), id));
        model.addAttribute("userEmail", sessionService.getEmail(session));
        
        return "professor/course-details";
    }

    @PostMapping("/grade")
    public String submitGrade(
            @RequestParam Long offeringId,
            @RequestParam Long studentId,
            @RequestParam Long componentId,
            @RequestParam double gradeValue,
            @RequestParam(defaultValue = "100") double maxGradeValue,
            @RequestParam(required = false) String comment,
            HttpSession session) {
        if (!sessionService.isTeacher(session)) {
            return "redirect:/login";
        }
        var teacher = sessionService.getCurrentTeacher(session);
        if (teacher.isEmpty()) {
            return "redirect:/login";
        }
        
        teacherAcademicService.saveGrade(
                teacher.get(),
                offeringId,
                studentId,
                componentId,
                gradeValue,
                maxGradeValue,
                comment
        );
        
        return "redirect:/professor/course/" + offeringId;
    }

    @PostMapping("/component")
    public String createComponent(
            @RequestParam Long offeringId,
            @RequestParam String name,
            @RequestParam ru.kors.finalproject.entity.AssessmentComponent.ComponentType type,
            @RequestParam double weightPercent,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!sessionService.isTeacher(session)) {
            return "redirect:/login";
        }
        var teacher = sessionService.getCurrentTeacher(session);
        if (teacher.isEmpty()) {
            return "redirect:/login";
        }
        try {
            teacherAcademicService.createComponent(teacher.get(), offeringId, name, type, weightPercent);
            redirectAttributes.addFlashAttribute("componentSuccess", true);
            redirectAttributes.addFlashAttribute("componentMessage", "Component created.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("componentSuccess", false);
            redirectAttributes.addFlashAttribute("componentMessage", e.getMessage());
        }
        return "redirect:/professor/course/" + offeringId;
    }

    @PostMapping("/component/publish")
    public String publishComponent(
            @RequestParam Long offeringId,
            @RequestParam Long componentId,
            @RequestParam(defaultValue = "true") boolean published,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!sessionService.isTeacher(session)) {
            return "redirect:/login";
        }
        var teacher = sessionService.getCurrentTeacher(session);
        if (teacher.isEmpty()) {
            return "redirect:/login";
        }
        try {
            teacherAcademicService.setComponentPublishState(teacher.get(), offeringId, componentId, published);
            redirectAttributes.addFlashAttribute("componentSuccess", true);
            redirectAttributes.addFlashAttribute("componentMessage", published ? "Component published." : "Component moved to draft.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("componentSuccess", false);
            redirectAttributes.addFlashAttribute("componentMessage", e.getMessage());
        }
        return "redirect:/professor/course/" + offeringId;
    }

    @PostMapping("/component/lock")
    public String lockComponent(
            @RequestParam Long offeringId,
            @RequestParam Long componentId,
            @RequestParam(defaultValue = "true") boolean locked,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!sessionService.isTeacher(session)) {
            return "redirect:/login";
        }
        var teacher = sessionService.getCurrentTeacher(session);
        if (teacher.isEmpty()) {
            return "redirect:/login";
        }
        try {
            teacherAcademicService.lockComponent(teacher.get(), offeringId, componentId, locked);
            redirectAttributes.addFlashAttribute("componentSuccess", true);
            redirectAttributes.addFlashAttribute("componentMessage", locked ? "Component locked." : "Component unlocked.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("componentSuccess", false);
            redirectAttributes.addFlashAttribute("componentMessage", e.getMessage());
        }
        return "redirect:/professor/course/" + offeringId;
    }

    @PostMapping("/attendance")
    public String markAttendance(
            @RequestParam Long offeringId,
            @RequestParam Long studentId,
            @RequestParam String classDate,
            @RequestParam ru.kors.finalproject.entity.Attendance.AttendanceStatus status,
            @RequestParam(required = false) String reason,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!sessionService.isTeacher(session)) {
            return "redirect:/login";
        }
        var teacher = sessionService.getCurrentTeacher(session);
        if (teacher.isEmpty()) {
            return "redirect:/login";
        }
        try {
            teacherAcademicService.markAttendance(
                    teacher.get(),
                    offeringId,
                    java.time.LocalDate.parse(classDate),
                    java.util.List.of(new TeacherAcademicService.AttendanceMarkInput(studentId, status, reason))
            );
            redirectAttributes.addFlashAttribute("attendanceSuccess", true);
            redirectAttributes.addFlashAttribute("attendanceMessage", "Attendance saved.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("attendanceSuccess", false);
            redirectAttributes.addFlashAttribute("attendanceMessage", e.getMessage());
        }
        return "redirect:/professor/course/" + offeringId;
    }

    @PostMapping("/final-grade")
    public String saveFinalGrade(
            @RequestParam Long offeringId,
            @RequestParam Long studentId,
            @RequestParam double numericValue,
            @RequestParam String letterValue,
            @RequestParam double points,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!sessionService.isTeacher(session)) {
            return "redirect:/login";
        }
        var teacher = sessionService.getCurrentTeacher(session);
        if (teacher.isEmpty()) {
            return "redirect:/login";
        }
        try {
            teacherAcademicService.upsertFinalGrade(teacher.get(), offeringId, studentId, numericValue, letterValue, points);
            redirectAttributes.addFlashAttribute("finalGradeSuccess", true);
            redirectAttributes.addFlashAttribute("finalGradeMessage", "Final grade saved.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("finalGradeSuccess", false);
            redirectAttributes.addFlashAttribute("finalGradeMessage", e.getMessage());
        }
        return "redirect:/professor/course/" + offeringId;
    }

    @PostMapping("/final-grade/publish")
    public String publishFinalGrade(
            @RequestParam Long offeringId,
            @RequestParam Long studentId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!sessionService.isTeacher(session)) {
            return "redirect:/login";
        }
        var teacher = sessionService.getCurrentTeacher(session);
        if (teacher.isEmpty()) {
            return "redirect:/login";
        }
        try {
            teacherAcademicService.publishFinalGrade(teacher.get(), offeringId, studentId);
            redirectAttributes.addFlashAttribute("finalGradeSuccess", true);
            redirectAttributes.addFlashAttribute("finalGradeMessage", "Final grade published.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("finalGradeSuccess", false);
            redirectAttributes.addFlashAttribute("finalGradeMessage", e.getMessage());
        }
        return "redirect:/professor/course/" + offeringId;
    }

    @PostMapping("/announcement")
    public String createAnnouncement(
            @RequestParam(required = false) Long offeringId,
            @RequestParam String title,
            @RequestParam String content,
            @RequestParam(defaultValue = "false") boolean publicVisible,
            @RequestParam(defaultValue = "false") boolean pinned,
            @RequestParam(required = false) String scheduledAt,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!sessionService.isTeacher(session)) {
            return "redirect:/login";
        }
        var teacher = sessionService.getCurrentTeacher(session);
        if (teacher.isEmpty()) {
            return "redirect:/login";
        }
        try {
            Instant scheduleInstant = null;
            if (scheduledAt != null && !scheduledAt.isBlank()) {
                LocalDateTime localDateTime = LocalDateTime.parse(scheduledAt);
                scheduleInstant = localDateTime.atZone(ZoneId.systemDefault()).toInstant();
            }
            announcementService.createAnnouncement(
                    teacher.get(), offeringId, title, content, publicVisible, pinned, scheduleInstant
            );
            redirectAttributes.addFlashAttribute("announceSuccess", true);
            redirectAttributes.addFlashAttribute("announceMessage", "Announcement saved.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("announceSuccess", false);
            redirectAttributes.addFlashAttribute("announceMessage", e.getMessage());
        }
        return offeringId != null ? "redirect:/professor/course/" + offeringId : "redirect:/professor/dashboard";
    }

    @PostMapping("/student-note")
    public String upsertStudentNote(
            @RequestParam Long offeringId,
            @RequestParam Long studentId,
            @RequestParam String note,
            @RequestParam TeacherStudentNote.RiskFlag riskFlag,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!sessionService.isTeacher(session)) {
            return "redirect:/login";
        }
        var teacher = sessionService.getCurrentTeacher(session);
        if (teacher.isEmpty()) {
            return "redirect:/login";
        }
        try {
            teacherAcademicService.upsertStudentNote(teacher.get(), offeringId, studentId, note, riskFlag);
            redirectAttributes.addFlashAttribute("noteSuccess", true);
            redirectAttributes.addFlashAttribute("noteMessage", "Student note saved.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("noteSuccess", false);
            redirectAttributes.addFlashAttribute("noteMessage", e.getMessage());
        }
        return "redirect:/professor/course/" + offeringId;
    }

    @PostMapping("/grade-change-request")
    public String createGradeChangeRequest(
            @RequestParam Long offeringId,
            @RequestParam Long gradeId,
            @RequestParam double newValue,
            @RequestParam String reason,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!sessionService.isTeacher(session)) {
            return "redirect:/login";
        }
        var teacher = sessionService.getCurrentTeacher(session);
        if (teacher.isEmpty()) {
            return "redirect:/login";
        }
        try {
            gradeChangeService.createForComponentGrade(teacher.get(), offeringId, gradeId, newValue, reason);
            redirectAttributes.addFlashAttribute("gradeChangeSuccess", true);
            redirectAttributes.addFlashAttribute("gradeChangeMessage", "Grade change request submitted.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("gradeChangeSuccess", false);
            redirectAttributes.addFlashAttribute("gradeChangeMessage", e.getMessage());
        }
        return "redirect:/professor/course/" + offeringId;
    }

    @PostMapping("/material")
    public String uploadMaterial(
            @RequestParam Long offeringId,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam String originalFileName,
            @RequestParam String storagePath,
            @RequestParam(defaultValue = "application/octet-stream") String contentType,
            @RequestParam(defaultValue = "0") long sizeBytes,
            @RequestParam(defaultValue = "ENROLLED_ONLY") CourseMaterial.MaterialVisibility visibility,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!sessionService.isTeacher(session)) {
            return "redirect:/login";
        }
        var teacher = sessionService.getCurrentTeacher(session);
        if (teacher.isEmpty()) {
            return "redirect:/login";
        }
        try {
            courseMaterialService.upload(teacher.get(), offeringId, title, description,
                    originalFileName, storagePath, contentType, sizeBytes, visibility);
            redirectAttributes.addFlashAttribute("materialSuccess", true);
            redirectAttributes.addFlashAttribute("materialMessage", "Material uploaded.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("materialSuccess", false);
            redirectAttributes.addFlashAttribute("materialMessage", e.getMessage());
        }
        return "redirect:/professor/course/" + offeringId;
    }

    @PostMapping("/material/{id}/toggle")
    public String toggleMaterial(
            @PathVariable Long id,
            @RequestParam Long offeringId,
            @RequestParam boolean published,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!sessionService.isTeacher(session)) {
            return "redirect:/login";
        }
        var teacher = sessionService.getCurrentTeacher(session);
        if (teacher.isEmpty()) {
            return "redirect:/login";
        }
        try {
            courseMaterialService.updateVisibility(teacher.get(), id, published);
            redirectAttributes.addFlashAttribute("materialSuccess", true);
            redirectAttributes.addFlashAttribute("materialMessage", published ? "Material published." : "Material hidden.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("materialSuccess", false);
            redirectAttributes.addFlashAttribute("materialMessage", e.getMessage());
        }
        return "redirect:/professor/course/" + offeringId;
    }

    @PostMapping("/material/{id}/delete")
    public String deleteMaterial(
            @PathVariable Long id,
            @RequestParam Long offeringId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!sessionService.isTeacher(session)) {
            return "redirect:/login";
        }
        var teacher = sessionService.getCurrentTeacher(session);
        if (teacher.isEmpty()) {
            return "redirect:/login";
        }
        try {
            courseMaterialService.delete(teacher.get(), id);
            redirectAttributes.addFlashAttribute("materialSuccess", true);
            redirectAttributes.addFlashAttribute("materialMessage", "Material deleted.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("materialSuccess", false);
            redirectAttributes.addFlashAttribute("materialMessage", e.getMessage());
        }
        return "redirect:/professor/course/" + offeringId;
    }

    @PostMapping("/student-file")
    public String uploadStudentFile(
            @RequestParam Long offeringId,
            @RequestParam Long studentId,
            @RequestParam String originalName,
            @RequestParam String storagePath,
            @RequestParam(defaultValue = "application/octet-stream") String contentType,
            @RequestParam(defaultValue = "0") long sizeBytes,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!sessionService.isTeacher(session)) {
            return "redirect:/login";
        }
        var teacher = sessionService.getCurrentTeacher(session);
        if (teacher.isEmpty()) {
            return "redirect:/login";
        }

        try {
            teacherAcademicService.uploadStudentFile(
                    teacher.get(),
                    offeringId,
                    studentId,
                    originalName,
                    storagePath,
                    contentType,
                    sizeBytes
            );
            redirectAttributes.addFlashAttribute("fileUploadSuccess", true);
            redirectAttributes.addFlashAttribute("fileUploadMessage", "File added to student files.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("fileUploadSuccess", false);
            redirectAttributes.addFlashAttribute("fileUploadMessage", ex.getMessage());
        }

        return "redirect:/professor/course/" + offeringId;
    }
}
