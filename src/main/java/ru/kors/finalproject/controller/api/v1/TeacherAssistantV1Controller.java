package ru.kors.finalproject.controller.api.v1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.kors.finalproject.entity.Teacher;
import ru.kors.finalproject.entity.User;
import ru.kors.finalproject.service.TeacherAssistantService;
import ru.kors.finalproject.web.api.v1.CurrentUserHelper;

@RestController
@RequestMapping("/api/v1/teacher/assistant")
@RequiredArgsConstructor
@Validated
@Tag(name = "Teacher Assistant", description = "AI assistant endpoints for teacher questions about sections, students, and operational workload.")
@SecurityRequirement(name = "Bearer")
public class TeacherAssistantV1Controller {
    private final CurrentUserHelper currentUserHelper;
    private final TeacherAssistantService teacherAssistantService;

    @PostMapping("/chat")
    @Operation(summary = "Ask teacher assistant", description = "Sends a natural-language question to the teacher assistant using teacher-specific context.")
    public ResponseEntity<?> chat(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody AssistantQuestionBody body) {
        Teacher teacher = currentUserHelper.requireTeacher(user);
        return ResponseEntity.ok(teacherAssistantService.ask(user, teacher, body.message()));
    }

    public record AssistantQuestionBody(@Schema(example = "Show me students at risk in my current sections") @NotBlank String message) {
    }
}
