package ru.kors.finalproject.controller.api.v1;

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
public class StudentAssistantV1Controller {
    private final CurrentUserHelper currentUserHelper;
    private final StudentAssistantService studentAssistantService;

    @PostMapping("/chat")
    public ResponseEntity<?> chat(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody AssistantQuestionBody body) {
        Student student = currentUserHelper.requireStudent(user);
        return ResponseEntity.ok(studentAssistantService.ask(student, body.message()));
    }

    public record AssistantQuestionBody(@NotBlank String message) {
    }
}
