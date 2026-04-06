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
import ru.kors.finalproject.entity.Student;
import ru.kors.finalproject.entity.User;
import ru.kors.finalproject.service.StudentAssistantService;
import ru.kors.finalproject.web.api.v1.CurrentUserHelper;

@RestController
@RequestMapping("/api/v1/student/assistant")
@RequiredArgsConstructor
@Validated
@Tag(name = "Student Assistant", description = "AI assistant endpoints for student-specific questions and planning.")
@SecurityRequirement(name = "Bearer")
public class StudentAssistantV1Controller {
    private final CurrentUserHelper currentUserHelper;
    private final StudentAssistantService studentAssistantService;

    @PostMapping("/chat")
    @Operation(summary = "Ask student assistant", description = "Sends a natural-language question to the student assistant. The assistant uses backend-provided student context and remains read-only.")
    public ResponseEntity<?> chat(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody AssistantQuestionBody body) {
        Student student = currentUserHelper.requireStudent(user);
        return ResponseEntity.ok(studentAssistantService.ask(student, body.message()));
    }

    public record AssistantQuestionBody(@Schema(example = "How much do I need on the final in Calculus II to finish with a B?") @NotBlank String message) {
    }
}
