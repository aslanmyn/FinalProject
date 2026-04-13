package ru.kors.finalproject.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.kors.finalproject.entity.Program;
import ru.kors.finalproject.entity.Semester;
import ru.kors.finalproject.entity.Student;
import ru.kors.finalproject.entity.Subject;
import ru.kors.finalproject.entity.SubjectOffering;
import ru.kors.finalproject.entity.Teacher;
import ru.kors.finalproject.repository.AttendanceRepository;
import ru.kors.finalproject.repository.ExamScheduleRepository;
import ru.kors.finalproject.repository.FinalGradeRepository;
import ru.kors.finalproject.repository.GradeRepository;
import ru.kors.finalproject.repository.HoldRepository;
import ru.kors.finalproject.repository.RegistrationRepository;
import ru.kors.finalproject.repository.SemesterRepository;
import ru.kors.finalproject.repository.StudentRequestRepository;
import ru.kors.finalproject.repository.SubjectOfferingRepository;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentAssistantServiceTest {

    @Mock
    private GeminiClientService geminiClientService;
    @Mock
    private RegistrationRepository registrationRepository;
    @Mock
    private GradeRepository gradeRepository;
    @Mock
    private FinalGradeRepository finalGradeRepository;
    @Mock
    private AttendanceRepository attendanceRepository;
    @Mock
    private HoldRepository holdRepository;
    @Mock
    private StudentRequestRepository studentRequestRepository;
    @Mock
    private ExamScheduleRepository examScheduleRepository;
    @Mock
    private AuditService auditService;
    @Mock
    private GpaCalculationService gpaCalculationService;
    @Mock
    private AcademicAnalyticsService academicAnalyticsService;
    @Mock
    private SubjectOfferingRepository subjectOfferingRepository;
    @Mock
    private SemesterRepository semesterRepository;

    private StudentAssistantService service;
    private Student student;
    private Program program;
    private Semester currentSemester;
    private Semester nextSemester;

    @BeforeEach
    void setUp() {
        service = new StudentAssistantService(
                geminiClientService,
                registrationRepository,
                gradeRepository,
                finalGradeRepository,
                attendanceRepository,
                holdRepository,
                studentRequestRepository,
                examScheduleRepository,
                auditService,
                gpaCalculationService,
                academicAnalyticsService,
                subjectOfferingRepository,
                semesterRepository
        );

        program = Program.builder()
                .id(10L)
                .name("Computer Science")
                .build();

        currentSemester = Semester.builder()
                .id(1L)
                .name("2025-2026 Fall")
                .startDate(LocalDate.of(2025, 9, 1))
                .endDate(LocalDate.of(2025, 12, 20))
                .current(true)
                .build();

        nextSemester = Semester.builder()
                .id(2L)
                .name("2025-2026 Spring")
                .startDate(LocalDate.of(2026, 1, 15))
                .endDate(LocalDate.of(2026, 5, 25))
                .current(false)
                .build();

        student = Student.builder()
                .id(101L)
                .email("a_student@kbtu.kz")
                .name("Aruzhan Student")
                .program(program)
                .currentSemester(currentSemester)
                .course(2)
                .build();
    }

    @Test
    @DisplayName("schedule request is handled by deterministic planner and respects day-specific preferences")
    void ask_scheduleRecommendationRequest_returnsStructuredDeterministicSchedule() {
        Teacher teacher = Teacher.builder()
                .id(30L)
                .name("Aidos Nurgaliyev")
                .build();

        Subject mondayCourse = Subject.builder().id(20L).code("CSCI2107").name("Computer Networks").program(program).build();
        Subject fridayCourse = Subject.builder().id(21L).code("MATH2201").name("Discrete Mathematics").program(program).build();
        Subject tuesdayCourse = Subject.builder().id(22L).code("ENGL2101").name("Academic English").program(program).build();

        SubjectOffering mondayLate = offering(8L, mondayCourse, teacher, DayOfWeek.MONDAY, 16, 18, "L-430");
        SubjectOffering mondayMorning = offering(9L, mondayCourse, teacher, DayOfWeek.MONDAY, 9, 11, "L-431");
        SubjectOffering fridayLate = offering(10L, fridayCourse, teacher, DayOfWeek.FRIDAY, 15, 17, "M-201");
        SubjectOffering fridayMorning = offering(11L, fridayCourse, teacher, DayOfWeek.FRIDAY, 9, 11, "M-202");
        SubjectOffering tuesdayMorning = offering(12L, tuesdayCourse, teacher, DayOfWeek.TUESDAY, 9, 10, "P-101");
        SubjectOffering tuesdayAfternoon = offering(13L, tuesdayCourse, teacher, DayOfWeek.TUESDAY, 15, 16, "P-102");

        when(semesterRepository.findAll()).thenReturn(List.of(currentSemester, nextSemester));
        when(subjectOfferingRepository.findBySemesterIdWithDetails(2L))
                .thenReturn(List.of(mondayLate, mondayMorning, fridayLate, fridayMorning, tuesdayMorning, tuesdayAfternoon));

        StudentAssistantService.StudentAssistantReply reply = service.ask(
                student,
                "У меня в понедельник и пятницу после 15, а остальные дни лучше с утра."
        );

        assertThat(reply.model()).isEqualTo("deterministic-schedule-planner");
        assertThat(reply.scheduleRecommendation()).isNotNull();
        assertThat(reply.scheduleRecommendation().semesterName()).isEqualTo("2025-2026 Spring");
        assertThat(reply.scheduleRecommendation().selectedSections()).hasSize(3);
        assertThat(reply.scheduleRecommendation().selectedSections().stream()
                .map(StudentAssistantService.SelectedSection::sectionId))
                .containsExactlyInAnyOrder(8L, 10L, 12L);
        assertThat(reply.scheduleRecommendation().satisfiedPreferences())
                .contains("Понедельник: после 15:00", "Пятница: после 15:00", "Остальные дни: лучше с утра");
        assertThat(reply.scheduleRecommendation().unsatisfiedPreferences()).isEmpty();
        assertThat(reply.scheduleRecommendation().visualSchedule().get("MONDAY").get(0).startTime()).isEqualTo("16:00");
        assertThat(reply.scheduleRecommendation().visualSchedule().get("FRIDAY").get(0).startTime()).isEqualTo("15:00");
        assertThat(reply.scheduleRecommendation().visualSchedule().get("TUESDAY").get(0).startTime()).isEqualTo("09:00");
        assertThat(reply.answer()).contains("2025-2026 Spring");
        assertThat(reply.answer()).contains("16:00-18:00 | L-430 | CSCI2107");
        assertThat(reply.answer()).contains("15:00-17:00 | M-201 | MATH2201");
        assertThat(reply.answer()).doesNotContain("Warnings:");
        assertThat(reply.answer()).doesNotContain("Could not fully satisfy:");

        verify(geminiClientService, never()).generateJson(anyString(), anyString(), anyString(), anyString(), anyDouble(), anyInt());
        verify(auditService).logStudentAction(
                eq(student),
                eq("AI_ASSISTANT_QUERY"),
                eq("StudentAssistant"),
                eq(student.getId()),
                contains("понедельник")
        );
    }

    @Test
    @DisplayName("GPA maximum questions still use deterministic planner without Gemini")
    void ask_maximumGpaQuestion_returnsDeterministicReply() {
        AcademicAnalyticsService.StudentPlannerCourse plannerCourse = new AcademicAnalyticsService.StudentPlannerCourse(
                8L,
                "CSCI2107",
                "Computer Networks",
                "Aidos Nurgaliyev",
                5,
                27.0,
                28.0,
                null,
                null,
                null,
                55.0,
                95.0,
                0.0,
                25.0,
                40.0
        );
        AcademicAnalyticsService.StudentPlannerDashboard plannerDashboard = new AcademicAnalyticsService.StudentPlannerDashboard(
                student.getId(),
                student.getName(),
                currentSemester.getId(),
                currentSemester.getName(),
                3.22,
                8,
                3.78,
                List.of(plannerCourse)
        );
        when(academicAnalyticsService.buildStudentPlannerDashboard(student)).thenReturn(plannerDashboard);

        StudentAssistantService.StudentAssistantReply reply = service.ask(
                student,
                "If I get maximum scores in all remaining courses, what will my maximum GPA be?"
        );

        assertThat(reply.model()).isEqualTo("deterministic-planner");
        assertThat(reply.scheduleRecommendation()).isNull();
        assertThat(reply.answer()).contains("3.78");

        verify(geminiClientService, never()).generate(anyString(), anyString(), anyString(), anyString(), anyDouble(), anyInt());
        verify(geminiClientService, never()).generateJson(anyString(), anyString(), anyString(), anyString(), anyDouble(), anyInt());
    }

    @Test
    @DisplayName("normal AI questions still return a friendly quota message when Gemini is exhausted")
    void ask_regularQuestion_whenQuotaExceeded_returnsFriendlyQuotaReply() {
        AcademicAnalyticsService.StudentPlannerDashboard plannerDashboard = new AcademicAnalyticsService.StudentPlannerDashboard(
                student.getId(),
                student.getName(),
                currentSemester.getId(),
                currentSemester.getName(),
                3.22,
                8,
                3.78,
                List.of()
        );
        when(academicAnalyticsService.buildStudentPlannerDashboard(student)).thenReturn(plannerDashboard);
        when(geminiClientService.generate(anyString(), anyString(), anyString(), anyString(), anyDouble(), anyInt()))
                .thenThrow(new GeminiClientService.GeminiQuotaExceededException("quota", new RuntimeException("limit")));

        StudentAssistantService.StudentAssistantReply reply = service.ask(
                student,
                "Explain my attendance risk in simple words"
        );

        assertThat(reply.model()).isEqualTo("gemini-quota-limit");
        assertThat(reply.answer()).contains("Gemini API");
        assertThat(reply.scheduleRecommendation()).isNull();

        verify(geminiClientService, times(1))
                .generate(anyString(), anyString(), anyString(), anyString(), anyDouble(), anyInt());
    }

    @Test
    @DisplayName("schedule planning stays available even when Gemini quota is exhausted")
    void ask_scheduleRecommendationRequest_whenQuotaExceeded_stillUsesDeterministicPlanner() {
        Teacher teacher = Teacher.builder()
                .id(30L)
                .name("Aidos Nurgaliyev")
                .build();
        Subject subject = Subject.builder()
                .id(20L)
                .code("CSCI2107")
                .name("Computer Networks")
                .program(program)
                .build();
        SubjectOffering offering = offering(8L, subject, teacher, DayOfWeek.MONDAY, 12, 14, "L-430");

        when(semesterRepository.findAll()).thenReturn(List.of(currentSemester, nextSemester));
        when(subjectOfferingRepository.findBySemesterIdWithDetails(2L)).thenReturn(List.of(offering));

        StudentAssistantService.StudentAssistantReply reply = service.ask(
                student,
                "Make my next semester schedule after 12 and avoid Friday if possible."
        );

        assertThat(reply.model()).isEqualTo("deterministic-schedule-planner");
        assertThat(reply.scheduleRecommendation()).isNotNull();
        assertThat(reply.scheduleRecommendation().selectedSections()).hasSize(1);

        verify(geminiClientService, never())
                .generateJson(anyString(), anyString(), anyString(), anyString(), anyDouble(), anyInt());
    }

    @Test
    @DisplayName("markdown is stripped from student assistant text replies")
    void ask_regularQuestion_stripsMarkdownFromGeminiAnswer() {
        AcademicAnalyticsService.StudentPlannerDashboard plannerDashboard = new AcademicAnalyticsService.StudentPlannerDashboard(
                student.getId(),
                student.getName(),
                currentSemester.getId(),
                currentSemester.getName(),
                3.22,
                0,
                3.22,
                List.of()
        );
        when(registrationRepository.findByStudentIdWithDetails(student.getId())).thenReturn(List.of());
        when(gradeRepository.findByStudentIdAndPublishedTrueWithDetails(student.getId())).thenReturn(List.of());
        when(finalGradeRepository.findByStudentIdAndPublishedTrueWithDetails(student.getId())).thenReturn(List.of());
        when(attendanceRepository.findByStudentIdWithDetails(student.getId())).thenReturn(List.of());
        when(holdRepository.findByStudentIdAndActiveTrue(student.getId())).thenReturn(List.of());
        when(studentRequestRepository.findByStudentIdWithDetailsOrderByCreatedAtDesc(student.getId())).thenReturn(List.of());
        when(academicAnalyticsService.buildStudentPlannerDashboard(student)).thenReturn(plannerDashboard);
        when(gpaCalculationService.calculatePublishedGpa(List.of())).thenReturn(3.22);
        when(geminiClientService.generate(anyString(), anyString(), anyString(), anyString(), anyDouble(), anyInt()))
                .thenReturn(new GeminiClientService.GeminiReply(
                        "Аружан, вот ваши текущие оценки:\n* **CSCI1204 | Programming Principles I**: 54.10/60\n* **CSCI2106 | Object-Oriented Programming and Design**: 50.20/60",
                        "gemini-2.5-flash",
                        Instant.parse("2026-04-14T12:00:00Z")
                ));

        StudentAssistantService.StudentAssistantReply reply = service.ask(
                student,
                "Каковы мои оценки?"
        );

        assertThat(reply.answer()).contains("Аружан, вот ваши текущие оценки:");
        assertThat(reply.answer()).contains("- CSCI1204 | Programming Principles I: 54.10/60");
        assertThat(reply.answer()).contains("- CSCI2106 | Object-Oriented Programming and Design: 50.20/60");
        assertThat(reply.answer()).doesNotContain("**");
        assertThat(reply.answer()).doesNotContain("* **");
    }

    private SubjectOffering offering(
            Long id,
            Subject subject,
            Teacher teacher,
            DayOfWeek dayOfWeek,
            int startHour,
            int endHour,
            String room
    ) {
        return SubjectOffering.builder()
                .id(id)
                .subject(subject)
                .semester(nextSemester)
                .teacher(teacher)
                .lessonType(SubjectOffering.LessonType.LECTURE)
                .capacity(25)
                .dayOfWeek(dayOfWeek)
                .startTime(LocalTime.of(startHour, 0))
                .endTime(LocalTime.of(endHour, 0))
                .room(room)
                .build();
    }
}
