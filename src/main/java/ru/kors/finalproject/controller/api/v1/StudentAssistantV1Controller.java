package ru.kors.finalproject.controller.api.v1;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.kors.finalproject.entity.Student;
import ru.kors.finalproject.entity.User;
import ru.kors.finalproject.repository.StudentRepository;
import ru.kors.finalproject.service.MobileApiAuthService;
import ru.kors.finalproject.service.StudentAssistantService;

@RestController
@RequestMapping("/api/v1/student/assistant")
@RequiredArgsConstructor
@Validated
public class StudentAssistantV1Controller {
    private final MobileApiAuthService mobileApiAuthService;
    private final StudentRepository studentRepository;
    private final StudentAssistantService studentAssistantService;

    @PostMapping("/chat")
    public ResponseEntity<?> chat(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody AssistantQuestionBody body) {
        User user = mobileApiAuthService.requireRole(authHeader, User.UserRole.STUDENT);
        Student student = studentRepository.findByEmailWithDetails(user.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Student profile not found"));
        return ResponseEntity.ok(studentAssistantService.ask(student, body.message()));
    }

    public record AssistantQuestionBody(@NotBlank String message) {
    }
}
