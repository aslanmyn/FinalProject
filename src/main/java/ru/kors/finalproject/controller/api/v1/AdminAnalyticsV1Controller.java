package ru.kors.finalproject.controller.api.v1;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.kors.finalproject.entity.User;
import ru.kors.finalproject.service.*;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Validated
public class AdminAnalyticsV1Controller {
    private final MobileApiAuthService mobileApiAuthService;
    private final AcademicAnalyticsService academicAnalyticsService;
    private final WorkflowEngineService workflowEngineService;
    private final AdminAssistantService adminAssistantService;

    @GetMapping("/analytics")
    public ResponseEntity<?> analytics(@RequestHeader("Authorization") String authHeader) {
        mobileApiAuthService.requireRole(authHeader, User.UserRole.ADMIN);
        return ResponseEntity.ok(academicAnalyticsService.buildAdminAnalyticsDashboard());
    }

    @GetMapping("/workflows")
    public ResponseEntity<?> workflows(@RequestHeader("Authorization") String authHeader) {
        mobileApiAuthService.requireRole(authHeader, User.UserRole.ADMIN);
        return ResponseEntity.ok(workflowEngineService.buildAdminOverview());
    }

    @GetMapping("/workflows/{type}/{id}/timeline")
    public ResponseEntity<?> workflowTimeline(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable WorkflowEngineService.WorkflowType type,
            @PathVariable Long id
    ) {
        mobileApiAuthService.requireRole(authHeader, User.UserRole.ADMIN);
        return ResponseEntity.ok(workflowEngineService.buildTimeline(type, id));
    }

    @PostMapping("/assistant/chat")
    public ResponseEntity<?> assistantChat(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody AssistantQuestionBody body
    ) {
        User admin = mobileApiAuthService.requireRole(authHeader, User.UserRole.ADMIN);
        return ResponseEntity.ok(adminAssistantService.ask(admin, body.message()));
    }

    public record AssistantQuestionBody(@NotBlank String message) {
    }
}
