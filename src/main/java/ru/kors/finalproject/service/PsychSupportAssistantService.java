package ru.kors.finalproject.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.kors.finalproject.entity.Program;
import ru.kors.finalproject.entity.Student;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PsychSupportAssistantService {
    private static final int MAX_MESSAGE_LENGTH = 2000;

    private final GeminiClientService geminiClientService;
    private final AuditService auditService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.ai.read-only:true}")
    private boolean readOnly = true;

    @Value("${app.ai.psych-support.support-contact:KBTU Student Support Office (support@kbtu.kz)}")
    private String supportContact = "KBTU Student Support Office (support@kbtu.kz)";

    @Value("${app.ai.psych-support.office-hours:Mon-Fri 09:00-18:00}")
    private String officeHours = "Mon-Fri 09:00-18:00";

    @Value("${app.ai.psych-support.emergency-contact:Local emergency services (112/911)}")
    private String emergencyContact = "Local emergency services (112/911)";

    public PsychSupportReply ask(Student student, String message) {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Question cannot be empty");
        }
        if (message.length() > MAX_MESSAGE_LENGTH) {
            throw new IllegalArgumentException("Question is too long");
        }

        String normalizedMessage = message.trim();
        boolean useRussian = shouldUseRussian(normalizedMessage);

        PsychSupportReply reply;
        if (isHighRiskMessage(normalizedMessage)) {
            reply = buildHighRiskReply(useRussian);
        } else {
            try {
                GeminiClientService.GeminiReply aiReply = geminiClientService.generateJson(
                        systemPrompt(),
                        "Student wellbeing support context",
                        buildSupportContext(student, useRussian),
                        normalizedMessage,
                        0.2,
                        700
                );
                reply = mergeReply(aiReply, parseAiJson(aiReply.answer(), AiPsychSupportPlan.class), useRussian);
            } catch (GeminiClientService.GeminiQuotaExceededException ex) {
                reply = buildQuotaReply(useRussian);
            } catch (RuntimeException ex) {
                throw new IllegalStateException("AI assistant is temporarily unavailable");
            }
        }

        auditService.logStudentAction(
                student,
                "AI_SUPPORT_QUERY",
                "PsychSupportAssistant",
                student.getId(),
                "risk=" + reply.riskLevel() + "; followUp=" + reply.needsHumanFollowUp() + "; length=" + normalizedMessage.length()
        );

        return sanitizeReply(reply);
    }

    private String buildSupportContext(Student student, boolean useRussian) {
        Program program = student.getProgram();
        StringBuilder context = new StringBuilder();
        context.append("Student name: ").append(student.getName()).append("\n");
        context.append("Program: ").append(program != null ? program.getName() : "-").append("\n");
        context.append("Course year: ").append(student.getCourse()).append("\n");
        context.append("Assistant mode: ").append(readOnly ? "READ_ONLY" : "READ_WRITE").append("\n");
        context.append("Preferred response language: ").append(useRussian ? "ru" : geminiClientService.getLocale()).append("\n");
        context.append("Campus support contact: ").append(supportContact).append("\n");
        context.append("Campus support office hours: ").append(officeHours).append("\n");
        context.append("Emergency contact: ").append(emergencyContact).append("\n");
        context.append("Important: This assistant is supportive, not diagnostic, and must not prescribe medication or claim therapy.\n");
        return context.toString();
    }

    private String systemPrompt() {
        return """
                You are the KBTU student wellbeing and psychological support assistant.
                Respond in %s unless the student clearly asks for another language.

                Your role:
                - Provide calm, supportive, non-judgmental emotional support.
                - Help the student reflect, ground themselves, and choose a safe next step.
                - Encourage qualified human support when needed.

                Safety rules:
                - Do NOT diagnose mental disorders.
                - Do NOT prescribe medication.
                - Do NOT claim to replace a psychologist, psychiatrist, doctor, or emergency service.
                - If the student appears at risk of self-harm, suicide, or harming others, set riskLevel=HIGH, set needsHumanFollowUp=true, and prioritize immediate safety steps plus emergency/human contact.
                - Keep the answer brief and practical.
                - Return plain text only in the answer field. No markdown, no bullets with asterisks, no tables.

                Return ONLY valid JSON in this exact shape:
                {
                  "answer": "Short supportive plain-text reply in the user's language",
                  "riskLevel": "LOW | MEDIUM | HIGH",
                  "needsHumanFollowUp": true,
                  "suggestedActions": ["short action", "short action"],
                  "recommendedResources": ["resource", "resource"]
                }
                """.formatted(geminiClientService.getLocale());
    }

    private PsychSupportReply mergeReply(
            GeminiClientService.GeminiReply aiReply,
            AiPsychSupportPlan plan,
            boolean useRussian
    ) {
        String riskLevel = normalizeRiskLevel(plan.riskLevel());
        boolean needsHumanFollowUp = Boolean.TRUE.equals(plan.needsHumanFollowUp()) || !"LOW".equals(riskLevel);
        List<String> suggestedActions = sanitizeStrings(plan.suggestedActions());
        if (suggestedActions.isEmpty()) {
            suggestedActions = defaultSuggestedActions(riskLevel, useRussian);
        }
        List<String> recommendedResources = mergeResources(
                sanitizeStrings(plan.recommendedResources()),
                defaultResources(riskLevel, needsHumanFollowUp, useRussian)
        );
        String answer = blankToNull(plan.answer());
        if (answer == null) {
            answer = defaultAnswer(riskLevel, useRussian);
        }
        return new PsychSupportReply(
                answer,
                aiReply.model(),
                aiReply.generatedAt(),
                riskLevel,
                needsHumanFollowUp,
                suggestedActions,
                recommendedResources
        );
    }

    private PsychSupportReply buildQuotaReply(boolean useRussian) {
        return new PsychSupportReply(
                useRussian
                        ? "Я сейчас не могу дать новый AI-ответ, но вы можете обратиться в Student Support Office или к близкому человеку, которому доверяете."
                        : "I cannot generate a new AI reply right now, but you can contact the Student Support Office or reach out to someone you trust.",
                "gemini-quota-limit",
                Instant.now(),
                "MEDIUM",
                true,
                defaultSuggestedActions("MEDIUM", useRussian),
                defaultResources("MEDIUM", true, useRussian)
        );
    }

    private PsychSupportReply buildHighRiskReply(boolean useRussian) {
        return new PsychSupportReply(
                useRussian
                        ? "Мне очень жаль, что вам сейчас так тяжело. Если есть риск, что вы можете причинить вред себе или кому-то ещё, пожалуйста, немедленно обратитесь в экстренные службы и свяжитесь с человеком рядом, которому вы доверяете. Не оставайтесь один или одна."
                        : "I'm sorry you're going through something this intense. If there is any risk that you might harm yourself or someone else, please contact emergency services right now and reach out to a trusted person nearby. Do not stay alone.",
                "safety-crisis-guard",
                Instant.now(),
                "HIGH",
                true,
                defaultSuggestedActions("HIGH", useRussian),
                defaultResources("HIGH", true, useRussian)
        );
    }

    private List<String> defaultSuggestedActions(String riskLevel, boolean useRussian) {
        if ("HIGH".equals(riskLevel)) {
            return useRussian
                    ? List.of(
                    "Позвоните в экстренные службы прямо сейчас.",
                    "Свяжитесь с близким человеком и скажите, что вам нужна срочная помощь.",
                    "Перейдите в безопасное место и не оставайтесь в одиночестве."
            )
                    : List.of(
                    "Call emergency services right now.",
                    "Reach out to a trusted person and tell them you need urgent help.",
                    "Move to a safe place and do not stay alone."
            );
        }
        if ("MEDIUM".equals(riskLevel)) {
            return useRussian
                    ? List.of(
                    "Сделайте короткую паузу и немного замедлите дыхание.",
                    "Опишите одной фразой, что сейчас давит сильнее всего.",
                    "Подумайте, с кем из взрослых специалистов или близких можно связаться сегодня."
            )
                    : List.of(
                    "Take a short pause and slow your breathing.",
                    "Name the one thing that feels heaviest right now.",
                    "Think about which trusted adult or specialist you can contact today."
            );
        }
        return useRussian
                ? List.of(
                "Сделайте короткую паузу и проверьте, как вы себя чувствуете прямо сейчас.",
                "Попробуйте назвать одну вещь, которую можно сделать в ближайшие 10 минут, чтобы стало чуть легче."
        )
                : List.of(
                "Take a brief pause and check how you feel right now.",
                "Name one small thing you can do in the next 10 minutes to feel a little steadier."
        );
    }

    private List<String> defaultResources(String riskLevel, boolean needsHumanFollowUp, boolean useRussian) {
        List<String> resources = new ArrayList<>();
        resources.add((useRussian ? "Student Support Office: " : "Student Support Office: ") + supportContact);
        resources.add((useRussian ? "Часы работы: " : "Office hours: ") + officeHours);
        if (needsHumanFollowUp) {
            resources.add(useRussian ? "Свяжитесь с близким человеком, которому доверяете." : "Reach out to a trusted friend or family member.");
        }
        if ("HIGH".equals(riskLevel)) {
            resources.add((useRussian ? "Экстренная помощь: " : "Emergency help: ") + emergencyContact);
        }
        return resources;
    }

    private String defaultAnswer(String riskLevel, boolean useRussian) {
        if ("HIGH".equals(riskLevel)) {
            return useRussian
                    ? "Сейчас самое важное — ваша безопасность и быстрый контакт с живым человеком."
                    : "Your immediate safety and real human support matter most right now.";
        }
        if ("MEDIUM".equals(riskLevel)) {
            return useRussian
                    ? "Похоже, вам сейчас тяжело. Давайте сосредоточимся на одном спокойном следующем шаге и на том, к кому можно обратиться за поддержкой."
                    : "It sounds like you're under a lot of pressure. Let's focus on one calm next step and on who you can reach out to for support.";
        }
        return useRussian
                ? "Я рядом, чтобы помочь вам спокойно разобрать ситуацию и выбрать бережный следующий шаг."
                : "I'm here to help you look at the situation calmly and choose a gentle next step.";
    }

    private List<String> mergeResources(List<String> preferred, List<String> defaults) {
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        merged.addAll(preferred);
        merged.addAll(defaults);
        return merged.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .toList();
    }

    private boolean isHighRiskMessage(String message) {
        String normalized = message.toLowerCase(Locale.ROOT);
        return containsAny(normalized,
                "suicide", "kill myself", "end my life", "want to die", "don't want to live", "self-harm", "hurt myself", "harm myself",
                "суицид", "убить себя", "хочу умереть", "не хочу жить", "покончить с собой", "навредить себе", "причинить себе вред", "порезать себя",
                "убью себя", "вскрыть вены", "спрыгнуть", "повеситься", "передоз", "передозировка"
        );
    }

    private boolean containsAny(String text, String... tokens) {
        for (String token : tokens) {
            if (text.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private boolean shouldUseRussian(String message) {
        if (message != null && message.chars().anyMatch(ch -> ch >= 0x0400 && ch <= 0x04FF)) {
            return true;
        }
        String locale = geminiClientService.getLocale();
        return locale != null && locale.toLowerCase(Locale.ROOT).startsWith("ru");
    }

    private String normalizeRiskLevel(String raw) {
        if (raw == null) {
            return "MEDIUM";
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "LOW", "MEDIUM", "HIGH" -> normalized;
            default -> "MEDIUM";
        };
    }

    private PsychSupportReply sanitizeReply(PsychSupportReply reply) {
        return new PsychSupportReply(
                stripMarkdown(reply.answer()),
                reply.model(),
                reply.generatedAt(),
                normalizeRiskLevel(reply.riskLevel()),
                reply.needsHumanFollowUp(),
                sanitizeStrings(reply.suggestedActions()),
                sanitizeStrings(reply.recommendedResources())
        );
    }

    private List<String> sanitizeStrings(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .toList();
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String stripMarkdown(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }

        String normalized = text.replace("\r\n", "\n").replace('\r', '\n');
        List<String> cleanedLines = normalized.lines()
                .map(this::stripMarkdownLine)
                .toList();

        String joined = String.join("\n", cleanedLines)
                .replaceAll("\n{3,}", "\n\n")
                .trim();
        return joined.isBlank() ? text.trim() : joined;
    }

    private String stripMarkdownLine(String line) {
        if (line == null || line.isBlank()) {
            return "";
        }
        String cleaned = line;
        cleaned = cleaned.replaceAll("^\\s{0,3}#{1,6}\\s*", "");
        cleaned = cleaned.replaceAll("^\\s*>\\s?", "");
        cleaned = cleaned.replaceAll("^\\s*[-*+]\\s+\\[[ xX]\\]\\s+", "- ");
        cleaned = cleaned.replaceAll("^\\s*[-*+]\\s+", "- ");
        cleaned = cleaned.replaceAll("\\*\\*(.*?)\\*\\*", "$1");
        cleaned = cleaned.replaceAll("__(.*?)__", "$1");
        cleaned = cleaned.replaceAll("(?<!\\*)\\*(?!\\s)(.*?)(?<!\\s)\\*(?!\\*)", "$1");
        cleaned = cleaned.replaceAll("(?<!_)_(?!\\s)(.*?)(?<!\\s)_(?!_)", "$1");
        cleaned = cleaned.replace("`", "");
        cleaned = cleaned.replaceAll("!\\[([^\\]]*)\\]\\(([^)]+)\\)", "$1");
        cleaned = cleaned.replaceAll("\\[([^\\]]+)\\]\\(([^)]+)\\)", "$1");
        return cleaned.trim();
    }

    private <T> T parseAiJson(String text, Class<T> type) {
        String json = extractJson(text);
        try {
            return objectMapper.readerFor(type)
                    .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .readValue(json);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("AI returned invalid psych support data");
        }
    }

    private String extractJson(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalStateException("AI returned empty psych support data");
        }

        String trimmed = raw.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstNewline >= 0 && lastFence > firstNewline) {
                trimmed = trimmed.substring(firstNewline + 1, lastFence).trim();
            }
        }

        int firstBrace = trimmed.indexOf('{');
        int lastBrace = trimmed.lastIndexOf('}');
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            trimmed = trimmed.substring(firstBrace, lastBrace + 1);
        }
        return trimmed;
    }

    private record AiPsychSupportPlan(
            String answer,
            String riskLevel,
            Boolean needsHumanFollowUp,
            List<String> suggestedActions,
            List<String> recommendedResources
    ) {
    }

    public record PsychSupportReply(
            String answer,
            String model,
            Instant generatedAt,
            String riskLevel,
            boolean needsHumanFollowUp,
            List<String> suggestedActions,
            List<String> recommendedResources
    ) {
    }
}
