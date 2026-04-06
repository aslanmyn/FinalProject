package ru.kors.finalproject.controller.api.v1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Admin Analytics", description = "Admin dashboards, workflow visibility, and AI assistant for platform-wide insight.")
@SecurityRequirement(name = "Bearer")
public class AdminAnalyticsV1Controller {
    private final AcademicAnalyticsService academicAnalyticsService;
    private final WorkflowEngineService workflowEngineService;
    private final AdminAssistantService adminAssistantService;

    @GetMapping("/analytics")
    @PreAuthorize("hasAuthority('PERM_SUPER')")
    @Operation(summary = "Get admin analytics dashboard", description = "Returns global analytics such as faculty risk, overloaded sections, request load, and critical students.")
    public ResponseEntity<?> analytics(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(academicAnalyticsService.buildAdminAnalyticsDashboard());
    }

    @GetMapping("/workflows")
    @PreAuthorize("hasAuthority('PERM_SUPER')")
    @Operation(summary = "Get admin workflow overview", description = "Returns admin-visible workflow queue and summary metrics.")
    public ResponseEntity<?> workflows(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(workflowEngineService.buildAdminOverview());
    }

    @GetMapping("/workflows/{type}/{id}/timeline")
    @PreAuthorize("hasAuthority('PERM_SUPER')")
    @Operation(summary = "Get workflow timeline", description = "Returns the timeline for a concrete workflow entity.")
    public ResponseEntity<?> workflowTimeline(
            @AuthenticationPrincipal User user,
            @PathVariable WorkflowEngineService.WorkflowType type,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(workflowEngineService.buildTimeline(type, id));
    }

    @PostMapping("/assistant/chat")
    @PreAuthorize("hasAuthority('PERM_SUPER')")
    @Operation(summary = "Ask admin assistant", description = "Sends an operational question to the admin AI assistant. The assistant is read-only and uses backend-built context.")
    public ResponseEntity<?> assistantChat(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody AssistantQuestionBody body
    ) {
        return ResponseEntity.ok(adminAssistantService.ask(user, body.message()));
    }

    public record AssistantQuestionBody(@Schema(example = "Which sections are overloaded right now?") @NotBlank String message) {
    }
}
