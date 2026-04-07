package ru.kors.finalproject.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.kors.finalproject.entity.*;
import ru.kors.finalproject.repository.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentAssistantService {
    private static final int MAX_MESSAGE_LENGTH = 2000;

    private final GeminiClientService geminiClientService;
    private final RegistrationRepository registrationRepository;
    private final GradeRepository gradeRepository;
    private final FinalGradeRepository finalGradeRepository;
    private final AttendanceRepository attendanceRepository;
    private final HoldRepository holdRepository;
    private final StudentRequestRepository studentRequestRepository;
    private final ExamScheduleRepository examScheduleRepository;
    private final AuditService auditService;
    private final GpaCalculationService gpaCalculationService;
    private final AcademicAnalyticsService academicAnalyticsService;
    private final SubjectOfferingRepository subjectOfferingRepository;
    private final SemesterRepository semesterRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.ai.read-only:true}")
    private boolean readOnly;

    public StudentAssistantReply ask(Student student, String message) {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Question cannot be empty");
        }
        if (message.length() > MAX_MESSAGE_LENGTH) {
            throw new IllegalArgumentException("Question is too long");
        }

        String normalizedMessage = message.trim();
        StudentAssistantReply reply;
        if (isScheduleRecommendationRequest(normalizedMessage)) {
            reply = buildAiScheduleRecommendation(student, normalizedMessage);
        } else {
            StudentAssistantReply deterministicReply = tryAnswerDeterministically(student, normalizedMessage);
            if (deterministicReply != null) {
                reply = deterministicReply;
            } else {
                String context = buildStudentContext(student);
                try {
                    reply = wrapReply(
                            geminiClientService.generate(systemPrompt(), "Student context", context, normalizedMessage, 0.2, 900),
                            null
                    );
                } catch (GeminiClientService.GeminiQuotaExceededException ex) {
                    reply = buildQuotaReply(false);
                } catch (RuntimeException ex) {
                    throw new IllegalStateException("AI assistant is temporarily unavailable");
                }
            }
        }

        auditService.logStudentAction(
                student,
                "AI_ASSISTANT_QUERY",
                "StudentAssistant",
                student.getId(),
                "message=" + truncate(normalizedMessage, 180)
        );

        return reply;
    }

    public StudentAssistantReply buildDemoScheduleRecommendation(Student student) {
        return buildDeterministicScheduleRecommendation(
                student,
                "Make my next semester schedule after 12 and avoid Friday if possible.",
                "deterministic-schedule-demo"
        );
    }

    private StudentAssistantReply wrapReply(
            GeminiClientService.GeminiReply reply,
            ScheduleRecommendation recommendation
    ) {
        return new StudentAssistantReply(reply.answer(), reply.model(), reply.generatedAt(), recommendation);
    }

    private StudentAssistantReply buildAiScheduleRecommendation(Student student, String message) {
        return buildDeterministicScheduleRecommendation(student, message, "deterministic-schedule-planner");
    }

    private StudentAssistantReply buildQuotaReply(boolean scheduleRequest) {
        String answer = scheduleRequest
                ? """
                Дневной лимит Gemini API сейчас исчерпан.
                Данные для следующего семестра и доступные секции уже подготовлены, но AI пока не может собрать и проверить вариант расписания.
                Попробуйте позже: когда квота вернется, assistant снова сможет подобрать расписание без конфликтов и показать его визуально.
                """.trim()
                : """
                Дневной лимит Gemini API сейчас исчерпан.
                Сам assistant и ваши академические данные готовы, но новый AI-ответ появится только после сброса квоты.
                Попробуйте позже сегодня или завтра.
                """.trim();
        return new StudentAssistantReply(answer, "gemini-quota-limit", Instant.now(), null);
    }

    private StudentAssistantReply buildDeterministicScheduleRecommendation(Student student, String message, String model) {
        if (student.getCourse() >= 4) {
            return new StudentAssistantReply(
                    "Вы на 4 курсе или выше, поэтому на следующем семестре у вас обычно остаются дипломный проект, практика и защита. Обычное предметное расписание собирать не требуется.",
                    model,
                    Instant.now(),
                    null
            );
        }

        SchedulePlanningContext planningContext = buildSchedulePlanningContext(student);
        if (planningContext.semester() == null) {
            return new StudentAssistantReply(
                    "Я пока не нашел в данных следующий академический семестр, поэтому не могу собрать расписание.",
                    model,
                    Instant.now(),
                    null
            );
        }
        if (planningContext.availableSections().isEmpty()) {
            return new StudentAssistantReply(
                    "Я нашел следующий семестр `" + planningContext.semester().getName()
                            + "`, но для него пока нет доступных секций с временами занятий.",
                    model,
                    Instant.now(),
                    null
            );
        }

        StudentSchedulePlanningEngine.PlanningResult plan = StudentSchedulePlanningEngine.plan(
            message,
            planningContext.availableSections().stream()
                    .map(option -> new StudentSchedulePlanningEngine.CourseOption(
                            option.sectionId(),
                            option.courseCode(),
                            option.courseName(),
                            option.teacherName(),
                            option.lessonType(),
                            option.capacity(),
                            option.meetingTimes().stream()
                                    .map(slot -> new StudentSchedulePlanningEngine.MeetingOption(
                                            slot.dayOfWeek(),
                                            slot.startTime(),
                                            slot.endTime(),
                                            slot.room()
                                    ))
                                    .toList()
                    ))
                    .toList()
    );

    AiSchedulePlan plannerPlan = new AiSchedulePlan(
            plan.feasible(),
            plan.partial(),
            plan.chatResponse(),
            plan.summary(),
            plan.satisfiedPreferences(),
            plan.unsatisfiedPreferences(),
            plan.blockingCourses(),
            plan.selectedSectionIds(),
            List.of(),
            plan.warnings(),
            Map.of()
    );

    ScheduleRecommendation recommendation = toScheduleRecommendation(planningContext, plannerPlan);
    String answer = buildScheduleAnswer(recommendation, plannerPlan);
    return new StudentAssistantReply(answer, model, Instant.now(), recommendation);
}

private SchedulePlanningContext buildSchedulePlanningContext(Student student) {
        if (student.getCourse() >= 4) {
            return new SchedulePlanningContext(null, List.of());
        }
        Semester targetSemester = resolveNextSemester(student);
        if (targetSemester == null) {
            return new SchedulePlanningContext(null, List.of());
        }

        List<SubjectOffering> semesterOfferings = subjectOfferingRepository.findBySemesterIdWithDetails(targetSemester.getId()).stream()
                .filter(offering -> offering.getSubject() != null)
                .filter(offering -> student.getProgram() == null
                        || (offering.getSubject().getProgram() != null
                        && Objects.equals(offering.getSubject().getProgram().getId(), student.getProgram().getId())))
                .toList();

        Integer planningCourseYear = resolvePlanningCourseYear(student);
        List<SubjectOffering> offerings = semesterOfferings.stream()
                .filter(offering -> planningCourseYear == null
                        || matchesPlanningCourseYear(offering.getSubject().getCode(), planningCourseYear))
                .toList();
        if (offerings.isEmpty()) {
            offerings = semesterOfferings;
        }

        offerings = offerings.stream()
                .sorted(Comparator.comparing((SubjectOffering offering) -> offering.getSubject().getCode(), Comparator.nullsLast(String::compareTo))
                        .thenComparing(SubjectOffering::getId))
                .toList();

        List<ScheduleSectionOption> options = offerings.stream()
                .map(offering -> new ScheduleSectionOption(
                        offering.getId(),
                        offering.getSubject().getCode(),
                        offering.getSubject().getName(),
                        offering.getTeacher() != null ? offering.getTeacher().getName() : null,
                        offering.getLessonType() != null ? offering.getLessonType().name() : null,
                        offering.getCapacity(),
                        formatMeetingSlots(offering)
                ))
                .toList();

        return new SchedulePlanningContext(targetSemester, options);
    }

    private Integer resolvePlanningCourseYear(Student student) {
        if (student.getCourse() <= 0) {
            return null;
        }
        Semester currentSemester = student.getCurrentSemester();
        String currentSemesterName = currentSemester != null && currentSemester.getName() != null
                ? currentSemester.getName().toLowerCase(Locale.ROOT)
                : "";
        if (currentSemesterName.contains("spring")) {
            return student.getCourse() + 1;
        }
        return student.getCourse();
    }

    private boolean matchesPlanningCourseYear(String courseCode, int planningCourseYear) {
        Integer actualCourseYear = extractCourseYear(courseCode);
        return actualCourseYear == null || actualCourseYear == planningCourseYear;
    }

    private Integer extractCourseYear(String courseCode) {
        if (courseCode == null) {
            return null;
        }
        for (char ch : courseCode.toCharArray()) {
            if (Character.isDigit(ch)) {
                return Character.getNumericValue(ch);
            }
        }
        return null;
    }

    private Semester resolveNextSemester(Student student) {
        List<Semester> semesters = semesterRepository.findAll().stream()
                .sorted(Comparator.comparing(Semester::getStartDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        Semester studentCurrentSemester = student.getCurrentSemester();
        Semester current = studentCurrentSemester != null
                ? studentCurrentSemester
                : semesterRepository.findByCurrentTrue().orElse(null);

        if (current != null && current.getStartDate() != null) {
            Semester resolvedCurrent = current;
            Semester next = semesters.stream()
                    .filter(semester -> !Objects.equals(semester.getId(), resolvedCurrent.getId()))
                    .filter(semester -> semester.getStartDate() != null && semester.getStartDate().isAfter(resolvedCurrent.getStartDate()))
                    .findFirst()
                    .orElse(null);
            if (next != null) {
                return next;
            }
        }

        return semesters.stream().filter(Semester::isCurrent).findFirst().orElse(current);
    }

    private List<ScheduleMeetingSlot> formatMeetingSlots(SubjectOffering offering) {
        if (offering.getMeetingTimes() != null && !offering.getMeetingTimes().isEmpty()) {
            return offering.getMeetingTimes().stream()
                    .sorted(Comparator.comparing(MeetingTime::getDayOfWeek).thenComparing(MeetingTime::getStartTime))
                    .map(slot -> new ScheduleMeetingSlot(
                            slot.getDayOfWeek() != null ? slot.getDayOfWeek().name() : null,
                            slot.getStartTime() != null ? slot.getStartTime().toString() : null,
                            slot.getEndTime() != null ? slot.getEndTime().toString() : null,
                            slot.getRoom()
                    ))
                    .toList();
        }

        if (offering.getDayOfWeek() == null || offering.getStartTime() == null || offering.getEndTime() == null) {
            return List.of();
        }

        return List.of(new ScheduleMeetingSlot(
                offering.getDayOfWeek().name(),
                offering.getStartTime().toString(),
                offering.getEndTime().toString(),
                offering.getRoom()
        ));
    }

    private Map<String, Object> buildPlanningPromptPayload(Student student, SchedulePlanningContext planningContext) {
        List<Map<String, Object>> requiredCourses = planningContext.availableSections().stream()
                .collect(Collectors.toMap(
                        ScheduleSectionOption::courseCode,
                        option -> {
                            Map<String, Object> row = new LinkedHashMap<>();
                            row.put("courseCode", option.courseCode());
                            row.put("courseName", option.courseName());
                            return row;
                        },
                        (left, right) -> left,
                        LinkedHashMap::new
                ))
                .values()
                .stream()
                .toList();

        List<Map<String, Object>> availableSections = planningContext.availableSections().stream()
                .map(option -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("courseCode", option.courseCode());
                    row.put("courseName", option.courseName());
                    row.put("sectionId", option.sectionId());
                    row.put("teacherName", option.teacherName());
                    row.put("lessonType", option.lessonType());
                    row.put("capacity", option.capacity());
                    row.put("meetingTimes", option.meetingTimes());
                    return row;
                })
                .toList();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("studentName", student.getName());
        payload.put("program", student.getProgram() != null ? student.getProgram().getName() : null);
        payload.put("faculty", student.getFaculty() != null ? student.getFaculty().getName() : null);
        payload.put("targetSemester", planningContext.semester().getName());
        payload.put("requiredCourses", requiredCourses);
        payload.put("availableSections", availableSections);
        payload.put("rules", List.of(
                "Select at most one section per required course.",
                "Do not invent sections or times.",
                "Do not create time conflicts.",
                "If all preferences cannot be satisfied, keep the schedule conflict-free and satisfy as many preferences as possible.",
                "If one or more courses make a preference impossible, explicitly list them in blockingCourses."
        ));
        return payload;
    }

    private AiSchedulePlanResult requestAiSchedulePlan(String message, String planningJson) {
        GeminiClientService.GeminiReply reply = geminiClientService.generateJson(
                schedulePlannerPrompt(),
                "Schedule planning input JSON",
                planningJson,
                message,
                0.15,
                1800
        );
        try {
            return new AiSchedulePlanResult(reply, parseAiJson(reply.answer(), AiSchedulePlan.class));
        } catch (IllegalStateException ex) {
            GeminiClientService.GeminiReply normalizedReply = geminiClientService.generateJson(
                    scheduleJsonNormalizationPrompt(),
                    "Raw schedule answer",
                    reply.answer(),
                    "Student request:\n" + message,
                    0.05,
                    1800
            );
            return new AiSchedulePlanResult(normalizedReply, parseAiJson(normalizedReply.answer(), AiSchedulePlan.class));
        }
    }

    private AiValidatorResult validateAiSchedulePlan(String message, String planningJson, AiSchedulePlan plan) {
        GeminiClientService.GeminiReply reply = geminiClientService.generateJson(
                scheduleValidatorPrompt(),
                "Original planning JSON",
                planningJson,
                "Student request:\n" + message + "\n\nProposed schedule JSON:\n" + toJson(plan),
                0.05,
                700
        );
        return parseAiJson(reply.answer(), AiValidatorResult.class);
    }

    private AiSchedulePlanResult requestAiSectionSelectionPlan(String message, String planningJson) {
        GeminiClientService.GeminiReply reply = geminiClientService.generateJson(
                scheduleSectionSelectionPrompt(),
                "Schedule planning input JSON",
                planningJson,
                message,
                0.1,
                1200
        );
        try {
            AiSchedulePlan plan = parseAiJson(reply.answer(), AiSchedulePlan.class);
            return new AiSchedulePlanResult(reply, plan);
        } catch (IllegalStateException ex) {
            GeminiClientService.GeminiReply normalizedReply = geminiClientService.generateJson(
                    scheduleSectionIdNormalizationPrompt(),
                    "Raw schedule answer",
                    reply.answer(),
                    "Student request:\n" + message,
                    0.05,
                    700
            );
            return new AiSchedulePlanResult(normalizedReply, parseAiJson(normalizedReply.answer(), AiSchedulePlan.class));
        }
    }

    private AiSchedulePlanResult repairAiSchedulePlan(String message, String planningJson, AiSchedulePlan plan, List<String> errors) {
        GeminiClientService.GeminiReply reply = geminiClientService.generateJson(
                scheduleRepairPrompt(),
                "Original planning JSON",
                planningJson,
                "Student request:\n" + message + "\n\nInvalid schedule JSON:\n" + toJson(plan) + "\n\nValidator errors:\n" + toJson(errors),
                0.1,
                1800
        );
        return new AiSchedulePlanResult(reply, parseAiJson(reply.answer(), AiSchedulePlan.class));
    }

    private AiSchedulePlanResult requestAiSectionSelectionRepairPlan(String message, String planningJson, List<String> errors) {
        return requestAiSectionSelectionPlan(
                message + "\n\nAvoid these validator errors:\n- " + String.join("\n- ", sanitizeStrings(errors)),
                planningJson
        );
    }

    private ScheduleRecommendation toScheduleRecommendation(SchedulePlanningContext planningContext, AiSchedulePlan plan) {
        List<SelectedSection> selectedSections = hydrateSelectedSections(planningContext, plan);
        Map<String, List<VisualScheduleItem>> visual = plan.visualSchedule() != null && !plan.visualSchedule().isEmpty()
                ? plan.visualSchedule().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> sanitizeVisualItems(entry.getValue()),
                        (left, right) -> left,
                        LinkedHashMap::new
                ))
                : buildVisualScheduleFromSections(selectedSections);

        return new ScheduleRecommendation(
                planningContext.semester() != null ? planningContext.semester().getName() : null,
                plan.feasible(),
                plan.partial(),
                blankToNull(plan.summary()),
                sanitizeStrings(plan.satisfiedPreferences()),
                sanitizeStrings(plan.unsatisfiedPreferences()),
                sanitizeStrings(plan.blockingCourses()),
                selectedSections,
                sanitizeStrings(plan.warnings()),
                visual
        );
    }

    private List<SelectedSection> hydrateSelectedSections(SchedulePlanningContext planningContext, AiSchedulePlan plan) {
        List<SelectedSection> explicitSections = sanitizeSections(plan.selectedSections());
        if (!explicitSections.isEmpty()) {
            return explicitSections;
        }

        List<Long> sectionIds = sanitizeSectionIds(plan.selectedSectionIds());
        if (sectionIds.isEmpty()) {
            return List.of();
        }

        Map<Long, ScheduleSectionOption> byId = planningContext.availableSections().stream()
                .collect(Collectors.toMap(
                        ScheduleSectionOption::sectionId,
                        option -> option,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        return sectionIds.stream()
                .map(byId::get)
                .filter(Objects::nonNull)
                .map(option -> new SelectedSection(
                        option.courseCode(),
                        option.courseName(),
                        option.sectionId(),
                        blankToNull(option.teacherName()),
                        option.meetingTimes().stream()
                                .map(slot -> new MeetingTimeView(slot.dayOfWeek(), slot.startTime(), slot.endTime(), blankToNull(slot.room())))
                                .toList()
                ))
                .toList();
    }

    private AiSchedulePlan mergePlanWarnings(AiSchedulePlan plan, List<String> additionalWarnings) {
        List<String> warnings = new ArrayList<>(sanitizeStrings(plan.warnings()));
        warnings.addAll(sanitizeStrings(additionalWarnings));
        return new AiSchedulePlan(
                plan.feasible(),
                true,
                plan.chatResponse(),
                plan.summary(),
                plan.satisfiedPreferences(),
                plan.unsatisfiedPreferences(),
                plan.blockingCourses(),
                plan.selectedSectionIds(),
                plan.selectedSections(),
                warnings,
                plan.visualSchedule()
        );
    }

    private Map<String, List<VisualScheduleItem>> buildVisualScheduleFromSections(List<SelectedSection> sections) {
        Map<String, List<VisualScheduleItem>> visual = new LinkedHashMap<>();
        for (String day : List.of("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY")) {
            visual.put(day, new ArrayList<>());
        }

        for (SelectedSection section : sections == null ? List.<SelectedSection>of() : sections) {
            for (MeetingTimeView slot : section.meetingTimes() == null ? List.<MeetingTimeView>of() : section.meetingTimes()) {
                if (slot.dayOfWeek() == null || slot.startTime() == null || slot.endTime() == null) {
                    continue;
                }
                visual.computeIfAbsent(slot.dayOfWeek(), key -> new ArrayList<>()).add(new VisualScheduleItem(
                        section.courseCode(),
                        section.courseName(),
                        slot.startTime(),
                        slot.endTime(),
                        slot.room(),
                        section.teacherName()
                ));
            }
        }

        visual.replaceAll((day, items) -> items.stream()
                .sorted(Comparator.comparing(VisualScheduleItem::startTime, Comparator.nullsLast(String::compareTo)))
                .toList());
        return visual;
    }

    private String buildScheduleAnswer(ScheduleRecommendation recommendation, AiSchedulePlan plan) {
        StringBuilder answer = new StringBuilder();
        if (plan.chatResponse() != null && !plan.chatResponse().isBlank()) {
            answer.append(plan.chatResponse().trim());
        } else if (recommendation.summary() != null) {
            answer.append(recommendation.summary().trim());
        } else {
            answer.append("I prepared a next-semester schedule recommendation based on the available section times.");
        }

        if (!recommendation.warnings().isEmpty()) {
            answer.append("\n\nWarnings:");
            recommendation.warnings().forEach(warning -> answer.append("\n- ").append(warning));
        }

        if (!recommendation.unsatisfiedPreferences().isEmpty()) {
            answer.append("\n\nCould not fully satisfy:");
            recommendation.unsatisfiedPreferences().forEach(item -> answer.append("\n- ").append(item));
        }

        if (!recommendation.blockingCourses().isEmpty()) {
            answer.append("\n\nBlocking courses:");
            recommendation.blockingCourses().forEach(item -> answer.append("\n- ").append(item));
        }

        return answer.toString().trim();
    }

    private boolean isScheduleRecommendationRequest(String message) {
    String normalized = message.toLowerCase(Locale.ROOT);
    boolean mentionsSchedule = containsAny(normalized,
            "schedule", "timetable", "class time", "class times",
            "\u0440\u0430\u0441\u043f\u0438\u0441", "\u0432\u0440\u0435\u043c\u044f \u0443\u0440\u043e\u043a", "\u0432\u0440\u0435\u043c\u044f \u043f\u0430\u0440", "\u043f\u0430\u0440\u044b", "\u0443\u0440\u043e\u043a\u0438");
    boolean mentionsIntent = containsAny(normalized,
            "make", "build", "plan", "arrange", "recommend", "compose",
            "\u0441\u0434\u0435\u043b\u0430\u0439", "\u0441\u043e\u0441\u0442\u0430\u0432", "\u043f\u043e\u0434\u0431\u0435\u0440\u0438", "\u0441\u043e\u0431\u0435\u0440\u0438", "\u043f\u043e\u0441\u0442\u0440\u043e\u0439", "\u043f\u0440\u0435\u0434\u043b\u043e\u0436");
    boolean mentionsPreference = containsAny(normalized,
            "after 12", "after noon", "after lunch", "avoid friday", "avoid saturday", "no conflicts", "compact",
            "\u043f\u043e\u0441\u043b\u0435 12", "\u043f\u043e\u0441\u043b\u0435 \u043e\u0431\u0435\u0434\u0430", "\u0431\u0435\u0437 \u043f\u044f\u0442\u043d\u0438\u0446\u044b", "\u0431\u0435\u0437 \u0441\u0443\u0431\u0431\u043e\u0442\u044b", "\u0431\u0435\u0437 \u043a\u043e\u043d\u0444\u043b\u0438\u043a\u0442", "\u0431\u0435\u0437 \u043e\u043a\u043e\u043d", "\u043a\u043e\u043c\u043f\u0430\u043a\u0442");
    boolean mentionsNextTerm = containsAny(normalized,
            "next semester", "next term", "\u0441\u043b\u0435\u0434\u0443\u044e\u0449", "\u0441\u043b\u0435\u0434 \u0441\u0435\u043c", "\u0441\u043b\u0435\u0434\u0443\u044e\u0449\u0438\u0439 \u0441\u0435\u043c\u0435\u0441\u0442\u0440");
    boolean mentionsDaySpecificTiming = containsAny(normalized,
            "\u043f\u043e\u043d\u0435\u0434", "\u0432\u0442\u043e\u0440", "\u0441\u0440\u0435\u0434", "\u0447\u0435\u0442\u0432", "\u043f\u044f\u0442", "\u0441\u0443\u0431",
            "monday", "tuesday", "wednesday", "thursday", "friday", "saturday")
            && containsAny(normalized,
            "\u043f\u043e\u0441\u043b\u0435", "\u0434\u043e", "\u0443\u0442\u0440\u043e\u043c", "\u0441 \u0443\u0442\u0440\u0430", "\u0432\u0435\u0447\u0435\u0440\u043e\u043c", "after", "before", "morning", "evening", "other days", "\u043e\u0441\u0442\u0430\u043b\u044c\u043d\u044b\u0435 \u0434\u043d\u0438");
    return (mentionsSchedule && (mentionsIntent || mentionsPreference || mentionsNextTerm))
            || (mentionsIntent && mentionsPreference)
            || (mentionsSchedule && mentionsPreference)
            || mentionsDaySpecificTiming;
}

private boolean containsAny(String text, String... tokens) {
        for (String token : tokens) {
            if (text.contains(token)) {
                return true;
            }
        }
        return false;
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
        AcademicAnalyticsService.StudentPlannerDashboard plannerDashboard = academicAnalyticsService.buildStudentPlannerDashboard(student);

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

        double publishedGpa = gpaCalculationService.calculatePublishedGpa(publishedFinalGrades);

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
        context.append("Maximum projected GPA if all remaining finals are 40/40: ")
                .append(formatDouble(plannerDashboard.maxProjectionGpa())).append("\n");
        context.append("Planner courses available: ").append(plannerDashboard.courses().size()).append("\n");
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
                Respond in %s unless the user clearly asks for another language.
                Use only the provided student context. Do not invent policies, grades, deadlines, or attendance records.
                If the needed data is missing, say so clearly.
                Keep answers practical and concise, but explain calculations when the student asks about scores or GPA.
                Grade rule: Attestation 1 max 30, Attestation 2 max 30, Final max 40, Total max 100.
                Formula: needed final score = target total - attestation1 - attestation2.
                If needed final score is above 40, say the target is not reachable.
                If needed final score is 0 or below, say the target is already secured.
                For GPA questions, use both the published GPA and any planner values in the context. If the context includes a maximum projected GPA, you may answer directly with it.
                Never claim you changed data. This assistant is read-only.
                """.formatted(geminiClientService.getLocale());
    }

    private String schedulePlannerPrompt() {
        return """
                You are an academic schedule planning assistant for a university portal.
                Respond in %s unless the user clearly asked for another language.

                Build the best possible next-semester schedule using ONLY the provided courses, section options, and meeting times.

                Rules:
                - Select at most one section per required course.
                - Prefer returning selectedSectionIds that reference the original input, even if you also return selectedSections.
                - Do not invent courses, sections, teachers, rooms, or times.
                - Do not modify meeting times.
                - Do not include conflicting sections.
                - A time conflict means overlapping day/time between any selected meeting slots.
                - Respect the user's preferences as much as possible.
                - If all preferences cannot be satisfied, keep the schedule conflict-free and satisfy as many preferences as possible.
                - If one or more courses make a preference impossible, explicitly say which course caused that.
                - If no valid full schedule exists, return the best partial schedule and explain why.
                - If a valid schedule exists, selectedSectionIds must not be empty.
                - Before finalizing, silently re-check conflicts, duplicate course selections, and unmet preferences yourself.

                Return ONLY valid JSON in this exact shape:
                %s
                """.formatted(geminiClientService.getLocale(), scheduleJsonSchema());
    }

    private String scheduleSectionSelectionPrompt() {
        return """
                You are an academic schedule planning assistant for a university portal.
                Respond in %s unless the user clearly asked for another language.

                Choose the best next-semester section combination using ONLY the provided section IDs.
                Do not invent IDs. Do not include duplicate courses. Avoid time conflicts. Respect user preferences as much as possible.
                If a valid schedule exists, selectedSectionIds must contain one or more real section IDs from the input.
                If a preference is impossible, still return the best conflict-free section ID combination and explain what could not be satisfied.
                Keep the JSON compact. Do not include markdown, long prose, or extra explanation outside the required fields.

                Return ONLY valid JSON in this exact shape:
                %s
                """.formatted(geminiClientService.getLocale(), scheduleSectionIdSchema());
    }

    private String scheduleValidatorPrompt() {
        return """
                You are a strict schedule validator.
                Respond with JSON only.

                Validate the proposed schedule against the original input data.

                Rules:
                - Do not trust the proposed answer automatically.
                - Verify that each selected section exists in the input.
                - Verify that no selected meeting times overlap.
                - Verify that no course was selected more than once.
                - Verify whether the stated preferences are actually satisfied.
                - If invalid, list exact problems.

                Return ONLY:
                {
                  "valid": true,
                  "errors": []
                }
                """;
    }

    private String scheduleRepairPrompt() {
        return """
                You are an academic schedule planning assistant repairing a previously invalid schedule.
                Respond in %s unless the user clearly asked for another language.

                Fix the invalid schedule using the validator errors below.
                Do not repeat the same mistake.
                Keep the schedule conflict-free.
                If full feasibility is impossible, return a partial schedule and explain why.
                Return ONLY valid JSON in this exact shape:
                %s
                """.formatted(geminiClientService.getLocale(), scheduleJsonSchema());
    }

    private String scheduleJsonNormalizationPrompt() {
        return """
                You are a strict JSON formatter for academic schedule plans.
                Respond with JSON only.

                Take the raw schedule answer and convert it into the exact schedule schema requested earlier.
                Do not invent courses, sections, teachers, rooms, or times.
                Preserve the original plan intent, but make the output valid JSON that matches the required schema.
                If the raw answer is incomplete, keep unknown lists empty instead of inventing data.
                Required JSON schema:
                %s
                """.formatted(scheduleJsonSchema());
    }

    private String scheduleSectionIdNormalizationPrompt() {
        return """
                You are a strict JSON formatter for compact academic section selection plans.
                Respond with JSON only.

                Take the raw section-selection answer and convert it into the exact compact schema requested earlier.
                Do not invent section IDs or preferences.
                Preserve the original plan intent, but make the output valid JSON that matches the required schema.
                If the raw answer is incomplete, keep unknown lists empty instead of inventing data.
                Required JSON schema:
                %s
                """.formatted(scheduleSectionIdSchema());
    }

    private String scheduleJsonSchema() {
        return """
                {
                  "feasible": true,
                  "partial": false,
                  "chatResponse": "human readable explanation",
                  "summary": "short summary",
                  "satisfiedPreferences": [],
                  "unsatisfiedPreferences": [],
                  "blockingCourses": [],
                  "selectedSectionIds": [0],
                  "selectedSections": [
                    {
                      "courseCode": "",
                      "courseName": "",
                      "sectionId": 0,
                      "teacherName": "",
                      "meetingTimes": [
                        {
                          "dayOfWeek": "",
                          "startTime": "",
                          "endTime": "",
                          "room": ""
                        }
                      ]
                    }
                  ],
                  "warnings": [],
                  "visualSchedule": {
                    "MONDAY": [],
                    "TUESDAY": [],
                    "WEDNESDAY": [],
                    "THURSDAY": [],
                    "FRIDAY": [],
                    "SATURDAY": []
                  }
                }
                """;
    }

    private String scheduleSectionIdSchema() {
        return """
                {
                  "feasible": true,
                  "partial": false,
                  "chatResponse": "Human-readable explanation of the schedule in the requested language",
                  "summary": "One-line summary",
                  "satisfiedPreferences": ["preference that was honored"],
                  "unsatisfiedPreferences": ["preference that could not be honored"],
                  "blockingCourses": ["courseCode that blocks a preference"],
                  "selectedSectionIds": [101, 205, 310],
                  "warnings": ["any important note"]
                }

                IMPORTANT: selectedSectionIds MUST contain real sectionId values from the availableSections input.
                Do NOT use placeholder values like 0. Pick exactly one sectionId per required course.
                """;
    }

    private boolean planHasNoSelections(AiSchedulePlan plan) {
        if (plan == null) {
            return true;
        }
        return sanitizeSectionIds(plan.selectedSectionIds()).isEmpty()
                && sanitizeSections(plan.selectedSections()).isEmpty();
    }

    private List<String> validateScheduleLocally(SchedulePlanningContext context, AiSchedulePlan plan) {
        List<String> errors = new ArrayList<>();
        List<Long> sectionIds = sanitizeSectionIds(plan.selectedSectionIds());
        Map<Long, ScheduleSectionOption> byId = context.availableSections().stream()
                .collect(Collectors.toMap(ScheduleSectionOption::sectionId, o -> o, (a, b) -> a));

        // Check all IDs exist
        for (Long id : sectionIds) {
            if (!byId.containsKey(id)) {
                errors.add("Section ID " + id + " not found in available offerings");
            }
        }

        // Check no duplicate courses
        Map<String, Long> courseToSection = new LinkedHashMap<>();
        for (Long id : sectionIds) {
            ScheduleSectionOption opt = byId.get(id);
            if (opt != null && opt.courseCode() != null) {
                Long prev = courseToSection.put(opt.courseCode(), id);
                if (prev != null) {
                    errors.add("Duplicate course: " + opt.courseCode());
                }
            }
        }

        // Check for time conflicts
        List<ScheduleSectionOption> selected = sectionIds.stream().map(byId::get).filter(Objects::nonNull).toList();
        for (int i = 0; i < selected.size(); i++) {
            for (int j = i + 1; j < selected.size(); j++) {
                if (hasTimeConflict(selected.get(i), selected.get(j))) {
                    errors.add("Time conflict between " + selected.get(i).courseCode() + " and " + selected.get(j).courseCode());
                }
            }
        }
        return errors;
    }

    private boolean hasTimeConflict(ScheduleSectionOption a, ScheduleSectionOption b) {
        for (ScheduleMeetingSlot slotA : a.meetingTimes()) {
            for (ScheduleMeetingSlot slotB : b.meetingTimes()) {
                if (slotA.dayOfWeek() != null && slotA.dayOfWeek().equals(slotB.dayOfWeek())
                        && slotA.startTime() != null && slotA.endTime() != null
                        && slotB.startTime() != null && slotB.endTime() != null
                        && slotA.startTime().compareTo(slotB.endTime()) < 0
                        && slotB.startTime().compareTo(slotA.endTime()) < 0) {
                    return true;
                }
            }
        }
        return false;
    }

    private StudentAssistantReply tryAnswerDeterministically(Student student, String message) {
        String normalized = message.toLowerCase(Locale.ROOT);
        boolean asksAboutGpa = normalized.contains("gpa") || normalized.contains("\u0433\u043f\u0430");
        boolean asksAboutMaximum = normalized.contains("\u043c\u0430\u043a\u0441\u0438\u043c")
                || normalized.contains("maximum")
                || normalized.contains("highest")
                || normalized.contains("best possible")
                || normalized.contains("\u0438\u0434\u0435\u0430\u043b\u044c")
                || normalized.contains("\u043c\u0430\u043a\u0441");
        boolean asksAboutPerfectScores = normalized.contains("\u043c\u0430\u043a\u0441\u0438\u043c\u0430\u043b\u044c\u043d\u044b\u0435 \u0431\u0430\u043b\u043b\u044b")
                || normalized.contains("maximum scores")
                || normalized.contains("perfect scores")
                || normalized.contains("\u0442\u043e\u043b\u044c\u043a\u043e \u043c\u0430\u043a\u0441\u0438\u043c\u0430\u043b\u044c\u043d\u044b\u0435")
                || normalized.contains("40/40")
                || normalized.contains("100/100");
        if (!asksAboutGpa || (!asksAboutMaximum && !asksAboutPerfectScores)) {
            return null;
        }
        AcademicAnalyticsService.StudentPlannerDashboard planner = academicAnalyticsService.buildStudentPlannerDashboard(student);
        if (planner.courses().isEmpty()) {
            String answer = """
                    \u0423 \u0432\u0430\u0441 \u0441\u0435\u0439\u0447\u0430\u0441 \u043d\u0435\u0442 \u0430\u043a\u0442\u0438\u0432\u043d\u044b\u0445 \u043a\u0443\u0440\u0441\u043e\u0432 \u0432 planner, \u043f\u043e\u044d\u0442\u043e\u043c\u0443 \u043c\u0430\u043a\u0441\u0438\u043c\u0430\u043b\u044c\u043d\u044b\u0439 \u043f\u0440\u043e\u0433\u043d\u043e\u0437\u0438\u0440\u0443\u0435\u043c\u044b\u0439 GPA \u0441\u043e\u0432\u043f\u0430\u0434\u0430\u0435\u0442 \u0441 \u0443\u0436\u0435 \u043e\u043f\u0443\u0431\u043b\u0438\u043a\u043e\u0432\u0430\u043d\u043d\u044b\u043c GPA: %s.
                    """.formatted(formatDouble(planner.currentPublishedGpa()));
            return new StudentAssistantReply(answer.trim(), "deterministic-planner", Instant.now(), null);
        }
        long coursesWithoutPublishedFinal = planner.courses().stream()
                .filter(course -> course.publishedFinal() == null)
                .count();
        String answer = """
                \u0415\u0441\u043b\u0438 \u043f\u043e \u0432\u0441\u0435\u043c \u043e\u0441\u0442\u0430\u0432\u0448\u0438\u043c\u0441\u044f \u043f\u0440\u0435\u0434\u043c\u0435\u0442\u0430\u043c \u043d\u0430\u0431\u0440\u0430\u0442\u044c \u043c\u0430\u043a\u0441\u0438\u043c\u0430\u043b\u044c\u043d\u044b\u0439 \u0444\u0438\u043d\u0430\u043b 40/40, \u0432\u0430\u0448 \u043c\u0430\u043a\u0441\u0438\u043c\u0430\u043b\u044c\u043d\u044b\u0439 \u043f\u0440\u043e\u0433\u043d\u043e\u0437\u0438\u0440\u0443\u0435\u043c\u044b\u0439 GPA \u0431\u0443\u0434\u0435\u0442 %s.
                \u0421\u0435\u0439\u0447\u0430\u0441 \u043e\u043f\u0443\u0431\u043b\u0438\u043a\u043e\u0432\u0430\u043d\u043d\u044b\u0439 GPA: %s.
                \u041a\u0443\u0440\u0441\u043e\u0432 \u0432 \u0442\u0435\u043a\u0443\u0449\u0435\u043c planner: %d.
                \u041a\u0443\u0440\u0441\u043e\u0432 \u0431\u0435\u0437 \u043e\u043f\u0443\u0431\u043b\u0438\u043a\u043e\u0432\u0430\u043d\u043d\u043e\u0433\u043e \u0444\u0438\u043d\u0430\u043b\u0430: %d.
                \u042d\u0442\u043e \u0440\u0430\u0441\u0447\u0435\u0442 \u043f\u043e \u0442\u0435\u043a\u0443\u0449\u0438\u043c \u0430\u0442\u0442\u0435\u0441\u0442\u0430\u0446\u0438\u044f\u043c \u0438 \u043f\u0440\u0435\u0434\u043f\u043e\u043b\u043e\u0436\u0435\u043d\u0438\u044e, \u0447\u0442\u043e \u0432\u0441\u0435 \u043e\u0441\u0442\u0430\u0432\u0448\u0438\u0435\u0441\u044f \u0444\u0438\u043d\u0430\u043b\u044b \u0431\u0443\u0434\u0443\u0442 \u043c\u0430\u043a\u0441\u0438\u043c\u0430\u043b\u044c\u043d\u044b\u043c\u0438.
                """.formatted(
                formatDouble(planner.maxProjectionGpa()),
                formatDouble(planner.currentPublishedGpa()),
                planner.courses().size(),
                coursesWithoutPublishedFinal
        );
        return new StudentAssistantReply(answer.trim(), "deterministic-planner", Instant.now(), null);
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

    private List<SelectedSection> sanitizeSections(List<AiSelectedSection> sections) {
        if (sections == null) {
            return List.of();
        }
        return sections.stream()
                .filter(section -> section.courseCode() != null && section.sectionId() != null)
                .map(section -> new SelectedSection(
                        section.courseCode(),
                        section.courseName(),
                        section.sectionId(),
                        blankToNull(section.teacherName()),
                        sanitizeMeetingTimes(section.meetingTimes())
                ))
                .toList();
    }

    private List<MeetingTimeView> sanitizeMeetingTimes(List<AiMeetingTime> meetingTimes) {
        if (meetingTimes == null) {
            return List.of();
        }
        return meetingTimes.stream()
                .filter(item -> item.dayOfWeek() != null && item.startTime() != null && item.endTime() != null)
                .map(item -> new MeetingTimeView(item.dayOfWeek(), item.startTime(), item.endTime(), blankToNull(item.room())))
                .toList();
    }

    private List<Long> sanitizeSectionIds(List<Long> sectionIds) {
        if (sectionIds == null) {
            return List.of();
        }
        return sectionIds.stream()
                .filter(Objects::nonNull)
                .filter(id -> id > 0)
                .distinct()
                .toList();
    }

    private List<VisualScheduleItem> sanitizeVisualItems(List<VisualScheduleItem> items) {
        if (items == null) {
            return List.of();
        }
        return items.stream()
                .filter(Objects::nonNull)
                .filter(item -> item.courseCode() != null && item.startTime() != null && item.endTime() != null)
                .toList();
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize AI context");
        }
    }

    private <T> T parseAiJson(String text, Class<T> type) {
        String json = extractJson(text);
        try {
            return objectMapper.readerFor(type)
                    .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .readValue(json);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("AI returned invalid structured schedule data");
        }
    }

    private String extractJson(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalStateException("AI returned empty structured schedule data");
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

    private record SchedulePlanningContext(Semester semester, List<ScheduleSectionOption> availableSections) {}

    private record ScheduleSectionOption(
            Long sectionId,
            String courseCode,
            String courseName,
            String teacherName,
            String lessonType,
            int capacity,
            List<ScheduleMeetingSlot> meetingTimes
    ) {}

    private record ScheduleMeetingSlot(String dayOfWeek, String startTime, String endTime, String room) {}

    private record AiSchedulePlanResult(GeminiClientService.GeminiReply reply, AiSchedulePlan plan) {}

    private record AiSchedulePlan(
            boolean feasible,
            boolean partial,
            String chatResponse,
            String summary,
            List<String> satisfiedPreferences,
            List<String> unsatisfiedPreferences,
            List<String> blockingCourses,
            List<Long> selectedSectionIds,
            List<AiSelectedSection> selectedSections,
            List<String> warnings,
            Map<String, List<VisualScheduleItem>> visualSchedule
    ) {}

    private record AiSelectedSection(
            String courseCode,
            String courseName,
            Long sectionId,
            String teacherName,
            List<AiMeetingTime> meetingTimes
    ) {}

    private record AiMeetingTime(String dayOfWeek, String startTime, String endTime, String room) {}

    private record AiValidatorResult(boolean valid, List<String> errors) {
        private AiValidatorResult {
            errors = errors == null ? List.of() : errors;
        }
    }

    public record StudentAssistantReply(
            String answer,
            String model,
            Instant generatedAt,
            ScheduleRecommendation scheduleRecommendation
    ) {}

    public record ScheduleRecommendation(
            String semesterName,
            boolean feasible,
            boolean partial,
            String summary,
            List<String> satisfiedPreferences,
            List<String> unsatisfiedPreferences,
            List<String> blockingCourses,
            List<SelectedSection> selectedSections,
            List<String> warnings,
            Map<String, List<VisualScheduleItem>> visualSchedule
    ) {}

    public record SelectedSection(
            String courseCode,
            String courseName,
            Long sectionId,
            String teacherName,
            List<MeetingTimeView> meetingTimes
    ) {}

    public record MeetingTimeView(String dayOfWeek, String startTime, String endTime, String room) {}

    public record VisualScheduleItem(
            String courseCode,
            String courseName,
            String startTime,
            String endTime,
            String room,
            String teacherName
    ) {}

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
}



