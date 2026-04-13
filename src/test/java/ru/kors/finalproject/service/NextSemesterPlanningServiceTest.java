package ru.kors.finalproject.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.ArgumentMatchers.any;
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
    @DisplayName("overview returns saved subjects separately from preferred section choices")
    void getOverview_returnsSubjectFirstPlan() {
        Subject databases = Subject.builder()
                .id(101L)
                .code("CSCI2104")
                .name("Databases")
                .credits(4)
                .build();
        Subject algorithms = Subject.builder()
                .id(102L)
                .code("CSCI2105")
                .name("Algorithms")
                .credits(4)
                .build();
        Teacher teacher = Teacher.builder().id(3L).name("Professor Aidos Nurgaliyev").build();
        SubjectOffering offering = SubjectOffering.builder()
                .id(44L)
                .subject(databases)
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
                                .build()
                ))
                .build();
        ProgramCurriculumItem item1 = ProgramCurriculumItem.builder()
                .id(1L)
                .program(program)
                .subject(databases)
                .academicYear(2)
                .semesterNumber(1)
                .displayOrder(1)
                .required(true)
                .build();
        ProgramCurriculumItem item2 = ProgramCurriculumItem.builder()
                .id(2L)
                .program(program)
                .subject(algorithms)
                .academicYear(2)
                .semesterNumber(1)
                .displayOrder(2)
                .required(true)
                .build();
        PlannedRegistration savedSubject = PlannedRegistration.builder()
                .id(7L)
                .student(student)
                .semester(nextSemester)
                .subject(databases)
                .subjectOffering(offering)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(semesterRepository.findAll()).thenReturn(List.of(currentSemester, nextSemester));
        when(programCurriculumItemRepository.findByProgramIdAndAcademicYearAndSemesterNumberWithDetails(1L, 2, 1))
                .thenReturn(List.of(item1, item2));
        when(subjectOfferingRepository.findBySemesterIdWithDetails(9L)).thenReturn(List.of(offering));
        when(plannedRegistrationRepository.findByStudentIdAndSemesterIdWithDetails(15L, 9L)).thenReturn(List.of(savedSubject));
        when(plannedRegistrationRepository.countBySubjectOfferingId(44L)).thenReturn(1L);

        NextSemesterPlanningService.NextSemesterPlanOverview overview = service.getOverview(student);

        assertThat(overview.selectionEnabled()).isTrue();
        assertThat(overview.selectedCount()).isEqualTo(1);
        assertThat(overview.savedSubjects()).hasSize(1);
        assertThat(overview.savedSubjects().get(0).subjectCode()).isEqualTo("CSCI2104");
        assertThat(overview.savedSubjects().get(0).sectionSelected()).isTrue();
        assertThat(overview.subjects()).hasSize(2);
        assertThat(overview.subjects().get(0).saved()).isTrue();
        assertThat(overview.subjects().get(0).selectedSectionId()).isEqualTo(44L);
        assertThat(overview.subjects().get(1).saved()).isFalse();
        assertThat(overview.subjects().get(1).sections()).isEmpty();
    }

    @Test
    @DisplayName("saveSubject rejects saving more than five next-semester subjects")
    void saveSubject_rejectsMoreThanFiveSelections() {
        Subject subject = Subject.builder()
                .id(101L)
                .code("CSCI2104")
                .name("Databases")
                .credits(4)
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
        when(plannedRegistrationRepository.findByStudentIdAndSemesterIdAndSubjectIdWithDetails(15L, 9L, 101L))
                .thenReturn(Optional.empty());
        when(plannedRegistrationRepository.countByStudentIdAndSemesterId(15L, 9L))
                .thenReturn(5L);

        assertThatThrownBy(() -> service.saveSubject(student, 101L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no more than 5 subjects");

        verify(plannedRegistrationRepository, never()).save(any());
    }

    @Test
    @DisplayName("chooseSection keeps subject draft and stores preferred section")
    void chooseSection_savesPreferredSection() {
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
        PlannedRegistration draft = PlannedRegistration.builder()
                .id(7L)
                .student(student)
                .semester(nextSemester)
                .subject(subject)
                .subjectOffering(null)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(semesterRepository.findAll()).thenReturn(List.of(currentSemester, nextSemester));
        when(plannedRegistrationRepository.findByStudentIdAndSemesterIdAndSubjectIdWithDetails(15L, 9L, 101L))
                .thenReturn(Optional.of(draft));
        when(subjectOfferingRepository.findByIdWithDetails(44L)).thenReturn(Optional.of(offering));
        when(plannedRegistrationRepository.findByStudentIdAndSemesterIdWithDetails(15L, 9L))
                .thenReturn(List.of(draft));
        when(programCurriculumItemRepository.findByProgramIdAndAcademicYearAndSemesterNumberWithDetails(1L, 2, 1))
                .thenReturn(List.of(ProgramCurriculumItem.builder()
                        .id(1L)
                        .program(program)
                        .subject(subject)
                        .academicYear(2)
                        .semesterNumber(1)
                        .displayOrder(1)
                        .required(true)
                        .build()));
        when(plannedRegistrationRepository.save(any(PlannedRegistration.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NextSemesterPlanningService.PlanActionResult result = service.chooseSection(student, 101L, 44L);

        assertThat(result.success()).isTrue();
        ArgumentCaptor<PlannedRegistration> captor = ArgumentCaptor.forClass(PlannedRegistration.class);
        verify(plannedRegistrationRepository).save(captor.capture());
        assertThat(captor.getValue().getSubject()).isEqualTo(subject);
        assertThat(captor.getValue().getSubjectOffering()).isEqualTo(offering);
    }
}
