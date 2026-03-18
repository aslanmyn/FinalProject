package ru.kors.finalproject.controller.api.v1;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.kors.finalproject.entity.Teacher;
import ru.kors.finalproject.entity.User;
import ru.kors.finalproject.repository.TeacherRepository;
import ru.kors.finalproject.service.AcademicAnalyticsService;
import ru.kors.finalproject.service.MobileApiAuthService;

@RestController
@RequestMapping("/api/v1/teacher")
@RequiredArgsConstructor
public class TeacherAnalyticsV1Controller {
    private final MobileApiAuthService mobileApiAuthService;
    private final TeacherRepository teacherRepository;
    private final AcademicAnalyticsService academicAnalyticsService;

    @GetMapping("/analytics/risk")
    public ResponseEntity<?> risk(@RequestHeader("Authorization") String authHeader) {
        User user = mobileApiAuthService.requireRole(authHeader, User.UserRole.PROFESSOR);
        Teacher teacher = teacherRepository.findByEmailWithDetails(user.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Teacher profile not found"));
        return ResponseEntity.ok(academicAnalyticsService.buildTeacherRiskDashboard(teacher));
    }
}
