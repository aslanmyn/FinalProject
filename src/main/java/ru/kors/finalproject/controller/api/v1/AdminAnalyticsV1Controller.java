package ru.kors.finalproject.controller.api.v1;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.kors.finalproject.entity.User;
import ru.kors.finalproject.service.*;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Validated
public class AdminAnalyticsV1Controller {
    private final AcademicAnalyticsService academicAnalyticsService;
    private final WorkflowEngineService workflowEngineService;
    private final AdminAssistantService adminAssistantService;

    @GetMapping("/analytics")
    @PreAuthorize("hasAuthority('PERM_SUPER')")
    public ResponseEntity<?> analytics(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(academicAnalyticsService.buildAdminAnalyticsDashboard());
    }

    @GetMapping("/workflows")
    @PreAuthorize("hasAuthority('PERM_SUPER')")
    public ResponseEntity<?> workflows(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(workflowEngineService.buildAdminOverview());
    }

    @GetMapping("/workflows/{type}/{id}/timeline")
    @PreAuthorize("hasAuthority('PERM_SUPER')")
    public ResponseEntity<?> workflowTimeline(
            @AuthenticationPrincipal User user,
            @PathVariable WorkflowEngineService.WorkflowType type,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(workflowEngineService.buildTimeline(type, id));
    }

    @PostMapping("/assistant/chat")
    @PreAuthorize("hasAuthority('PERM_SUPER')")
    public ResponseEntity<?> assistantChat(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody AssistantQuestionBody body
    ) {
        return ResponseEntity.ok(adminAssistantService.ask(user, body.message()));
    }

    public record AssistantQuestionBody(@NotBlank String message) {
    }
}
