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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.kors.finalproject.entity.Student;
import ru.kors.finalproject.entity.User;
import ru.kors.finalproject.service.PsychSupportAssistantService;
import ru.kors.finalproject.web.api.v1.CurrentUserHelper;

@RestController
@RequestMapping("/api/v1/student/support/assistant")
@RequiredArgsConstructor
@Validated
@Tag(name = "Student Support Assistant", description = "Wellbeing and psychological support assistant endpoints for students.")
@SecurityRequirement(name = "Bearer")
public class PsychSupportAssistantV1Controller {
    private final CurrentUserHelper currentUserHelper;
    private final PsychSupportAssistantService psychSupportAssistantService;

    @PostMapping("/chat")
    @Operation(
            summary = "Ask student support assistant",
            description = "Sends a natural-language wellbeing or emotional-support question to the student support AI assistant. The assistant is read-only, does not diagnose, and can escalate crisis responses."
    )
    public ResponseEntity<?> chat(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody AssistantQuestionBody body
    ) {
        Student student = currentUserHelper.requireStudent(user);
        return ResponseEntity.ok(psychSupportAssistantService.ask(student, body.message()));
    }

    public record AssistantQuestionBody(
            @Schema(example = "Мне тревожно и тяжело сосредоточиться, что можно сделать прямо сейчас?")
            @NotBlank String message
    ) {
    }
}
