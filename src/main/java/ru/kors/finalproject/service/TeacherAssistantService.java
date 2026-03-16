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
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeacherAssistantService {
    private static final int MAX_MESSAGE_LENGTH = 2000;

    private final SubjectOfferingRepository subjectOfferingRepository;
    private final RegistrationRepository registrationRepository;
    private final AttendanceRepository attendanceRepository;
    private final AssessmentComponentRepository assessmentComponentRepository;
    private final GradeRepository gradeRepository;
    private final FinalGradeRepository finalGradeRepository;
    private final GradeChangeRequestRepository gradeChangeRequestRepository;
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

    public AssistantReply ask(User user, Teacher teacher, String message) {
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

        String context = buildTeacherContext(teacher);
        String answer;
        try {
            answer = generateAnswer(context, message.trim());
        } catch (RuntimeException ex) {
            throw new IllegalStateException("AI assistant is temporarily unavailable");
        }

        auditService.logUserAction(
                user,
                "AI_ASSISTANT_QUERY",
                "TeacherAssistant",
                teacher.getId(),
                "message=" + truncate(message.trim(), 180)
        );

        return new AssistantReply(answer, geminiModel, Instant.now());
    }

    private String generateAnswer(String context, String message) {
        GeminiGenerateRequest request = new GeminiGenerateRequest(
                new GeminiInstruction(List.of(new GeminiPart(systemPrompt()))),
                List.of(new GeminiContent("user", List.of(new GeminiPart("""
                        Teacher context:
                        %s

                        User question:
                        %s
                        """.formatted(context, message))))),
                new GeminiGenerationConfig(0.2, 800)
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

    private String buildTeacherContext(Teacher teacher) {
        List<SubjectOffering> allSections = subjectOfferingRepository.findByTeacherIdWithDetails(teacher.getId());
        List<SubjectOffering> focusedSections = selectFocusedSections(allSections);
        List<GradeChangeRequest> teacherRequests = gradeChangeRequestRepository.findByTeacherIdWithDetailsOrderByCreatedAtDesc(teacher.getId());

        StringBuilder context = new StringBuilder();
        context.append("Teacher: ").append(teacher.getName()).append(" (").append(teacher.getEmail()).append(")\n");
        context.append("Faculty: ").append(valueOrDash(teacher.getFaculty() != null ? teacher.getFaculty().getName() : null)).append("\n");
        context.append("Department: ").append(valueOrDash(teacher.getDepartment())).append("\n");
        context.append("Position: ").append(valueOrDash(teacher.getPositionTitle())).append("\n");
        context.append("Assistant mode: ").append(readOnly ? "READ_ONLY" : "READ_WRITE").append("\n");
        context.append("Total assigned sections: ").append(allSections.size()).append("\n");
        context.append("Focused sections in context: ").append(focusedSections.size()).append("\n\n");

        context.append("Pending grade change requests:\n");
        List<GradeChangeRequest> pendingRequests = teacherRequests.stream()
                .filter(request -> request.getStatus() == GradeChangeRequest.RequestStatus.SUBMITTED)
                .limit(10)
                .toList();
        if (pendingRequests.isEmpty()) {
            context.append("- No pending requests.\n");
        } else {
            for (GradeChangeRequest request : pendingRequests) {
                context.append("- ")
                        .append(request.getSubjectOffering() != null && request.getSubjectOffering().getSubject() != null
                                ? request.getSubjectOffering().getSubject().getCode()
                                : "Section")
                        .append(" | student=").append(request.getStudent() != null ? request.getStudent().getName() : "-")
                        .append(" | old=").append(request.getOldValue())
                        .append(" | new=").append(request.getNewValue())
                        .append(" | reason=").append(truncate(request.getReason(), 120))
                        .append(" | created=").append(request.getCreatedAt())
                        .append("\n");
            }
        }
        context.append("\n");

        context.append("Section summaries:\n");
        if (focusedSections.isEmpty()) {
            context.append("- No sections found.\n");
            return context.toString();
        }

        for (SubjectOffering section : focusedSections) {
            List<Registration> roster = registrationRepository.findBySubjectOfferingIdAndStatusInWithDetails(
                    section.getId(),
                    List.of(Registration.RegistrationStatus.CONFIRMED, Registration.RegistrationStatus.SUBMITTED)
            );
            List<Attendance> attendance = attendanceRepository.findBySubjectOfferingIdOrderByDateDescWithDetails(section.getId());
            List<AssessmentComponent> components = assessmentComponentRepository.findBySubjectOfferingIdOrderByCreatedAtAsc(section.getId());
            List<Grade> grades = gradeRepository.findBySubjectOfferingIdWithDetails(section.getId());
            List<FinalGrade> finalGrades = finalGradeRepository.findBySubjectOfferingIdWithDetails(section.getId());
            List<GradeChangeRequest> sectionPendingRequests = teacherRequests.stream()
                    .filter(request -> request.getSubjectOffering() != null && Objects.equals(request.getSubjectOffering().getId(), section.getId()))
                    .filter(request -> request.getStatus() == GradeChangeRequest.RequestStatus.SUBMITTED)
                    .toList();

            Map<Long, StudentPerformance> performanceByStudent = new LinkedHashMap<>();
            for (Registration registration : roster) {
                Student student = registration.getStudent();
                performanceByStudent.put(student.getId(), new StudentPerformance(student.getId(), student.getName(), student.getEmail()));
            }

            attendance.forEach(item -> {
                StudentPerformance performance = performanceByStudent.get(item.getStudent().getId());
                if (performance == null) return;
                performance.totalAttendance += 1;
                if (item.getStatus() == Attendance.AttendanceStatus.PRESENT) {
                    performance.present += 1;
                } else if (item.getStatus() == Attendance.AttendanceStatus.LATE) {
                    performance.late += 1;
                } else if (item.getStatus() == Attendance.AttendanceStatus.ABSENT) {
                    performance.absent += 1;
                }
            });

            grades.forEach(grade -> {
                StudentPerformance performance = performanceByStudent.get(grade.getStudent().getId());
                if (performance == null) return;
                String normalized = normalizeComponent(grade);
                if (normalized.contains("attestation 1")) {
                    performance.attestation1 = grade.getGradeValue();
                } else if (normalized.contains("attestation 2")) {
                    performance.attestation2 = grade.getGradeValue();
                } else if (normalized.contains("final") || grade.getType() == Grade.GradeType.FINAL) {
                    performance.finalExam = grade.getGradeValue();
                }
                if (grade.isPublished()) {
                    performance.publishedGrades += 1;
                }
            });

            finalGrades.forEach(finalGrade -> {
                StudentPerformance performance = performanceByStudent.get(finalGrade.getStudent().getId());
                if (performance == null) return;
                performance.finalTotal = finalGrade.getNumericValue();
                performance.finalLetter = finalGrade.getLetterValue();
            });

            List<StudentPerformance> atRisk = performanceByStudent.values().stream()
                    .peek(StudentPerformance::calculateRisk)
                    .filter(StudentPerformance::isAtRisk)
                    .sorted(Comparator.comparing(StudentPerformance::riskScore).reversed()
                            .thenComparing(performance -> performance.name))
                    .limit(8)
                    .toList();

            long publishedComponents = components.stream().filter(AssessmentComponent::isPublished).count();
            long lockedComponents = components.stream().filter(AssessmentComponent::isLocked).count();
            long publishedGradeRecords = grades.stream().filter(Grade::isPublished).count();
            long publishedFinalCount = finalGrades.stream().filter(FinalGrade::isPublished).count();
            double averagePublishedFinal = finalGrades.stream()
                    .filter(FinalGrade::isPublished)
                    .mapToDouble(FinalGrade::getNumericValue)
                    .average()
                    .orElse(0.0);

            long sectionPresent = attendance.stream().filter(item -> item.getStatus() == Attendance.AttendanceStatus.PRESENT).count();
            long sectionLate = attendance.stream().filter(item -> item.getStatus() == Attendance.AttendanceStatus.LATE).count();
            long sectionAbsent = attendance.stream().filter(item -> item.getStatus() == Attendance.AttendanceStatus.ABSENT).count();
            double attendanceRate = attendance.isEmpty()
                    ? 0.0
                    : ((sectionPresent + sectionLate) * 100.0 / attendance.size());

            context.append("- Section ").append(section.getId())
                    .append(" | ").append(section.getSubject() != null ? section.getSubject().getCode() : "-")
                    .append(" | ").append(section.getSubject() != null ? section.getSubject().getName() : "-")
                    .append(" | semester=").append(section.getSemester() != null ? section.getSemester().getName() : "-")
                    .append(" | lessonType=").append(section.getLessonType())
                    .append(" | credits=").append(section.getSubject() != null ? section.getSubject().getCredits() : 0)
                    .append(" | capacity=").append(section.getCapacity())
                    .append(" | enrolled=").append(roster.size())
                    .append(" | schedule=").append(formatMeetingTimes(section.getMeetingTimes()))
                    .append("\n");
            context.append("  Components: total=").append(components.size())
                    .append(", published=").append(publishedComponents)
                    .append(", locked=").append(lockedComponents)
                    .append(", publishedGradeRecords=").append(publishedGradeRecords)
                    .append(", publishedFinals=").append(publishedFinalCount).append("/").append(roster.size())
                    .append(", avgPublishedFinal=").append(formatDouble(averagePublishedFinal))
                    .append(", pendingGradeChanges=").append(sectionPendingRequests.size())
                    .append("\n");
            context.append("  Attendance: present=").append(sectionPresent)
                    .append(", late=").append(sectionLate)
                    .append(", absent=").append(sectionAbsent)
                    .append(", rate=").append(formatDouble(attendanceRate)).append("%")
                    .append("\n");

            if (atRisk.isEmpty()) {
                context.append("  Students at risk: none detected.\n");
            } else {
                context.append("  Students at risk:\n");
                for (StudentPerformance performance : atRisk) {
                    context.append("    - ").append(performance.name)
                            .append(" | email=").append(performance.email)
                            .append(" | reasons=").append(String.join("; ", performance.riskReasons))
                            .append(" | att1=").append(formatDouble(performance.attestation1))
                            .append(" | att2=").append(formatDouble(performance.attestation2))
                            .append(" | final=").append(performance.finalExam == null ? "-" : formatDouble(performance.finalExam))
                            .append(" | total=").append(performance.finalTotal == null ? "-" : formatDouble(performance.finalTotal))
                            .append(" | attendance=").append(performance.attendanceRateText())
                            .append("\n");
                }
            }
            context.append("\n");
        }

        return context.toString();
    }

    private List<SubjectOffering> selectFocusedSections(List<SubjectOffering> sections) {
        List<SubjectOffering> current = sections.stream()
                .filter(section -> section.getSemester() != null && section.getSemester().isCurrent())
                .sorted(sectionComparator())
                .toList();
        if (!current.isEmpty()) {
            return current;
        }

        Semester latestSemester = sections.stream()
                .map(SubjectOffering::getSemester)
                .filter(Objects::nonNull)
                .max(Comparator.comparing(Semester::getStartDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);
        if (latestSemester == null) {
            return List.of();
        }
        return sections.stream()
                .filter(section -> section.getSemester() != null && Objects.equals(section.getSemester().getId(), latestSemester.getId()))
                .sorted(sectionComparator())
                .toList();
    }

    private Comparator<SubjectOffering> sectionComparator() {
        return Comparator
                .comparing((SubjectOffering section) -> section.getSemester() != null ? section.getSemester().getStartDate() : null,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(section -> section.getSubject() != null ? section.getSubject().getCode() : "");
    }

    private String formatMeetingTimes(List<MeetingTime> meetingTimes) {
        if (meetingTimes == null || meetingTimes.isEmpty()) {
            return "no schedule";
        }
        return meetingTimes.stream()
                .sorted(Comparator.comparing(MeetingTime::getDayOfWeek).thenComparing(MeetingTime::getStartTime))
                .map(item -> item.getDayOfWeek() + " " + item.getStartTime() + "-" + item.getEndTime()
                        + (item.getRoom() != null && !item.getRoom().isBlank() ? " " + item.getRoom() : ""))
                .collect(Collectors.joining(", "));
    }

    private String normalizeComponent(Grade grade) {
        String componentName = grade.getComponent() != null ? grade.getComponent().getName() : "";
        String comment = grade.getComment() != null ? grade.getComment() : "";
        return (componentName + " " + comment).toLowerCase();
    }

    private String systemPrompt() {
        return """
                You are the KBTU Portal teacher assistant.
                Respond in Russian unless the teacher clearly asks for English.
                Use only the provided teacher context. Do not invent grades, attendance data, students, or policies.
                Keep answers practical and operational.
                Focus on: students at risk, attendance problems, unpublished grades/finals, pending grade change requests, and section workload.
                If exact data is missing, say so clearly.
                Never claim you changed any record. This assistant is read-only.
                """;
    }

    private String formatDouble(double value) {
        return String.format(Locale.US, "%.2f", value);
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

    private static class StudentPerformance {
        private final Long id;
        private final String name;
        private final String email;
        private double attestation1;
        private double attestation2;
        private Double finalExam;
        private Double finalTotal;
        private String finalLetter;
        private int present;
        private int late;
        private int absent;
        private int totalAttendance;
        private int publishedGrades;
        private int riskScore;
        private final List<String> riskReasons = new ArrayList<>();

        private StudentPerformance(Long id, String name, String email) {
            this.id = id;
            this.name = name;
            this.email = email;
        }

        private void calculateRisk() {
            riskReasons.clear();
            riskScore = 0;
            if (totalAttendance >= 2) {
                double rate = ((present + late) * 100.0) / totalAttendance;
                if (rate < 75.0) {
                    riskReasons.add("attendance " + String.format(Locale.US, "%.1f%%", rate));
                    riskScore += 3;
                }
            }
            if (absent >= 2) {
                riskReasons.add("absent " + absent + " times");
                riskScore += 2;
            }
            double subtotal = attestation1 + attestation2;
            if (subtotal > 0 && subtotal < 35.0) {
                riskReasons.add("attestation subtotal " + String.format(Locale.US, "%.1f/60", subtotal));
                riskScore += 3;
            } else if (subtotal >= 35.0 && subtotal < 45.0) {
                riskReasons.add("attestation subtotal " + String.format(Locale.US, "%.1f/60", subtotal));
                riskScore += 1;
            }
            if (publishedGrades == 0) {
                riskReasons.add("no published grade records yet");
                riskScore += 1;
            }
        }

        private boolean isAtRisk() {
            return riskScore > 0;
        }

        private String attendanceRateText() {
            if (totalAttendance == 0) {
                return "no records";
            }
            double rate = ((present + late) * 100.0) / totalAttendance;
            return String.format(Locale.US, "%.1f%% (%d present, %d late, %d absent)", rate, present, late, absent);
        }

        private int riskScore() {
            return riskScore;
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
