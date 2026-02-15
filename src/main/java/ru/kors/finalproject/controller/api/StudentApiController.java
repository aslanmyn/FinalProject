package ru.kors.finalproject.controller.api;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.kors.finalproject.entity.*;
import ru.kors.finalproject.repository.*;
import ru.kors.finalproject.service.AddDropService;
import ru.kors.finalproject.service.NotificationService;
import ru.kors.finalproject.service.RequestService;
import ru.kors.finalproject.service.SessionService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
public class StudentApiController {

    private final SessionService sessionService;
    private final StudentRepository studentRepository;
    private final GradeRepository gradeRepository;
    private final FinalGradeRepository finalGradeRepository;
    private final RegistrationRepository registrationRepository;
    private final AttendanceRepository attendanceRepository;
    private final MeetingTimeRepository meetingTimeRepository;
    private final HoldRepository holdRepository;
    private final FileAssetRepository fileAssetRepository;
    private final NewsRepository newsRepository;
    private final ExamScheduleRepository examScheduleRepository;
    private final ChargeRepository chargeRepository;
    private final PaymentRepository paymentRepository;
    private final StudentRequestRepository studentRequestRepository;
    private final AddDropService addDropService;
    private final NotificationService notificationService;
    private final RequestService requestService;
    private final SubjectOfferingRepository subjectOfferingRepository;

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(HttpSession session) {
        var student = sessionService.getCurrentStudent(session);
        if (student.isEmpty()) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(student.get());
    }

    @GetMapping("/notifications")
    public ResponseEntity<?> getNotifications(HttpSession session) {
        var student = sessionService.getCurrentStudent(session);
        if (student.isEmpty()) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(Map.of(
                "unread", notificationService.unreadCount(student.get().getEmail()),
                "items", notificationService.listForEmail(student.get().getEmail())
        ));
    }

    @GetMapping("/course-registration/available")
    public ResponseEntity<?> availableForRegistration(HttpSession session) {
        var student = sessionService.getCurrentStudent(session);
        if (student.isEmpty()) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(addDropService.getAvailableForAdd(student.get()));
    }

    @PostMapping("/course-registration/submit")
    public ResponseEntity<?> submitRegistration(@RequestParam Long subjectOfferingId, HttpSession session) {
        var student = sessionService.getCurrentStudent(session);
        if (student.isEmpty()) return ResponseEntity.status(401).build();
        var result = addDropService.registerCourse(student.get(), subjectOfferingId);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/add-drop/add")
    public ResponseEntity<?> addCourse(@RequestParam Long subjectOfferingId, HttpSession session) {
        var student = sessionService.getCurrentStudent(session);
        if (student.isEmpty()) return ResponseEntity.status(401).build();
        var result = addDropService.addCourse(student.get(), subjectOfferingId);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/add-drop/drop")
    public ResponseEntity<?> dropCourse(
            @RequestParam Long subjectOfferingId,
            @RequestParam(required = false) String reason,
            HttpSession session) {
        var student = sessionService.getCurrentStudent(session);
        if (student.isEmpty()) return ResponseEntity.status(401).build();
        var result = addDropService.dropCourse(student.get(), subjectOfferingId, reason);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/enrollments")
    public ResponseEntity<?> getEnrollments(HttpSession session) {
        var student = sessionService.getCurrentStudent(session);
        if (student.isEmpty()) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(addDropService.registrationsForStudent(student.get()));
    }

    @GetMapping("/transcript")
    public ResponseEntity<?> getTranscript(HttpSession session) {
        var student = sessionService.getCurrentStudent(session);
        if (student.isEmpty()) return ResponseEntity.status(401).build();

        Student s = student.get();
        var finalGrades = finalGradeRepository.findByStudentIdAndPublishedTrue(s.getId());
        
        Map<String, Object> response = new HashMap<>();
        response.put("student", s.getName());
        response.put("email", s.getEmail());
        response.put("gpa", calculateGPA(finalGrades));
        response.put("finalGrades", finalGrades);
        response.put("creditsEarned", s.getCreditsEarned());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/journal")
    public ResponseEntity<?> getJournal(HttpSession session) {
        var student = sessionService.getCurrentStudent(session);
        if (student.isEmpty()) return ResponseEntity.status(401).build();
        List<Grade> grades = gradeRepository.findByStudentId(student.get().getId()).stream()
                .filter(Grade::isPublished)
                .toList();
        return ResponseEntity.ok(grades);
    }

    @GetMapping("/assessment-results")
    public ResponseEntity<?> getAssessmentResults(HttpSession session) {
        var student = sessionService.getCurrentStudent(session);
        if (student.isEmpty()) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(finalGradeRepository.findByStudentIdAndPublishedTrue(student.get().getId()));
    }

    @GetMapping("/schedule")
    public ResponseEntity<?> getSchedule(HttpSession session) {
        var student = sessionService.getCurrentStudent(session);
        if (student.isEmpty()) return ResponseEntity.status(401).build();

        Student s = student.get();
        var registrations = registrationRepository.findActiveByStudentIdWithDetails(s.getId());
        var schedule = registrations.stream().map(r -> {
            List<MeetingTime> meetingTimes = meetingTimeRepository.findBySubjectOfferingId(r.getSubjectOffering().getId());
            if (meetingTimes.isEmpty() && r.getSubjectOffering().getDayOfWeek() != null) {
                MeetingTime fallback = MeetingTime.builder()
                        .subjectOffering(r.getSubjectOffering())
                        .dayOfWeek(r.getSubjectOffering().getDayOfWeek())
                        .startTime(r.getSubjectOffering().getStartTime())
                        .endTime(r.getSubjectOffering().getEndTime())
                        .room(r.getSubjectOffering().getRoom())
                        .lessonType(r.getSubjectOffering().getLessonType())
                        .build();
                meetingTimes = List.of(fallback);
            }
            return Map.of(
                    "enrollmentId", r.getId(),
                    "status", r.getStatus(),
                    "subjectCode", r.getSubjectOffering().getSubject().getCode(),
                    "subjectName", r.getSubjectOffering().getSubject().getName(),
                    "sectionId", r.getSubjectOffering().getId(),
                    "meetingTimes", meetingTimes
            );
        }).toList();
        
        return ResponseEntity.ok(schedule);
    }

    @GetMapping("/attendance")
    public ResponseEntity<?> getAttendance(HttpSession session) {
        var student = sessionService.getCurrentStudent(session);
        if (student.isEmpty()) return ResponseEntity.status(401).build();

        Student s = student.get();
        var attendance = attendanceRepository.findByStudentId(s.getId());
        long present = attendance.stream().filter(a -> a.getStatus() == Attendance.AttendanceStatus.PRESENT).count();
        long total = attendance.size();
        double percentage = total == 0 ? 0.0 : (present * 100.0 / total);
        Map<String, Object> response = Map.of(
                "records", attendance,
                "summary", Map.of(
                        "total", total,
                        "present", present,
                        "percentage", percentage,
                        "warning", percentage < 70.0
                )
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/financial/account")
    public ResponseEntity<?> getFinancialAccount(HttpSession session) {
        var student = sessionService.getCurrentStudent(session);
        if (student.isEmpty()) return ResponseEntity.status(401).build();
        Student s = student.get();
        return ResponseEntity.ok(Map.of(
                "charges", chargeRepository.findByStudentIdOrderByDueDateDesc(s.getId()),
                "payments", paymentRepository.findByStudentIdOrderByDateDesc(s.getId()),
                "holds", holdRepository.findByStudentIdAndActiveTrue(s.getId()),
                "balance", chargeRepository.findByStudentIdOrderByDueDateDesc(s.getId()).stream()
                        .map(Charge::getAmount).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add)
                        .subtract(paymentRepository.findByStudentIdOrderByDateDesc(s.getId()).stream()
                                .map(Payment::getAmount).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add))
        ));
    }

    @GetMapping("/requests")
    public ResponseEntity<?> getRequests(HttpSession session) {
        var student = sessionService.getCurrentStudent(session);
        if (student.isEmpty()) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(studentRequestRepository.findByStudentIdOrderByCreatedAtDesc(student.get().getId()));
    }

    @PostMapping("/requests")
    public ResponseEntity<?> createRequest(
            @RequestParam String category,
            @RequestParam String description,
            HttpSession session) {
        var student = sessionService.getCurrentStudent(session);
        if (student.isEmpty()) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(requestService.createRequest(student.get(), category, description));
    }

    @GetMapping("/requests/{id}/messages")
    public ResponseEntity<?> requestMessages(@PathVariable Long id, HttpSession session) {
        var student = sessionService.getCurrentStudent(session);
        if (student.isEmpty()) return ResponseEntity.status(401).build();
        var req = studentRequestRepository.findById(id);
        if (req.isEmpty() || !req.get().getStudent().getId().equals(student.get().getId())) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(Map.of(
                "messages", requestService.getMessages(id),
                "attachments", requestService.getAttachments(id)
        ));
    }

    @PostMapping("/requests/{id}/messages")
    public ResponseEntity<?> addRequestMessage(
            @PathVariable Long id,
            @RequestParam String message,
            HttpSession session) {
        var student = sessionService.getCurrentStudent(session);
        var user = sessionService.getCurrentUser(session);
        if (student.isEmpty() || user.isEmpty()) return ResponseEntity.status(401).build();
        var req = studentRequestRepository.findById(id);
        if (req.isEmpty() || !req.get().getStudent().getId().equals(student.get().getId())) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(requestService.addMessage(id, user.get(), message));
    }

    @GetMapping("/files")
    public ResponseEntity<?> getFiles(HttpSession session) {
        var student = sessionService.getCurrentStudent(session);
        if (student.isEmpty()) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(fileAssetRepository.findByOwnerStudentIdOrderByUploadedAtDesc(student.get().getId()));
    }

    @GetMapping("/exam-schedule")
    public ResponseEntity<?> examSchedule(HttpSession session) {
        var student = sessionService.getCurrentStudent(session);
        if (student.isEmpty()) return ResponseEntity.status(401).build();
        var offeringIds = registrationRepository.findActiveByStudentIdWithDetails(student.get().getId()).stream()
                .map(r -> r.getSubjectOffering().getId()).toList();
        var exams = examScheduleRepository.findAll().stream()
                .filter(exam -> offeringIds.contains(exam.getSubjectOffering().getId()))
                .toList();
        return ResponseEntity.ok(exams);
    }

    @GetMapping("/news")
    public ResponseEntity<?> getNews() {
        return ResponseEntity.ok(newsRepository.findByOrderByCreatedAtDesc());
    }

    private double calculateGPA(java.util.List<FinalGrade> grades) {
        if (grades.isEmpty()) return 0.0;
        
        double sum = grades.stream().mapToDouble(FinalGrade::getPoints).sum();
        return sum / grades.size();
    }
}
