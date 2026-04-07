package ru.kors.finalproject.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.kors.finalproject.entity.User;

import java.time.Instant;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminAssistantService {
    private static final int MAX_MESSAGE_LENGTH = 2000;

    private final AcademicAnalyticsService academicAnalyticsService;
    private final WorkflowEngineService workflowEngineService;
    private final GeminiClientService geminiClientService;
    private final AuditService auditService;

    @Value("${app.ai.read-only:true}")
    private boolean readOnly;

    public AssistantReply ask(User admin, String message) {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Question cannot be empty");
        }
        if (message.length() > MAX_MESSAGE_LENGTH) {
            throw new IllegalArgumentException("Question is too long");
        }

        String context = buildAdminContext();
        GeminiClientService.GeminiReply reply;
        try {
            reply = geminiClientService.generate(
                    systemPrompt(),
                    "Admin context",
                    context,
                    message.trim(),
                    0.2,
                    850
            );
        } catch (GeminiClientService.GeminiQuotaExceededException ex) {
            reply = new GeminiClientService.GeminiReply(
                    """
                    Дневной лимит Gemini API сейчас исчерпан.
                    Админский аналитический контекст готов, но новый AI-ответ появится только после сброса квоты.
                    Попробуйте позже.
                    """.trim(),
                    "gemini-quota-limit",
                    Instant.now()
            );
        } catch (RuntimeException ex) {
            throw new IllegalStateException("AI assistant is temporarily unavailable");
        }

        auditService.logUserAction(
                admin,
                "AI_ASSISTANT_QUERY",
                "AdminAssistant",
                admin.getId(),
                "message=" + truncate(message.trim(), 180)
        );

        return new AssistantReply(reply.answer(), reply.model(), reply.generatedAt());
    }

    private String buildAdminContext() {
        AcademicAnalyticsService.AdminAnalyticsDashboard analytics = academicAnalyticsService.buildAdminAnalyticsDashboard();
        WorkflowEngineService.WorkflowOverview workflows = workflowEngineService.buildAdminOverview();

        StringBuilder context = new StringBuilder();
        context.append("Assistant mode: ").append(readOnly ? "READ_ONLY" : "READ_WRITE").append("\n");
        context.append("Platform metrics: students=").append(analytics.metrics().students())
                .append(", teachers=").append(analytics.metrics().teachers())
                .append(", currentSections=").append(analytics.metrics().currentSections())
                .append(", requests=").append(analytics.metrics().requests())
                .append(", activeHolds=").append(analytics.metrics().activeHolds())
                .append(", openWindows=").append(analytics.metrics().openWindows())
                .append("\n\n");

        context.append("Faculty risk summary:\n");
        if (analytics.facultyRisks().isEmpty()) {
            context.append("- No faculty risk data.\n");
        } else {
            analytics.facultyRisks().forEach(item -> context.append("- ")
                    .append(item.facultyName())
                    .append(" | students=").append(item.studentCount())
                    .append(" | atRisk=").append(item.atRiskStudents())
                    .append(" | medium=").append(item.mediumRiskStudents())
                    .append(" | avgRisk=").append(item.averageRisk())
                    .append(" | avgAttendance=").append(item.averageAttendance())
                    .append(" | financialHolds=").append(item.studentsWithFinancialHolds())
                    .append("\n"));
        }
        context.append("\n");

        context.append("Overloaded sections:\n");
        if (analytics.overloadedSections().isEmpty()) {
            context.append("- None.\n");
        } else {
            analytics.overloadedSections().forEach(item -> context.append("- ")
                    .append(item.courseCode()).append(" ")
                    .append(item.courseName())
                    .append(" | ").append(item.semesterName())
                    .append(" | teacher=").append(item.teacherName())
                    .append(" | faculty=").append(item.facultyName())
                    .append(" | enrolled=").append(item.enrolledCount()).append("/").append(item.capacity())
                    .append(" | utilization=").append(item.utilizationPercent()).append("%\n"));
        }
        context.append("\n");

        context.append("Workflow backlog:\n");
        analytics.workflowSummary().forEach(item -> context.append("- ")
                .append(item.workflowType()).append(": ").append(item.count()).append("\n"));
        context.append("Open workflow items in queue: ").append(workflows.items().size()).append("\n\n");

        context.append("Top request categories:\n");
        if (analytics.requestLoads().isEmpty()) {
            context.append("- None.\n");
        } else {
            analytics.requestLoads().forEach(item -> context.append("- ").append(item.category())
                    .append(": ").append(item.count()).append("\n"));
        }
        context.append("\n");

        context.append("Critical students:\n");
        if (analytics.criticalStudents().isEmpty()) {
            context.append("- None.\n");
        } else {
            analytics.criticalStudents().forEach(item -> context.append("- ")
                    .append(item.studentName())
                    .append(" | faculty=").append(item.facultyName())
                    .append(" | level=").append(item.level())
                    .append(" | score=").append(item.riskScore())
                    .append(" | reason=").append(item.primaryReason())
                    .append("\n"));
        }
        context.append("\n");

        context.append("Workflow queue items:\n");
        if (workflows.items().isEmpty()) {
            context.append("- No active workflows.\n");
        } else {
            workflows.items().stream().limit(20).forEach(item -> context.append("- ")
                    .append(item.type()).append(" #").append(item.entityId())
                    .append(" | ").append(item.title())
                    .append(" | owner=").append(item.subject())
                    .append(" | status=").append(item.status())
                    .append(" | next=").append(item.nextStatuses().stream().collect(Collectors.joining(",")))
                    .append(" | overdue=").append(item.overdue())
                    .append("\n"));
        }

        return context.toString();
    }

    private String systemPrompt() {
        return """
                You are the KBTU Portal admin assistant.
                Respond in %s unless the admin clearly asks for another language.
                Use only the provided admin context. Do not invent numbers, policies, sections, students, or deadlines.
                Focus on operational insight: overloaded sections, request load, workflow backlog, faculty-level risk, and critical students.
                If the data is insufficient for a precise answer, say so clearly and provide the best grounded summary available.
                Never claim that you changed any record or approved any workflow. This assistant is read-only.
                """.formatted(geminiClientService.getLocale());
    }

    private String truncate(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max - 3) + "...";
    }

    public record AssistantReply(String answer, String model, Instant generatedAt) {
    }
}

