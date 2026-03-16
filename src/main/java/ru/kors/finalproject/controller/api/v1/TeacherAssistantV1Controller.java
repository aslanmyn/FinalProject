package ru.kors.finalproject.controller.api.v1;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.kors.finalproject.entity.Teacher;
import ru.kors.finalproject.entity.User;
import ru.kors.finalproject.repository.TeacherRepository;
import ru.kors.finalproject.service.MobileApiAuthService;
import ru.kors.finalproject.service.TeacherAssistantService;

@RestController
@RequestMapping("/api/v1/teacher/assistant")
@RequiredArgsConstructor
@Validated
public class TeacherAssistantV1Controller {
    private final MobileApiAuthService mobileApiAuthService;
    private final TeacherRepository teacherRepository;
    private final TeacherAssistantService teacherAssistantService;

    @PostMapping("/chat")
    public ResponseEntity<?> chat(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody AssistantQuestionBody body) {
        User user = mobileApiAuthService.requireRole(authHeader, User.UserRole.PROFESSOR);
        Teacher teacher = teacherRepository.findByEmailWithDetails(user.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Teacher profile not found"));
        return ResponseEntity.ok(teacherAssistantService.ask(user, teacher, body.message()));
    }

    public record AssistantQuestionBody(@NotBlank String message) {
    }
}
