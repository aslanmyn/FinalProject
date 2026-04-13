package ru.kors.finalproject.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.kors.finalproject.entity.Program;
import ru.kors.finalproject.entity.Student;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PsychSupportAssistantServiceTest {

    @Mock
    private GeminiClientService geminiClientService;
    @Mock
    private AuditService auditService;

    private PsychSupportAssistantService service;
    private Student student;

    @BeforeEach
    void setUp() {
        service = new PsychSupportAssistantService(
                geminiClientService,
                auditService
        );

        Program program = Program.builder()
                .id(10L)
                .name("Computer Science")
                .build();

        student = Student.builder()
                .id(101L)
                .email("a_student@kbtu.kz")
                .name("Aruzhan Student")
                .program(program)
                .course(2)
                .build();
    }

    @Test
    @DisplayName("high-risk messages return immediate safety response without Gemini")
    void ask_highRiskMessage_returnsCrisisReply() {
        PsychSupportAssistantService.PsychSupportReply reply = service.ask(
                student,
                "Я хочу умереть и причинить себе вред"
        );

        assertThat(reply.riskLevel()).isEqualTo("HIGH");
        assertThat(reply.needsHumanFollowUp()).isTrue();
        assertThat(reply.model()).isEqualTo("safety-crisis-guard");
        assertThat(reply.recommendedResources()).anyMatch(item -> item.contains("support@kbtu.kz"));
        assertThat(reply.recommendedResources()).anyMatch(item -> item.contains("112/911"));

        verify(geminiClientService, never())
                .generateJson(anyString(), anyString(), anyString(), anyString(), anyDouble(), anyInt());
        verify(auditService).logStudentAction(
                eq(student),
                eq("AI_SUPPORT_QUERY"),
                eq("PsychSupportAssistant"),
                eq(student.getId()),
                contains("risk=HIGH")
        );
    }

    @Test
    @DisplayName("normal support questions return structured reply and strip markdown from answer")
    void ask_regularMessage_returnsStructuredReply() {
        when(geminiClientService.generateJson(anyString(), anyString(), anyString(), anyString(), anyDouble(), anyInt()))
                .thenReturn(new GeminiClientService.GeminiReply(
                        """
                        {
                          "answer": "**Я рядом.** Попробуйте `медленно` сделать несколько спокойных вдохов.",
                          "riskLevel": "LOW",
                          "needsHumanFollowUp": false,
                          "suggestedActions": ["Сделайте паузу на 2 минуты", "Назовите одну мысль, которая тревожит сильнее всего"],
                          "recommendedResources": ["Student Support Office"]
                        }
                        """.trim(),
                        "gemini-2.5-flash",
                        Instant.parse("2026-04-14T12:00:00Z")
                ));

        PsychSupportAssistantService.PsychSupportReply reply = service.ask(
                student,
                "Мне тревожно перед экзаменом, что можно сделать прямо сейчас?"
        );

        assertThat(reply.riskLevel()).isEqualTo("LOW");
        assertThat(reply.needsHumanFollowUp()).isFalse();
        assertThat(reply.answer()).contains("Я рядом.");
        assertThat(reply.answer()).doesNotContain("**");
        assertThat(reply.answer()).doesNotContain("`");
        assertThat(reply.suggestedActions()).hasSize(2);
        assertThat(reply.recommendedResources()).isNotEmpty();
        assertThat(reply.model()).isEqualTo("gemini-2.5-flash");

        verify(auditService).logStudentAction(
                eq(student),
                eq("AI_SUPPORT_QUERY"),
                eq("PsychSupportAssistant"),
                eq(student.getId()),
                contains("risk=LOW")
        );
    }
}
