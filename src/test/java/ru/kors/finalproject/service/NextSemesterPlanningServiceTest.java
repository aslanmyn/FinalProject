package ru.kors.finalproject.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.kors.finalproject.entity.MeetingTime;
import ru.kors.finalproject.entity.PlannedRegistration;
import ru.kors.finalproject.entity.Program;
import ru.kors.finalproject.entity.ProgramCurriculumItem;
import ru.kors.finalproject.entity.Semester;
import ru.kors.finalproject.entity.Student;
import ru.kors.finalproject.entity.Subject;
import ru.kors.finalproject.entity.SubjectOffering;
import ru.kors.finalproject.entity.Teacher;
import ru.kors.finalproject.repository.PlannedRegistrationRepository;
import ru.kors.finalproject.repository.ProgramCurriculumItemRepository;
import ru.kors.finalproject.repository.RegistrationRepository;
import ru.kors.finalproject.repository.SemesterRepository;
import ru.kors.finalproject.repository.SubjectOfferingRepository;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NextSemesterPlanningServiceTest {

    @Mock
    private SemesterRepository semesterRepository;
    @Mock
    private ProgramCurriculumItemRepository programCurriculumItemRepository;
    @Mock
    private SubjectOfferingRepository subjectOfferingRepository;
    @Mock
    private PlannedRegistrationRepository plannedRegistrationRepository;
    @Mock
    private RegistrationRepository registrationRepository;
    @Mock
    private AuditService auditService;

    private NextSemesterPlanningService service;
    private Student student;
    private Program program;
    private Semester currentSemester;
    private Semester nextSemester;

    @BeforeEach
    void setUp() {
        service = new NextSemesterPlanningService(
                semesterRepository,
                programCurriculumItemRepository,
                subjectOfferingRepository,
                plannedRegistrationRepository,
                registrationRepository,
                auditService
        );

        program = Program.builder()
                .id(1L)
                .name("Information Technology and Engineering")
                .creditLimit(22)
                .build();

        currentSemester = Semester.builder()
                .id(8L)
                .name("2025-2026 Spring")
                .startDate(LocalDate.of(2026, 1, 19))
                .endDate(LocalDate.of(2026, 5, 29))
                .current(true)
                .build();

        nextSemester = Semester.builder()
                .id(9L)
                .name("2026-2027 Fall")
                .startDate(LocalDate.of(2026, 9, 1))
                .endDate(LocalDate.of(2026, 12, 20))
                .current(false)
                .build();

        student = Student.builder()
                .id(15L)
                .email("a_student@kbtu.kz")
                .name("Aruzhan Student")
                .program(program)
                .course(1)
                .currentSemester(currentSemester)
                .build();
    }

    @Test
    @DisplayName("overview returns next term curriculum subjects and offerings for a spring student")
    void getOverview_returnsNextTermPlan() {
        Subject subject = Subject.builder()
                .id(101L)
                .code("CSCI2104")
                .name("Databases")
                .credits(4)
                .build();
        Teacher teacher = Teacher.builder().id(3L).name("Professor Aidos Nurgaliyev").build();
        SubjectOffering offering = SubjectOffering.builder()
                .id(44L)
                .subject(subject)
                .semester(nextSemester)
                .teacher(teacher)
                .capacity(20)
                .meetingTimes(List.of(
                        MeetingTime.builder()
                                .dayOfWeek(DayOfWeek.MONDAY)
                                .startTime(LocalTime.of(12, 0))
                                .endTime(LocalTime.of(14, 0))
                                .room("L-201")
                                .lessonType(SubjectOffering.LessonType.LECTURE)
                                .build(),
                        MeetingTime.builder()
                                .dayOfWeek(DayOfWeek.WEDNESDAY)
                                .startTime(LocalTime.of(15, 0))
                                .endTime(LocalTime.of(16, 0))
                                .room("P-201")
                                .lessonType(SubjectOffering.LessonType.PRACTICE)
                                .build()
                ))
                .build();
        ProgramCurriculumItem item = ProgramCurriculumItem.builder()
                .id(1L)
                .program(program)
                .subject(subject)
                .academicYear(2)
                .semesterNumber(1)
                .displayOrder(1)
                .required(true)
                .build();

        when(semesterRepository.findAll()).thenReturn(List.of(currentSemester, nextSemester));
        when(programCurriculumItemRepository.findByProgramIdAndAcademicYearAndSemesterNumberWithDetails(1L, 2, 1))
                .thenReturn(List.of(item));
        when(subjectOfferingRepository.findBySemesterIdWithDetails(9L)).thenReturn(List.of(offering));
        when(plannedRegistrationRepository.findByStudentIdAndSemesterIdWithDetails(15L, 9L)).thenReturn(List.of());
        when(registrationRepository.countBySubjectOfferingIdAndStatusIn(eq(44L), anyList())).thenReturn(0L);

        NextSemesterPlanningService.NextSemesterPlanOverview overview = service.getOverview(student);

        assertThat(overview.selectionEnabled()).isTrue();
        assertThat(overview.academicYear()).isEqualTo(2);
        assertThat(overview.semesterNumber()).isEqualTo(1);
        assertThat(overview.semesterName()).isEqualTo("2026-2027 Fall");
        assertThat(overview.subjects()).hasSize(1);
        assertThat(overview.subjects().get(0).subjectCode()).isEqualTo("CSCI2104");
        assertThat(overview.subjects().get(0).sections()).hasSize(1);
        assertThat(overview.subjects().get(0).sections().get(0).meetingTimes()).hasSize(2);
    }

    @Test
    @DisplayName("selectSection rejects saving more than five next-semester subjects")
    void selectSection_rejectsMoreThanFiveSelections() {
        Subject subject = Subject.builder()
                .id(101L)
                .code("CSCI2104")
                .name("Databases")
                .credits(4)
                .build();
        SubjectOffering offering = SubjectOffering.builder()
                .id(44L)
                .subject(subject)
                .semester(nextSemester)
                .capacity(20)
                .meetingTimes(List.of(
                        MeetingTime.builder()
                                .dayOfWeek(DayOfWeek.MONDAY)
                                .startTime(LocalTime.of(12, 0))
                                .endTime(LocalTime.of(14, 0))
                                .lessonType(SubjectOffering.LessonType.LECTURE)
                                .build()
                ))
                .build();
        ProgramCurriculumItem item = ProgramCurriculumItem.builder()
                .id(1L)
                .program(program)
                .subject(subject)
                .academicYear(2)
                .semesterNumber(1)
                .displayOrder(1)
                .required(true)
                .build();
        List<PlannedRegistration> existingSelections = List.of(
                planned(1L, student, nextSemester, 201L, "CSCI2105"),
                planned(2L, student, nextSemester, 202L, "INFT2205"),
                planned(3L, student, nextSemester, 203L, "INFT2102"),
                planned(4L, student, nextSemester, 204L, "HUM1101"),
                planned(5L, student, nextSemester, 205L, "STAT2201")
        );

        when(semesterRepository.findAll()).thenReturn(List.of(currentSemester, nextSemester));
        when(subjectOfferingRepository.findByIdWithDetails(44L)).thenReturn(Optional.of(offering));
        when(programCurriculumItemRepository.findByProgramIdAndAcademicYearAndSemesterNumberWithDetails(1L, 2, 1))
                .thenReturn(List.of(item));
        when(plannedRegistrationRepository.findByStudentIdAndSemesterIdWithDetails(15L, 9L))
                .thenReturn(existingSelections);

        assertThatThrownBy(() -> service.selectSection(student, 44L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no more than 5 subjects");

        verify(plannedRegistrationRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("removeSection deletes saved next-semester section by student and offering")
    void removeSection_deletesSavedSelection() {
        PlannedRegistration existing = planned(7L, student, nextSemester, 467L, "CSCI2104");

        when(semesterRepository.findAll()).thenReturn(List.of(currentSemester, nextSemester));
        when(plannedRegistrationRepository.findByStudentIdAndSubjectOfferingId(15L, 467L))
                .thenReturn(Optional.of(existing));
        when(plannedRegistrationRepository.deleteByStudentIdAndSubjectOfferingId(15L, 467L))
                .thenReturn(1);

        NextSemesterPlanningService.PlanActionResult result = service.removeSection(student, 467L);

        assertThat(result.success()).isTrue();
        verify(plannedRegistrationRepository).deleteByStudentIdAndSubjectOfferingId(15L, 467L);
    }

    private PlannedRegistration planned(Long id, Student student, Semester semester, Long offeringId, String subjectCode) {
        Subject subject = Subject.builder()
                .id(offeringId)
                .code(subjectCode)
                .name(subjectCode)
                .credits(4)
                .build();
        SubjectOffering offering = SubjectOffering.builder()
                .id(offeringId)
                .subject(subject)
                .semester(semester)
                .meetingTimes(List.of(
                        MeetingTime.builder()
                                .dayOfWeek(DayOfWeek.MONDAY)
                                .startTime(LocalTime.of(8, 0))
                                .endTime(LocalTime.of(9, 0))
                                .lessonType(SubjectOffering.LessonType.LECTURE)
                                .build()
                ))
                .build();
        return PlannedRegistration.builder()
                .id(id)
                .student(student)
                .semester(semester)
                .subjectOffering(offering)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
}
