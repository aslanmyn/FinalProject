package ru.kors.finalproject.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.kors.finalproject.entity.*;
import ru.kors.finalproject.repository.*;
import ru.kors.finalproject.service.*;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final SessionService sessionService;
    private final AdminAcademicService adminAcademicService;
    private final HoldService holdService;
    private final ExamScheduleService examScheduleService;
    private final FinancialService financialService;
    private final GradeChangeService gradeChangeService;
    private final MobilityService mobilityService;
    private final ClearanceService clearanceService;
    private final SurveyService surveyService;
    private final ChecklistService checklistService;
    private final RequestService requestService;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final SemesterRepository semesterRepository;
    private final SubjectOfferingRepository subjectOfferingRepository;
    private final NewsRepository newsRepository;
    private final AuditLogRepository auditLogRepository;
    private final StudentRequestRepository studentRequestRepository;

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        if (!requireAdmin(session)) return "redirect:/login";
        model.addAttribute("userEmail", sessionService.getEmail(session));
        model.addAttribute("totalStudents", studentRepository.count());
        model.addAttribute("totalSections", subjectOfferingRepository.count());
        model.addAttribute("pendingGradeChanges", gradeChangeService.listPending());
        model.addAttribute("activeHoldsCount", holdService.listAllActiveHolds().size());
        model.addAttribute("pendingRequests", studentRequestRepository.findByStatusOrderByCreatedAtDesc(
                StudentRequest.RequestStatus.NEW));
        model.addAttribute("currentUser", sessionService.getCurrentUser(session).orElse(null));
        return "admin/dashboard";
    }

    @GetMapping("/academic")
    public String academic(HttpSession session, Model model) {
        if (!requireAdminPermission(session, User.AdminPermission.REGISTRAR)) return "redirect:/login";
        model.addAttribute("userEmail", sessionService.getEmail(session));
        model.addAttribute("terms", adminAcademicService.listTerms());
        model.addAttribute("subjects", adminAcademicService.listSubjects());
        model.addAttribute("teachers", adminAcademicService.listTeachers());
        Long currentSemId = semesterRepository.findByCurrentTrue().map(Semester::getId).orElse(null);
        model.addAttribute("currentSemesterId", currentSemId);
        model.addAttribute("sections", currentSemId != null
                ? adminAcademicService.listSections(currentSemId) : List.of());
        return "admin/academic";
    }

    @PostMapping("/academic/term")
    public String createTerm(@RequestParam String name, @RequestParam String startDate,
                              @RequestParam String endDate, @RequestParam(defaultValue = "false") boolean current,
                              HttpSession session, RedirectAttributes ra) {
        if (!requireAdminPermission(session, User.AdminPermission.REGISTRAR)) return "redirect:/login";
        User actor = sessionService.getCurrentUser(session).orElseThrow();
        adminAcademicService.createTerm(name, LocalDate.parse(startDate), LocalDate.parse(endDate), current, actor);
        ra.addFlashAttribute("success", "Term created");
        return "redirect:/admin/academic";
    }

    @PostMapping("/academic/section")
    public String createSection(@RequestParam Long subjectId, @RequestParam Long semesterId,
                                 @RequestParam(required = false) Long teacherId, @RequestParam int capacity,
                                 @RequestParam SubjectOffering.LessonType lessonType,
                                 HttpSession session, RedirectAttributes ra) {
        if (!requireAdminPermission(session, User.AdminPermission.REGISTRAR)) return "redirect:/login";
        User actor = sessionService.getCurrentUser(session).orElseThrow();
        adminAcademicService.createSection(subjectId, semesterId, teacherId, capacity, lessonType, actor);
        ra.addFlashAttribute("success", "Section created");
        return "redirect:/admin/academic";
    }

    @PostMapping("/academic/meeting-time")
    public String addMeetingTime(@RequestParam Long sectionId, @RequestParam DayOfWeek dayOfWeek,
                                  @RequestParam String startTime, @RequestParam String endTime,
                                  @RequestParam String room, @RequestParam SubjectOffering.LessonType lessonType,
                                  HttpSession session, RedirectAttributes ra) {
        if (!requireAdminPermission(session, User.AdminPermission.REGISTRAR)) return "redirect:/login";
        User actor = sessionService.getCurrentUser(session).orElseThrow();
        try {
            adminAcademicService.addMeetingTime(sectionId, dayOfWeek, LocalTime.parse(startTime),
                    LocalTime.parse(endTime), room, lessonType, actor);
            ra.addFlashAttribute("success", "Meeting time added");
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/academic";
    }

    @PostMapping("/academic/window")
    public String upsertWindow(@RequestParam Long semesterId, @RequestParam RegistrationWindow.WindowType type,
                                @RequestParam String startDate, @RequestParam String endDate,
                                @RequestParam(defaultValue = "true") boolean active,
                                HttpSession session, RedirectAttributes ra) {
        if (!requireAdminPermission(session, User.AdminPermission.REGISTRAR)) return "redirect:/login";
        User actor = sessionService.getCurrentUser(session).orElseThrow();
        adminAcademicService.upsertWindow(semesterId, type, LocalDate.parse(startDate),
                LocalDate.parse(endDate), active, actor);
        ra.addFlashAttribute("success", "Registration window updated");
        return "redirect:/admin/academic";
    }

    @PostMapping("/academic/override-enroll")
    public String overrideEnroll(@RequestParam Long studentId, @RequestParam Long sectionId,
                                  @RequestParam String reason, HttpSession session, RedirectAttributes ra) {
        if (!requireAdminPermission(session, User.AdminPermission.REGISTRAR)) return "redirect:/login";
        User actor = sessionService.getCurrentUser(session).orElseThrow();
        try {
            adminAcademicService.adminOverrideEnroll(studentId, sectionId, reason, actor);
            ra.addFlashAttribute("success", "Enrollment override applied");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/academic";
    }

    @GetMapping("/finance")
    public String finance(HttpSession session, Model model) {
        if (!requireAdminPermission(session, User.AdminPermission.FINANCE)) return "redirect:/login";
        model.addAttribute("userEmail", sessionService.getEmail(session));
        model.addAttribute("students", studentRepository.findAll());
        model.addAttribute("activeHolds", holdService.listAllActiveHolds());
        return "admin/finance";
    }

    @PostMapping("/finance/invoice")
    public String createInvoice(@RequestParam Long studentId, @RequestParam BigDecimal amount,
                                 @RequestParam String description, @RequestParam String dueDate,
                                 HttpSession session, RedirectAttributes ra) {
        if (!requireAdminPermission(session, User.AdminPermission.FINANCE)) return "redirect:/login";
        Student student = studentRepository.findById(studentId).orElseThrow();
        financialService.createInvoice(student, amount, description, LocalDate.parse(dueDate));
        ra.addFlashAttribute("success", "Invoice created");
        return "redirect:/admin/finance";
    }

    @PostMapping("/finance/payment")
    public String registerPayment(@RequestParam Long studentId, @RequestParam Long chargeId,
                                   @RequestParam BigDecimal amount, HttpSession session, RedirectAttributes ra) {
        if (!requireAdminPermission(session, User.AdminPermission.FINANCE)) return "redirect:/login";
        Student student = studentRepository.findById(studentId).orElseThrow();
        financialService.registerPayment(student, chargeId, amount, LocalDate.now());
        ra.addFlashAttribute("success", "Payment registered");
        return "redirect:/admin/finance";
    }

    @GetMapping("/holds")
    public String holds(HttpSession session, Model model) {
        if (!requireAdminPermission(session, User.AdminPermission.FINANCE)) return "redirect:/login";
        model.addAttribute("userEmail", sessionService.getEmail(session));
        model.addAttribute("activeHolds", holdService.listAllActiveHolds());
        model.addAttribute("students", studentRepository.findAll());
        return "admin/holds";
    }

    @PostMapping("/holds/create")
    public String createHold(@RequestParam Long studentId, @RequestParam Hold.HoldType type,
                              @RequestParam String reason, HttpSession session, RedirectAttributes ra) {
        if (!requireAdminPermission(session, User.AdminPermission.FINANCE)) return "redirect:/login";
        User actor = sessionService.getCurrentUser(session).orElseThrow();
        try {
            holdService.createHold(studentId, type, reason, actor);
            ra.addFlashAttribute("success", "Hold created");
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/holds";
    }

    @PostMapping("/holds/{id}/remove")
    public String removeHold(@PathVariable Long id, @RequestParam String removalReason,
                              HttpSession session, RedirectAttributes ra) {
        if (!requireAdminPermission(session, User.AdminPermission.FINANCE)) return "redirect:/login";
        User actor = sessionService.getCurrentUser(session).orElseThrow();
        holdService.removeHold(id, removalReason, actor);
        ra.addFlashAttribute("success", "Hold removed");
        return "redirect:/admin/holds";
    }

    @GetMapping("/exams")
    public String exams(HttpSession session, Model model) {
        if (!requireAdminPermission(session, User.AdminPermission.REGISTRAR)) return "redirect:/login";
        model.addAttribute("userEmail", sessionService.getEmail(session));
        Long semId = semesterRepository.findByCurrentTrue().map(Semester::getId).orElse(null);
        model.addAttribute("exams", semId != null ? examScheduleService.listBySemester(semId) : List.of());
        model.addAttribute("sections", semId != null
                ? adminAcademicService.listSections(semId) : List.of());
        return "admin/exams";
    }

    @PostMapping("/exams/create")
    public String createExam(@RequestParam Long sectionId, @RequestParam String examDate,
                              @RequestParam String examTime, @RequestParam String room,
                              @RequestParam String format, HttpSession session, RedirectAttributes ra) {
        if (!requireAdminPermission(session, User.AdminPermission.REGISTRAR)) return "redirect:/login";
        User actor = sessionService.getCurrentUser(session).orElseThrow();
        try {
            examScheduleService.createExamSession(sectionId, LocalDate.parse(examDate),
                    LocalTime.parse(examTime), room, format, actor);
            ra.addFlashAttribute("success", "Exam scheduled");
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/exams";
    }

    @PostMapping("/exams/{id}/delete")
    public String deleteExam(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        if (!requireAdminPermission(session, User.AdminPermission.REGISTRAR)) return "redirect:/login";
        User actor = sessionService.getCurrentUser(session).orElseThrow();
        examScheduleService.deleteExamSession(id, actor);
        ra.addFlashAttribute("success", "Exam deleted");
        return "redirect:/admin/exams";
    }

    @GetMapping("/requests")
    public String requests(HttpSession session, Model model) {
        if (!requireAdminPermission(session, User.AdminPermission.SUPPORT)) return "redirect:/login";
        model.addAttribute("userEmail", sessionService.getEmail(session));
        model.addAttribute("requests", studentRequestRepository.findAll());
        model.addAttribute("admins", userRepository.findAll().stream()
                .filter(u -> u.getRole() == User.UserRole.ADMIN).toList());
        return "admin/requests";
    }

    @PostMapping("/requests/{id}/assign")
    public String assignRequest(@PathVariable Long id, @RequestParam Long userId,
                                 HttpSession session, RedirectAttributes ra) {
        if (!requireAdminPermission(session, User.AdminPermission.SUPPORT)) return "redirect:/login";
        User actor = sessionService.getCurrentUser(session).orElseThrow();
        requestService.assign(id, userId, actor);
        ra.addFlashAttribute("success", "Request assigned");
        return "redirect:/admin/requests";
    }

    @PostMapping("/requests/{id}/status")
    public String updateRequestStatus(@PathVariable Long id, @RequestParam StudentRequest.RequestStatus status,
                                       HttpSession session, RedirectAttributes ra) {
        if (!requireAdminPermission(session, User.AdminPermission.SUPPORT)) return "redirect:/login";
        User actor = sessionService.getCurrentUser(session).orElseThrow();
        requestService.updateStatus(id, status, actor);
        ra.addFlashAttribute("success", "Request status updated");
        return "redirect:/admin/requests";
    }

    @GetMapping("/content")
    public String content(HttpSession session, Model model) {
        if (!requireAdminPermission(session, User.AdminPermission.CONTENT)) return "redirect:/login";
        model.addAttribute("userEmail", sessionService.getEmail(session));
        model.addAttribute("newsList", newsRepository.findByOrderByCreatedAtDesc());
        model.addAttribute("surveys", surveyService.listAll());
        return "admin/content";
    }

    @PostMapping("/content/news")
    public String createNews(@RequestParam String title, @RequestParam String content,
                              @RequestParam(required = false) String category,
                              HttpSession session, RedirectAttributes ra) {
        if (!requireAdminPermission(session, User.AdminPermission.CONTENT)) return "redirect:/login";
        News news = News.builder()
                .title(title)
                .content(content)
                .category(category)
                .createdAt(java.time.Instant.now())
                .build();
        newsRepository.save(news);
        ra.addFlashAttribute("success", "News created");
        return "redirect:/admin/content";
    }

    @PostMapping("/content/survey")
    public String createSurvey(@RequestParam String title, @RequestParam String startDate,
                                @RequestParam String endDate, @RequestParam(defaultValue = "false") boolean anonymous,
                                HttpSession session, RedirectAttributes ra) {
        if (!requireAdminPermission(session, User.AdminPermission.CONTENT)) return "redirect:/login";
        User actor = sessionService.getCurrentUser(session).orElseThrow();
        surveyService.create(title, LocalDate.parse(startDate), LocalDate.parse(endDate),
                anonymous, null, null, actor);
        ra.addFlashAttribute("success", "Survey created");
        return "redirect:/admin/content";
    }

    @PostMapping("/content/survey/{id}/close")
    public String closeSurvey(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        if (!requireAdminPermission(session, User.AdminPermission.CONTENT)) return "redirect:/login";
        User actor = sessionService.getCurrentUser(session).orElseThrow();
        surveyService.closeSurvey(id, actor);
        ra.addFlashAttribute("success", "Survey closed");
        return "redirect:/admin/content";
    }

    @GetMapping("/mobility")
    public String mobility(HttpSession session, Model model) {
        if (!requireAdminPermission(session, User.AdminPermission.MOBILITY)) return "redirect:/login";
        model.addAttribute("userEmail", sessionService.getEmail(session));
        model.addAttribute("applications", mobilityService.listAll());
        return "admin/mobility";
    }

    @PostMapping("/mobility/{id}/status")
    public String updateMobilityStatus(@PathVariable Long id,
                                        @RequestParam MobilityApplication.MobilityStatus status,
                                        HttpSession session, RedirectAttributes ra) {
        if (!requireAdminPermission(session, User.AdminPermission.MOBILITY)) return "redirect:/login";
        User actor = sessionService.getCurrentUser(session).orElseThrow();
        try {
            mobilityService.updateStatus(id, status, actor);
            ra.addFlashAttribute("success", "Status updated");
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/mobility";
    }

    @GetMapping("/clearance")
    public String clearance(HttpSession session, Model model) {
        if (!requireAdminPermission(session, User.AdminPermission.MOBILITY)) return "redirect:/login";
        model.addAttribute("userEmail", sessionService.getEmail(session));
        model.addAttribute("sheets", clearanceService.listAll());
        return "admin/clearance";
    }

    @PostMapping("/clearance/checkpoint/{id}/review")
    public String reviewCheckpoint(@PathVariable Long id, @RequestParam boolean approve,
                                    @RequestParam(required = false) String comment,
                                    HttpSession session, RedirectAttributes ra) {
        if (!requireAdminPermission(session, User.AdminPermission.MOBILITY)) return "redirect:/login";
        User actor = sessionService.getCurrentUser(session).orElseThrow();
        clearanceService.reviewCheckpoint(id, approve, comment, actor);
        ra.addFlashAttribute("success", approve ? "Checkpoint approved" : "Checkpoint rejected");
        return "redirect:/admin/clearance";
    }

    @GetMapping("/grade-changes")
    public String gradeChanges(HttpSession session, Model model) {
        if (!requireAdminPermission(session, User.AdminPermission.REGISTRAR)) return "redirect:/login";
        model.addAttribute("userEmail", sessionService.getEmail(session));
        model.addAttribute("pendingGradeChanges", gradeChangeService.listPending());
        return "admin/grade-changes";
    }

    @PostMapping("/grade-change/{id}/review")
    public String reviewGradeChange(@PathVariable Long id, @RequestParam boolean approve,
                                     @RequestParam(required = false) String comment,
                                     HttpSession session, RedirectAttributes ra) {
        if (!requireAdminPermission(session, User.AdminPermission.REGISTRAR)) return "redirect:/login";
        User actor = sessionService.getCurrentUser(session).orElseThrow();
        gradeChangeService.review(id, approve, comment, actor);
        ra.addFlashAttribute("success", approve ? "Grade change approved" : "Grade change rejected");
        return "redirect:/admin/grade-changes";
    }

    @PostMapping("/students/{id}/status")
    public String updateStudentStatus(@PathVariable Long id, @RequestParam Student.StudentStatus status,
                                       HttpSession session, RedirectAttributes ra) {
        if (!requireAdmin(session)) return "redirect:/login";
        studentRepository.findById(id).ifPresent(s -> {
            s.setStatus(status);
            studentRepository.save(s);
        });
        ra.addFlashAttribute("success", "Student status updated");
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/audit")
    public String auditLogs(HttpSession session, Model model) {
        if (!requireAdminPermission(session, User.AdminPermission.SUPER)) return "redirect:/login";
        model.addAttribute("userEmail", sessionService.getEmail(session));
        model.addAttribute("logs", auditLogRepository.findAll().stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .limit(200)
                .toList());
        return "admin/audit";
    }

    @GetMapping("/users")
    public String users(HttpSession session, Model model) {
        if (!requireAdminPermission(session, User.AdminPermission.SUPER)) return "redirect:/login";
        model.addAttribute("userEmail", sessionService.getEmail(session));
        model.addAttribute("users", userRepository.findAll());
        return "admin/users";
    }

    @PostMapping("/users/{id}/permissions")
    public String setPermissions(@PathVariable Long id, @RequestParam(required = false) List<User.AdminPermission> permissions,
                                  HttpSession session, RedirectAttributes ra) {
        if (!requireAdminPermission(session, User.AdminPermission.SUPER)) return "redirect:/login";
        User user = userRepository.findById(id).orElseThrow();
        if (user.getRole() != User.UserRole.ADMIN) {
            ra.addFlashAttribute("error", "User is not admin");
            return "redirect:/admin/users";
        }
        if (permissions == null || permissions.isEmpty()) {
            user.setAdminPermissions(java.util.EnumSet.noneOf(User.AdminPermission.class));
        } else {
            user.setAdminPermissions(java.util.EnumSet.copyOf(permissions));
        }
        userRepository.save(user);
        ra.addFlashAttribute("success", "Permissions updated");
        return "redirect:/admin/users";
    }

    private boolean requireAdmin(HttpSession session) {
        return sessionService.isAdmin(session);
    }

    private boolean requireAdminPermission(HttpSession session, User.AdminPermission permission) {
        if (!sessionService.isAdmin(session)) return false;
        return sessionService.hasAdminPermission(session, permission);
    }
}
