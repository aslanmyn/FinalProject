package ru.kors.finalproject.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import ru.kors.finalproject.entity.*;
import ru.kors.finalproject.repository.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class StudentAssistantService {
    private static final int MAX_MESSAGE_LENGTH = 2000;

    private final RegistrationRepository registrationRepository;
    private final GradeRepository gradeRepository;
    private final FinalGradeRepository finalGradeRepository;
    private final AttendanceRepository attendanceRepository;
    private final HoldRepository holdRepository;
    private final StudentRequestRepository studentRequestRepository;
    private final ExamScheduleRepository examScheduleRepository;
    private final AuditService auditService;

    @Value("${app.ai.enabled:false}")
    private boolean aiEnabled;

    @Value("${app.ai.read-only:true}")
    private boolean readOnly;

    @Value("${app.ai.gemini.api-key:}")
    private String geminiApiKey;

    @Value("${app.ai.gemini.model:gemini-2.5-flash}")
    private String geminiModel;

    private final RestClient restClient = RestClient.builder()
            .baseUrl("https://generativelanguage.googleapis.com/v1beta")
            .build();

    public AssistantReply ask(Student student, String message) {
        if (!aiEnabled) {
            throw new IllegalStateException("AI assistant is disabled");
        }
        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            throw new IllegalStateException("Gemini API key is not configured");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Question cannot be empty");
        }
        if (message.length() > MAX_MESSAGE_LENGTH) {
            throw new IllegalArgumentException("Question is too long");
        }

        String context = buildStudentContext(student);
        String answer;
        try {
            answer = generateAnswer(context, message.trim());
        } catch (RuntimeException ex) {
            throw new IllegalStateException("AI assistant is temporarily unavailable");
        }

        auditService.logStudentAction(
                student,
                "AI_ASSISTANT_QUERY",
                "StudentAssistant",
                student.getId(),
                "message=" + truncate(message.trim(), 180)
        );

        return new AssistantReply(answer, geminiModel, Instant.now());
    }

    private String generateAnswer(String context, String message) {
        GeminiGenerateRequest request = new GeminiGenerateRequest(
                new GeminiInstruction(List.of(new GeminiPart(systemPrompt()))),
                List.of(new GeminiContent("user", List.of(new GeminiPart("""
                        Student context:
                        %s

                        User question:
                        %s
                        """.formatted(context, message))))),
                new GeminiGenerationConfig(0.2, 700)
        );

        GeminiGenerateResponse response = restClient.post()
                .uri("/models/{model}:generateContent", geminiModel)
                .header("x-goog-api-key", geminiApiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(GeminiGenerateResponse.class);

        if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
            throw new IllegalStateException("Empty AI response");
        }

        String text = response.candidates().stream()
                .map(GeminiCandidate::content)
                .filter(Objects::nonNull)
                .flatMap(content -> content.parts() != null ? content.parts().stream() : java.util.stream.Stream.empty())
                .map(GeminiPart::text)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .findFirst()
                .orElse(null);

        if (text == null || text.isBlank()) {
            throw new IllegalStateException("AI returned no text");
        }
        return text;
    }

    private String buildStudentContext(Student student) {
        Semester activeSemester = student.getCurrentSemester();
        List<Registration> registrations = registrationRepository.findByStudentIdWithDetails(student.getId());
        if (activeSemester == null) {
            activeSemester = registrations.stream()
                    .map(registration -> registration.getSubjectOffering() != null ? registration.getSubjectOffering().getSemester() : null)
                    .filter(Objects::nonNull)
                    .max(Comparator.comparing(Semester::getStartDate, Comparator.nullsLast(Comparator.naturalOrder())))
                    .orElse(null);
        }

        Long activeSemesterId = activeSemester != null ? activeSemester.getId() : null;
        List<Registration> semesterRegistrations = registrations.stream()
                .filter(registration -> registration.getStatus() != Registration.RegistrationStatus.DROPPED)
                .filter(registration -> activeSemesterId == null
                        || (registration.getSubjectOffering() != null
                        && registration.getSubjectOffering().getSemester() != null
                        && Objects.equals(registration.getSubjectOffering().getSemester().getId(), activeSemesterId)))
                .sorted(Comparator.comparing(registration -> registration.getSubjectOffering().getSubject().getCode()))
                .toList();

        List<Grade> publishedGrades = gradeRepository.findByStudentIdAndPublishedTrueWithDetails(student.getId());
        List<FinalGrade> publishedFinalGrades = finalGradeRepository.findByStudentIdAndPublishedTrueWithDetails(student.getId());
        List<Attendance> attendanceRecords = attendanceRepository.findByStudentIdWithDetails(student.getId());
        List<Hold> activeHolds = holdRepository.findByStudentIdAndActiveTrue(student.getId());
        List<StudentRequest> requests = studentRequestRepository.findByStudentIdWithDetailsOrderByCreatedAtDesc(student.getId());

        Map<Long, CourseInsight> courseInsights = new LinkedHashMap<>();
        for (Registration registration : semesterRegistrations) {
            SubjectOffering offering = registration.getSubjectOffering();
            if (offering == null || offering.getSubject() == null) {
                continue;
            }
            courseInsights.put(offering.getId(), new CourseInsight(
                    offering.getId(),
                    offering.getSubject().getCode(),
                    offering.getSubject().getName(),
                    offering.getTeacher() != null ? offering.getTeacher().getName() : null,
                    offering.getSubject().getCredits(),
                    registration.getStatus().name()
            ));
        }

        publishedGrades.stream()
                .filter(grade -> grade.getSubjectOffering() != null
                        && courseInsights.containsKey(grade.getSubjectOffering().getId()))
                .forEach(grade -> {
                    CourseInsight insight = courseInsights.get(grade.getSubjectOffering().getId());
                    String normalized = normalizeComponent(grade);
                    if (normalized.contains("attestation 1")) {
                        insight.attestation1 = grade.getGradeValue();
                    } else if (normalized.contains("attestation 2")) {
                        insight.attestation2 = grade.getGradeValue();
                    } else if (normalized.contains("final") || grade.getType() == Grade.GradeType.FINAL) {
                        insight.finalExam = grade.getGradeValue();
                    }
                });

        publishedFinalGrades.stream()
                .filter(finalGrade -> finalGrade.getSubjectOffering() != null
                        && courseInsights.containsKey(finalGrade.getSubjectOffering().getId()))
                .forEach(finalGrade -> {
                    CourseInsight insight = courseInsights.get(finalGrade.getSubjectOffering().getId());
                    insight.total = finalGrade.getNumericValue();
                    insight.letter = finalGrade.getLetterValue();
                    insight.points = finalGrade.getPoints();
                });

        attendanceRecords.stream()
                .filter(attendance -> attendance.getSubjectOffering() != null
                        && courseInsights.containsKey(attendance.getSubjectOffering().getId()))
                .forEach(attendance -> {
                    CourseInsight insight = courseInsights.get(attendance.getSubjectOffering().getId());
                    insight.totalAttendance += 1;
                    if (attendance.getStatus() == Attendance.AttendanceStatus.PRESENT) {
                        insight.present += 1;
                    } else if (attendance.getStatus() == Attendance.AttendanceStatus.LATE) {
                        insight.late += 1;
                    } else if (attendance.getStatus() == Attendance.AttendanceStatus.ABSENT) {
                        insight.absent += 1;
                    }
                });

        List<ExamSchedule> upcomingExams = activeSemesterId == null || courseInsights.isEmpty()
                ? List.of()
                : examScheduleRepository.findBySubjectOfferingIdInWithDetails(
                                new ArrayList<>(courseInsights.keySet()))
                        .stream()
                        .sorted(Comparator.comparing(ExamSchedule::getExamDate).thenComparing(ExamSchedule::getExamTime))
                        .limit(10)
                        .toList();

        double publishedGpa = publishedFinalGrades.isEmpty()
                ? 0.0
                : publishedFinalGrades.stream().mapToDouble(FinalGrade::getPoints).average().orElse(0.0);

        long overallPresent = attendanceRecords.stream().filter(item -> item.getStatus() == Attendance.AttendanceStatus.PRESENT).count();
        long overallLate = attendanceRecords.stream().filter(item -> item.getStatus() == Attendance.AttendanceStatus.LATE).count();
        long overallAbsent = attendanceRecords.stream().filter(item -> item.getStatus() == Attendance.AttendanceStatus.ABSENT).count();
        double overallAttendanceRate = attendanceRecords.isEmpty()
                ? 0.0
                : ((overallPresent + overallLate) * 100.0 / attendanceRecords.size());

        StringBuilder context = new StringBuilder();
        context.append("Student: ").append(student.getName()).append(" (").append(student.getEmail()).append(")\n");
        context.append("Program: ").append(valueOrDash(student.getProgram() != null ? student.getProgram().getName() : null)).append("\n");
        context.append("Faculty: ").append(valueOrDash(student.getFaculty() != null ? student.getFaculty().getName() : null)).append("\n");
        context.append("Course year: ").append(student.getCourse()).append("\n");
        context.append("Published GPA: ").append(formatDouble(publishedGpa)).append("\n");
        context.append("Credits earned: ").append(student.getCreditsEarned()).append("\n");
        context.append("Active semester: ").append(activeSemester != null ? activeSemester.getName() : "N/A").append("\n");
        context.append("Assistant mode: ").append(readOnly ? "READ_ONLY" : "READ_WRITE").append("\n\n");

        context.append("Overall attendance summary:\n");
        context.append("- Present: ").append(overallPresent)
                .append(", Late: ").append(overallLate)
                .append(", Absent: ").append(overallAbsent)
                .append(", Rate: ").append(formatDouble(overallAttendanceRate)).append("%\n\n");

        context.append("Current semester course insights:\n");
        if (courseInsights.isEmpty()) {
            context.append("- No active semester courses found.\n");
        } else {
            for (CourseInsight insight : courseInsights.values()) {
                double subtotal = insight.attestation1 + insight.attestation2;
                context.append("- ").append(insight.code).append(" | ").append(insight.name)
                        .append(" | teacher=").append(valueOrDash(insight.teacherName))
                        .append(" | credits=").append(insight.credits)
                        .append(" | status=").append(insight.registrationStatus)
                        .append(" | att1=").append(formatDouble(insight.attestation1)).append("/30")
                        .append(" | att2=").append(formatDouble(insight.attestation2)).append("/30")
                        .append(" | final=").append(formatOptionalDouble(insight.finalExam)).append("/40")
                        .append(" | subtotal=").append(formatDouble(subtotal)).append("/60")
                        .append(" | neededFinalFor60=").append(formatNeededFinal(subtotal, 60))
                        .append(" | neededFinalFor70=").append(formatNeededFinal(subtotal, 70))
                        .append(" | neededFinalFor80=").append(formatNeededFinal(subtotal, 80))
                        .append(" | neededFinalFor90=").append(formatNeededFinal(subtotal, 90))
                        .append(" | total=").append(formatOptionalDouble(insight.total))
                        .append(" | letter=").append(valueOrDash(insight.letter))
                        .append(" | attendance=").append(insight.attendanceRateText())
                        .append("\n");
            }
        }
        context.append("\n");

        context.append("Upcoming exams:\n");
        if (upcomingExams.isEmpty()) {
            context.append("- No upcoming exams found.\n");
        } else {
            for (ExamSchedule exam : upcomingExams) {
                context.append("- ")
                        .append(exam.getSubjectOffering().getSubject().getCode())
                        .append(" | ")
                        .append(exam.getSubjectOffering().getSubject().getName())
                        .append(" | ")
                        .append(exam.getExamDate())
                        .append(" ")
                        .append(exam.getExamTime())
                        .append(" | room=")
                        .append(valueOrDash(exam.getRoom()))
                        .append(" | format=")
                        .append(valueOrDash(exam.getFormat()))
                        .append("\n");
            }
        }
        context.append("\n");

        context.append("Active holds:\n");
        if (activeHolds.isEmpty()) {
            context.append("- No active holds.\n");
        } else {
            for (Hold hold : activeHolds) {
                context.append("- ").append(hold.getType()).append(": ").append(valueOrDash(hold.getReason())).append("\n");
            }
        }
        context.append("\n");

        context.append("Recent requests:\n");
        if (requests.isEmpty()) {
            context.append("- No requests.\n");
        } else {
            requests.stream().limit(5).forEach(request -> context.append("- ")
                    .append(request.getCategory())
                    .append(" | status=").append(request.getStatus())
                    .append(" | created=").append(request.getCreatedAt())
                    .append(" | description=").append(truncate(request.getDescription(), 120))
                    .append("\n"));
        }

        return context.toString();
    }

    private String systemPrompt() {
        return """
                You are the KBTU Portal student assistant.
                Respond in Russian unless the user clearly asks for English.
                Use only the provided student context. Do not invent policies, grades, deadlines, or attendance records.
                If the needed data is missing, say so clearly.
                Keep answers practical and concise, but explain calculations when the student asks about scores or GPA.
                Grade rule: Attestation 1 max 30, Attestation 2 max 30, Final max 40, Total max 100.
                Formula: needed final score = target total - attestation1 - attestation2.
                If needed final score is above 40, say the target is not reachable.
                If needed final score is 0 or below, say the target is already secured.
                For GPA questions, rely on the published GPA in the context. If exact future GPA cannot be computed from the provided data, say that and give advice based on current courses.
                Never claim you changed data. This assistant is read-only.
                """;
    }

    private String normalizeComponent(Grade grade) {
        String componentName = grade.getComponent() != null ? grade.getComponent().getName() : "";
        String comment = grade.getComment() != null ? grade.getComment() : "";
        return (componentName + " " + comment).toLowerCase();
    }

    private String formatNeededFinal(double subtotal, int target) {
        double needed = target - subtotal;
        if (needed <= 0) {
            return "0.00 (already reached)";
        }
        if (needed > 40) {
            return formatDouble(needed) + " (not possible)";
        }
        return formatDouble(needed);
    }

    private String formatOptionalDouble(Double value) {
        return value == null ? "-" : formatDouble(value);
    }

    private String formatDouble(double value) {
        return String.format(java.util.Locale.US, "%.2f", value);
    }

    private String valueOrDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String truncate(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max - 3) + "...";
    }

    public record AssistantReply(String answer, String model, Instant generatedAt) {
    }

    private static class CourseInsight {
        private final Long offeringId;
        private final String code;
        private final String name;
        private final String teacherName;
        private final int credits;
        private final String registrationStatus;
        private double attestation1;
        private double attestation2;
        private Double finalExam;
        private Double total;
        private Double points;
        private String letter;
        private int present;
        private int late;
        private int absent;
        private int totalAttendance;

        private CourseInsight(Long offeringId, String code, String name, String teacherName, int credits, String registrationStatus) {
            this.offeringId = offeringId;
            this.code = code;
            this.name = name;
            this.teacherName = teacherName;
            this.credits = credits;
            this.registrationStatus = registrationStatus;
        }

        private String attendanceRateText() {
            if (totalAttendance == 0) {
                return "no records";
            }
            double rate = ((present + late) * 100.0) / totalAttendance;
            return String.format(java.util.Locale.US, "%.1f%% (%d present, %d late, %d absent)", rate, present, late, absent);
        }
    }

    private record GeminiGenerateRequest(
            @JsonProperty("system_instruction") GeminiInstruction systemInstruction,
            List<GeminiContent> contents,
            GeminiGenerationConfig generationConfig
    ) {
    }

    private record GeminiInstruction(List<GeminiPart> parts) {
    }

    private record GeminiContent(String role, List<GeminiPart> parts) {
    }

    private record GeminiPart(String text) {
    }

    private record GeminiGenerationConfig(Double temperature, Integer maxOutputTokens) {
    }

    private record GeminiGenerateResponse(List<GeminiCandidate> candidates) {
    }

    private record GeminiCandidate(GeminiContent content) {
    }
}
