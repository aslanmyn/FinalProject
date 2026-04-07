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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
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
    @DisplayName("ask builds a structured next-semester schedule recommendation from an AI plan")
    void ask_scheduleRecommendationRequest_returnsStructuredSchedule() {
        Subject subject = Subject.builder()
                .id(20L)
                .code("CSCI2107")
                .name("Computer Networks")
                .program(program)
                .build();

        Teacher teacher = Teacher.builder()
                .id(30L)
                .name("Aidos Nurgaliyev")
                .build();

        SubjectOffering offering = SubjectOffering.builder()
                .id(8L)
                .subject(subject)
                .semester(nextSemester)
                .teacher(teacher)
                .lessonType(SubjectOffering.LessonType.LECTURE)
                .capacity(25)
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(12, 0))
                .endTime(LocalTime.of(14, 0))
                .room("L-430")
                .build();

        when(semesterRepository.findAll()).thenReturn(List.of(currentSemester, nextSemester));
        when(subjectOfferingRepository.findBySemesterIdWithDetails(2L)).thenReturn(List.of(offering));
        when(geminiClientService.getLocale()).thenReturn("ru");
        when(geminiClientService.generateJson(anyString(), anyString(), anyString(), anyString(), anyDouble(), anyInt()))
                .thenReturn(new GeminiClientService.GeminiReply("""
                        {
                          "feasible": true,
                          "partial": false,
                          "chatResponse": "I built a next semester schedule without conflicts and after 12:00.",
                          "summary": "After 12 and avoid Friday",
                          "satisfiedPreferences": ["After 12:00", "Avoid Friday"],
                          "unsatisfiedPreferences": [],
                          "blockingCourses": [],
                          "selectedSections": [
                            {
                              "courseCode": "CSCI2107",
                              "courseName": "Computer Networks",
                              "sectionId": 8,
                              "teacherName": "Aidos Nurgaliyev",
                              "meetingTimes": [
                                {
                                  "dayOfWeek": "MONDAY",
                                  "startTime": "12:00",
                                  "endTime": "14:00",
                                  "room": "L-430"
                                }
                              ]
                            }
                          ],
                          "warnings": []
                        }
                        """, "gemini-2.5-flash", Instant.parse("2026-04-06T10:00:00Z")))
                .thenReturn(new GeminiClientService.GeminiReply("""
                        {
                          "valid": true,
                          "errors": []
                        }
                        """, "gemini-2.5-flash", Instant.parse("2026-04-06T10:00:01Z")));

        StudentAssistantService.StudentAssistantReply reply = service.ask(
                student,
                "Make my next semester schedule after 12 and avoid Friday if possible."
        );

        assertThat(reply.model()).isEqualTo("gemini-2.5-flash");
        assertThat(reply.answer()).contains("without conflicts").contains("after 12");
        assertThat(reply.scheduleRecommendation()).isNotNull();
        assertThat(reply.scheduleRecommendation().semesterName()).isEqualTo("2025-2026 Spring");
        assertThat(reply.scheduleRecommendation().selectedSections()).hasSize(1);
        assertThat(reply.scheduleRecommendation().selectedSections().get(0).courseCode()).isEqualTo("CSCI2107");
        assertThat(reply.scheduleRecommendation().visualSchedule())
                .containsKey("MONDAY");
        assertThat(reply.scheduleRecommendation().visualSchedule().get("MONDAY"))
                .hasSize(1);
        assertThat(reply.scheduleRecommendation().visualSchedule().get("MONDAY").get(0).startTime())
                .isEqualTo("12:00");

        verify(geminiClientService, times(2))
                .generateJson(anyString(), anyString(), anyString(), anyString(), anyDouble(), anyInt());
        verify(auditService).logStudentAction(
                eq(student),
                eq("AI_ASSISTANT_QUERY"),
                eq("StudentAssistant"),
                eq(student.getId()),
                contains("next semester schedule")
        );
    }

    @Test
    @DisplayName("ask answers maximum GPA questions deterministically without calling Gemini")
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
        verify(auditService).logStudentAction(
                eq(student),
                eq("AI_ASSISTANT_QUERY"),
                eq("StudentAssistant"),
                eq(student.getId()),
                contains("maximum scores")
        );
    }
    @Test
    @DisplayName("ask returns a clear quota message for normal AI questions when Gemini quota is exhausted")
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
        assertThat(reply.answer()).contains("лимит Gemini API").contains("сброса квоты");
        assertThat(reply.scheduleRecommendation()).isNull();

        verify(geminiClientService, times(1))
                .generate(anyString(), anyString(), anyString(), anyString(), anyDouble(), anyInt());
        verify(auditService).logStudentAction(
                eq(student),
                eq("AI_ASSISTANT_QUERY"),
                eq("StudentAssistant"),
                eq(student.getId()),
                contains("attendance risk")
        );
    }

    @Test
    @DisplayName("ask returns a clear quota message for schedule planning when Gemini quota is exhausted")
    void ask_scheduleRecommendationRequest_whenQuotaExceeded_returnsFriendlyQuotaReply() {
        Subject subject = Subject.builder()
                .id(20L)
                .code("CSCI2107")
                .name("Computer Networks")
                .program(program)
                .build();

        Teacher teacher = Teacher.builder()
                .id(30L)
                .name("Aidos Nurgaliyev")
                .build();

        SubjectOffering offering = SubjectOffering.builder()
                .id(8L)
                .subject(subject)
                .semester(nextSemester)
                .teacher(teacher)
                .lessonType(SubjectOffering.LessonType.LECTURE)
                .capacity(25)
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(12, 0))
                .endTime(LocalTime.of(14, 0))
                .room("L-430")
                .build();

        when(semesterRepository.findAll()).thenReturn(List.of(currentSemester, nextSemester));
        when(subjectOfferingRepository.findBySemesterIdWithDetails(2L)).thenReturn(List.of(offering));
        when(geminiClientService.getLocale()).thenReturn("ru");
        when(geminiClientService.generateJson(anyString(), anyString(), anyString(), anyString(), anyDouble(), anyInt()))
                .thenThrow(new GeminiClientService.GeminiQuotaExceededException("quota", new RuntimeException("limit")));

        StudentAssistantService.StudentAssistantReply reply = service.ask(
                student,
                "Make my next semester schedule after 12 and avoid Friday if possible."
        );

        assertThat(reply.model()).isEqualTo("gemini-quota-limit");
        assertThat(reply.answer()).contains("лимит Gemini API").contains("расписания");
        assertThat(reply.scheduleRecommendation()).isNull();

        verify(geminiClientService, atLeastOnce())
                .generateJson(anyString(), anyString(), anyString(), anyString(), anyDouble(), anyInt());
        verify(auditService).logStudentAction(
                eq(student),
                eq("AI_ASSISTANT_QUERY"),
                eq("StudentAssistant"),
                eq(student.getId()),
                contains("next semester schedule")
        );
    }
}

