package ru.kors.finalproject.controller.api.v1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.kors.finalproject.entity.Teacher;
import ru.kors.finalproject.entity.User;
import ru.kors.finalproject.service.AcademicAnalyticsService;
import ru.kors.finalproject.web.api.v1.CurrentUserHelper;

@RestController
@RequestMapping("/api/v1/teacher")
@RequiredArgsConstructor
@Tag(name = "Teacher Analytics", description = "Teacher-facing risk and section analytics.")
@SecurityRequirement(name = "Bearer")
public class TeacherAnalyticsV1Controller {
    private final CurrentUserHelper currentUserHelper;
    private final AcademicAnalyticsService academicAnalyticsService;

    @GetMapping("/analytics/risk")
    @Operation(summary = "Get teacher risk dashboard", description = "Returns section-level student risk insights for the current teacher.")
    public ResponseEntity<?> risk(@AuthenticationPrincipal User user) {
        Teacher teacher = currentUserHelper.requireTeacher(user);
        return ResponseEntity.ok(academicAnalyticsService.buildTeacherRiskDashboard(teacher));
    }
}
