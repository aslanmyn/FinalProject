package ru.kors.finalproject.controller.api.v1;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.kors.finalproject.entity.Student;
import ru.kors.finalproject.entity.User;
import ru.kors.finalproject.service.AcademicAnalyticsService;
import ru.kors.finalproject.service.WorkflowEngineService;
import ru.kors.finalproject.web.api.v1.CurrentUserHelper;

import java.util.List;

@RestController
@RequestMapping("/api/v1/student")
@RequiredArgsConstructor
public class StudentAnalyticsV1Controller {
    private final CurrentUserHelper currentUserHelper;
    private final AcademicAnalyticsService academicAnalyticsService;
    private final WorkflowEngineService workflowEngineService;

    @GetMapping("/analytics/risk")
    public ResponseEntity<?> risk(@AuthenticationPrincipal User user) {
        Student student = currentUserHelper.requireStudent(user);
        return ResponseEntity.ok(academicAnalyticsService.buildStudentRiskDashboard(student));
    }

    @GetMapping("/planner")
    public ResponseEntity<?> planner(@AuthenticationPrincipal User user) {
        Student student = currentUserHelper.requireStudent(user);
        return ResponseEntity.ok(academicAnalyticsService.buildStudentPlannerDashboard(student));
    }

    @PostMapping("/planner/simulate")
    public ResponseEntity<?> simulate(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody PlannerSimulationBody body
    ) {
        Student student = currentUserHelper.requireStudent(user);
        return ResponseEntity.ok(academicAnalyticsService.simulateStudentPlanner(student, body.toMap()));
    }

    @GetMapping("/workflows")
    public ResponseEntity<?> workflows(@AuthenticationPrincipal User user) {
        Student student = currentUserHelper.requireStudent(user);
        return ResponseEntity.ok(workflowEngineService.buildStudentOverview(student));
    }

    public record PlannerProjectionInput(Long sectionId, Double projectedFinalScore) {
    }

    public record PlannerSimulationBody(List<PlannerProjectionInput> projectedFinals) {
        public java.util.Map<Long, Double> toMap() {
            if (projectedFinals == null) {
                return java.util.Map.of();
            }
            return projectedFinals.stream()
                    .filter(item -> item.sectionId() != null && item.projectedFinalScore() != null)
                    .collect(java.util.stream.Collectors.toMap(
                            PlannerProjectionInput::sectionId,
                            PlannerProjectionInput::projectedFinalScore,
                            (left, right) -> right,
                            java.util.LinkedHashMap::new
                    ));
        }
    }
}
