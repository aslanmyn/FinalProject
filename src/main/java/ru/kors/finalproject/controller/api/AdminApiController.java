package ru.kors.finalproject.controller.api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.kors.finalproject.entity.*;
import ru.kors.finalproject.repository.*;
import ru.kors.finalproject.service.AddDropService;
import ru.kors.finalproject.service.FinancialService;
import ru.kors.finalproject.service.GradeChangeService;
import ru.kors.finalproject.service.RequestService;
import ru.kors.finalproject.service.SessionService;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminApiController {

    private final SessionService sessionService;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final SemesterRepository semesterRepository;
    private final SubjectRepository subjectRepository;
    private final SubjectOfferingRepository subjectOfferingRepository;
    private final MeetingTimeRepository meetingTimeRepository;
    private final RegistrationWindowRepository registrationWindowRepository;
    private final AddDropService addDropService;
    private final FinancialService financialService;
    private final GradeChangeService gradeChangeService;
    private final StudentRequestRepository studentRequestRepository;
    private final RequestService requestService;
    private final NewsRepository newsRepository;
    private final ExamScheduleRepository examScheduleRepository;
    private final UserRepository userRepository;

    @GetMapping("/students")
    public ResponseEntity<?> getAllStudents(jakarta.servlet.http.HttpSession session) {
        if (!hasPermission(session, User.AdminPermission.REGISTRAR, User.AdminPermission.SUPPORT)) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(studentRepository.findAll());
    }

    @GetMapping("/students/{id}")
    public ResponseEntity<?> getStudent(@PathVariable Long id, jakarta.servlet.http.HttpSession session) {
        if (!hasPermission(session, User.AdminPermission.REGISTRAR, User.AdminPermission.SUPPORT)) return ResponseEntity.status(403).build();
        return studentRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/students/{id}/status")
    public ResponseEntity<?> updateStudentStatus(
            @PathVariable Long id,
            @RequestParam Student.StudentStatus status,
            jakarta.servlet.http.HttpSession session) {
        if (!hasPermission(session, User.AdminPermission.REGISTRAR)) return ResponseEntity.status(403).build();
        var student = studentRepository.findById(id);
        if (student.isEmpty()) return ResponseEntity.notFound().build();
        
        Student s = student.get();
        s.setStatus(status);
        studentRepository.save(s);
        return ResponseEntity.ok("Status updated");
    }

    @PostMapping("/setup/term")
    public ResponseEntity<?> createTerm(
            @RequestParam String name,
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(defaultValue = "false") boolean current,
            jakarta.servlet.http.HttpSession session) {
        if (!hasPermission(session, User.AdminPermission.REGISTRAR)) return ResponseEntity.status(403).build();
        if (current) {
            semesterRepository.findAll().forEach(s -> {
                if (s.isCurrent()) {
                    s.setCurrent(false);
                    semesterRepository.save(s);
                }
            });
        }
        Semester semester = Semester.builder()
                .name(name)
                .startDate(LocalDate.parse(startDate))
                .endDate(LocalDate.parse(endDate))
                .current(current)
                .build();
        return ResponseEntity.ok(semesterRepository.save(semester));
    }

    @PostMapping("/setup/section")
    public ResponseEntity<?> createSection(
            @RequestParam Long subjectId,
            @RequestParam Long semesterId,
            @RequestParam(required = false) Long teacherId,
            @RequestParam int capacity,
            @RequestParam(required = false) SubjectOffering.LessonType lessonType,
            jakarta.servlet.http.HttpSession session) {
        if (!hasPermission(session, User.AdminPermission.REGISTRAR)) return ResponseEntity.status(403).build();
        Subject subject = subjectRepository.findById(subjectId).orElseThrow();
        Semester semester = semesterRepository.findById(semesterId).orElseThrow();
        Teacher teacher = teacherId != null ? teacherRepository.findById(teacherId).orElse(null) : null;

        SubjectOffering offering = SubjectOffering.builder()
                .subject(subject)
                .semester(semester)
                .teacher(teacher)
                .capacity(capacity)
                .lessonType(lessonType != null ? lessonType : SubjectOffering.LessonType.LECTURE)
                .build();
        return ResponseEntity.ok(subjectOfferingRepository.save(offering));
    }

    @PostMapping("/setup/section/{id}/meeting-time")
    public ResponseEntity<?> addMeetingTime(
            @PathVariable Long id,
            @RequestParam java.time.DayOfWeek dayOfWeek,
            @RequestParam String startTime,
            @RequestParam String endTime,
            @RequestParam String room,
            @RequestParam SubjectOffering.LessonType lessonType,
            jakarta.servlet.http.HttpSession session) {
        if (!hasPermission(session, User.AdminPermission.REGISTRAR)) return ResponseEntity.status(403).build();
        SubjectOffering offering = subjectOfferingRepository.findById(id).orElseThrow();
        MeetingTime meetingTime = MeetingTime.builder()
                .subjectOffering(offering)
                .dayOfWeek(dayOfWeek)
                .startTime(java.time.LocalTime.parse(startTime))
                .endTime(java.time.LocalTime.parse(endTime))
                .room(room)
                .lessonType(lessonType)
                .build();
        return ResponseEntity.ok(meetingTimeRepository.save(meetingTime));
    }

    @PostMapping("/windows")
    public ResponseEntity<?> upsertWindow(
            @RequestParam Long semesterId,
            @RequestParam RegistrationWindow.WindowType type,
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(defaultValue = "true") boolean active,
            jakarta.servlet.http.HttpSession session) {
        if (!hasPermission(session, User.AdminPermission.REGISTRAR)) return ResponseEntity.status(403).build();
        Semester semester = semesterRepository.findById(semesterId).orElseThrow();
        RegistrationWindow window = registrationWindowRepository.findBySemesterIdAndTypeAndActiveTrue(semesterId, type)
                .orElseGet(() -> RegistrationWindow.builder()
                        .semester(semester)
                        .type(type)
                        .build());
        window.setStartDate(LocalDate.parse(startDate));
        window.setEndDate(LocalDate.parse(endDate));
        window.setActive(active);
        return ResponseEntity.ok(registrationWindowRepository.save(window));
    }

    @PostMapping("/enrollment/override")
    public ResponseEntity<?> overrideEnrollment(
            @RequestParam Long studentId,
            @RequestParam Long subjectOfferingId,
            jakarta.servlet.http.HttpSession session) {
        if (!hasPermission(session, User.AdminPermission.REGISTRAR)) return ResponseEntity.status(403).build();
        Student student = studentRepository.findById(studentId).orElseThrow();
        return ResponseEntity.ok(addDropService.adminOverrideEnroll(student, subjectOfferingId));
    }

    @PostMapping("/finance/invoice")
    public ResponseEntity<?> createInvoice(
            @RequestParam Long studentId,
            @RequestParam BigDecimal amount,
            @RequestParam String description,
            @RequestParam String dueDate,
            jakarta.servlet.http.HttpSession session) {
        if (!hasPermission(session, User.AdminPermission.FINANCE)) return ResponseEntity.status(403).build();
        Student student = studentRepository.findById(studentId).orElseThrow();
        return ResponseEntity.ok(financialService.createInvoice(student, amount, description, LocalDate.parse(dueDate)));
    }

    @PostMapping("/finance/payment")
    public ResponseEntity<?> registerPayment(
            @RequestParam Long studentId,
            @RequestParam Long chargeId,
            @RequestParam BigDecimal amount,
            @RequestParam(required = false) String paymentDate,
            jakarta.servlet.http.HttpSession session) {
        if (!hasPermission(session, User.AdminPermission.FINANCE)) return ResponseEntity.status(403).build();
        Student student = studentRepository.findById(studentId).orElseThrow();
        LocalDate date = paymentDate != null ? LocalDate.parse(paymentDate) : LocalDate.now();
        return ResponseEntity.ok(financialService.registerPayment(student, chargeId, amount, date));
    }

    @GetMapping("/requests")
    public ResponseEntity<?> requests(jakarta.servlet.http.HttpSession session) {
        if (!hasPermission(session, User.AdminPermission.SUPPORT)) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(studentRequestRepository.findAll());
    }

    @PostMapping("/requests/{id}/assign")
    public ResponseEntity<?> assignRequest(
            @PathVariable Long id,
            @RequestParam Long userId,
            jakarta.servlet.http.HttpSession session) {
        var actor = sessionService.getCurrentUser(session);
        if (!hasPermission(session, User.AdminPermission.SUPPORT) || actor.isEmpty()) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(requestService.assign(id, userId, actor.get()));
    }

    @PostMapping("/requests/{id}/status")
    public ResponseEntity<?> requestStatus(
            @PathVariable Long id,
            @RequestParam StudentRequest.RequestStatus status,
            jakarta.servlet.http.HttpSession session) {
        var actor = sessionService.getCurrentUser(session);
        if (!hasPermission(session, User.AdminPermission.SUPPORT) || actor.isEmpty()) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(requestService.updateStatus(id, status, actor.get()));
    }

    @PostMapping("/news")
    public ResponseEntity<?> createNews(
            @RequestParam String title,
            @RequestParam String content,
            @RequestParam(defaultValue = "General") String category,
            jakarta.servlet.http.HttpSession session) {
        if (!hasPermission(session, User.AdminPermission.CONTENT)) return ResponseEntity.status(403).build();
        News news = News.builder()
                .title(title)
                .content(content)
                .category(category)
                .createdAt(Instant.now())
                .build();
        return ResponseEntity.ok(newsRepository.save(news));
    }

    @PostMapping("/exams")
    public ResponseEntity<?> createExam(
            @RequestParam Long subjectOfferingId,
            @RequestParam String examDate,
            @RequestParam String examTime,
            @RequestParam String room,
            @RequestParam(defaultValue = "OFFLINE") String format,
            jakarta.servlet.http.HttpSession session) {
        if (!hasPermission(session, User.AdminPermission.REGISTRAR)) return ResponseEntity.status(403).build();
        SubjectOffering offering = subjectOfferingRepository.findById(subjectOfferingId).orElseThrow();
        ExamSchedule exam = ExamSchedule.builder()
                .subjectOffering(offering)
                .examDate(LocalDate.parse(examDate))
                .examTime(java.time.LocalTime.parse(examTime))
                .room(room)
                .format(format)
                .build();
        return ResponseEntity.ok(examScheduleRepository.save(exam));
    }

    @GetMapping("/grade-change-requests")
    public ResponseEntity<?> gradeChangeRequests(jakarta.servlet.http.HttpSession session) {
        if (!hasPermission(session, User.AdminPermission.REGISTRAR)) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(gradeChangeService.listPending());
    }

    @PostMapping("/grade-change-requests/{id}/review")
    public ResponseEntity<?> reviewGradeChangeRequest(
            @PathVariable Long id,
            @RequestParam boolean approve,
            @RequestParam(required = false) String reviewerComment,
            jakarta.servlet.http.HttpSession session) {
        if (!hasPermission(session, User.AdminPermission.REGISTRAR)) return ResponseEntity.status(403).build();
        var currentUser = sessionService.getCurrentUser(session);
        if (currentUser.isEmpty()) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(gradeChangeService.review(id, approve, reviewerComment, currentUser.get()));
    }

    @PostMapping("/users/{id}/permissions")
    public ResponseEntity<?> setUserPermissions(
            @PathVariable Long id,
            @RequestParam List<User.AdminPermission> permissions,
            jakarta.servlet.http.HttpSession session) {
        if (!hasPermission(session, User.AdminPermission.SUPER)) return ResponseEntity.status(403).build();
        User user = userRepository.findById(id).orElseThrow();
        if (user.getRole() != User.UserRole.ADMIN) {
            return ResponseEntity.badRequest().body("Target user is not ADMIN");
        }
        if (permissions == null || permissions.isEmpty()) {
            user.setAdminPermissions(java.util.EnumSet.noneOf(User.AdminPermission.class));
        } else {
            user.setAdminPermissions(java.util.EnumSet.copyOf(permissions));
        }
        userRepository.save(user);
        return ResponseEntity.ok(user);
    }

    @PostMapping("/teachers/{id}/role")
    public ResponseEntity<?> setTeacherRole(
            @PathVariable Long id,
            @RequestParam Teacher.TeacherRole role,
            jakarta.servlet.http.HttpSession session) {
        if (!hasPermission(session, User.AdminPermission.REGISTRAR, User.AdminPermission.SUPER)) return ResponseEntity.status(403).build();
        Teacher teacher = teacherRepository.findById(id).orElseThrow();
        teacher.setRole(role);
        return ResponseEntity.ok(teacherRepository.save(teacher));
    }

    @GetMapping("/statistics")
    public ResponseEntity<?> getStatistics(jakarta.servlet.http.HttpSession session) {
        if (!sessionService.isAdmin(session)) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(Map.of(
                "totalStudents", studentRepository.count(),
                "totalTeachers", teacherRepository.count(),
                "totalSections", subjectOfferingRepository.count(),
                "totalRequests", studentRequestRepository.count()
        ));
    }

    private boolean hasPermission(jakarta.servlet.http.HttpSession session, User.AdminPermission... permissions) {
        if (!sessionService.isAdmin(session)) {
            return false;
        }
        for (User.AdminPermission permission : permissions) {
            if (sessionService.hasAdminPermission(session, permission)) {
                return true;
            }
        }
        return false;
    }
}
