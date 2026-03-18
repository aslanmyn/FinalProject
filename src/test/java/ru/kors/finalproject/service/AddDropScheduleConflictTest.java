package ru.kors.finalproject.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.kors.finalproject.entity.*;
import ru.kors.finalproject.repository.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Targeted unit tests for AddDropService scenarios NOT covered by AddDropServiceTest:
 *  - schedule conflict detection (overlapping timeslots)
 *  - prerequisite enforcement (incomplete prereq blocks enrollment)
 *  - admin override bypasses window / hold / capacity checks
 *  - dropCourse blocked when add/drop window is closed
 *  - ACADEMIC hold blocks enrollment
 */
@ExtendWith(MockitoExtension.class)
class AddDropScheduleConflictTest {

    @Mock private SubjectOfferingRepository subjectOfferingRepository;
    @Mock private RegistrationRepository registrationRepository;
    @Mock private RegistrationWindowRepository registrationWindowRepository;
    @Mock private SubjectPrerequisiteRepository prerequisiteRepository;
    @Mock private MeetingTimeRepository meetingTimeRepository;
    @Mock private HoldRepository holdRepository;
    @Mock private FinalGradeRepository finalGradeRepository;
    @Mock private WorkflowEngineService workflowEngineService;
    @Mock private FinancialService financialService;
    @Mock private AuditService auditService;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private AddDropService addDropService;

    private Student student;
    private Program program;
    private Semester semester;
    private Subject subject;
    private SubjectOffering offering;
    private RegistrationWindow openWindow;

    @BeforeEach
    void setUp() {
        program = Program.builder().id(1L).name("CS").creditLimit(30).build();
        semester = Semester.builder().id(1L).name("Fall 2025").current(true).build();
        student = Student.builder()
                .id(1L).email("a_sched@kbtu.kz").name("Sched Tester")
                .course(2).program(program).currentSemester(semester).creditsEarned(40).build();

        subject = Subject.builder().id(10L).code("CS300").name("Algorithms").credits(6).program(program).build();
        offering = SubjectOffering.builder()
                .id(100L).subject(subject).semester(semester).capacity(30)
                .dayOfWeek(DayOfWeek.MONDAY).startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(10, 30))
                .build();

        openWindow = RegistrationWindow.builder()
                .id(1L).semester(semester).type(RegistrationWindow.WindowType.ADD_DROP)
                .startDate(LocalDate.now().minusDays(1)).endDate(LocalDate.now().plusDays(5)).active(true)
                .build();
    }

    // =========================================================================
    // Schedule conflict
    // =========================================================================

    @Test
    @DisplayName("addCourse - error when new course overlaps an already-registered timeslot")
    void addCourse_scheduleConflict() {
        // Target: Monday 09:00–10:30 (offering)
        // Existing: Monday 09:00–10:30 exactly — full overlap
        Subject conflictSubject = Subject.builder().id(20L).code("CS101").name("Intro").credits(3).program(program).build();
        SubjectOffering conflictOffering = SubjectOffering.builder()
                .id(200L).subject(conflictSubject).semester(semester).capacity(30)
                .dayOfWeek(DayOfWeek.MONDAY).startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(10, 30))
                .build();
        Registration existingReg = Registration.builder()
                .id(60L).student(student).subjectOffering(conflictOffering)
                .status(Registration.RegistrationStatus.CONFIRMED).build();

        when(subjectOfferingRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(offering));
        when(financialService.hasRegistrationLock(student)).thenReturn(false);
        when(holdRepository.findByStudentIdAndActiveTrue(1L)).thenReturn(List.of());
        when(registrationWindowRepository.findBySemesterIdAndTypeAndActiveTrue(1L, RegistrationWindow.WindowType.ADD_DROP))
                .thenReturn(Optional.of(openWindow));
        when(registrationRepository.findByStudentIdAndSubjectOfferingId(1L, 100L)).thenReturn(Optional.empty());
        when(registrationRepository.findActiveByStudentIdWithDetails(1L)).thenReturn(List.of(existingReg));
        when(prerequisiteRepository.findBySubjectId(10L)).thenReturn(List.of());
        // No MeetingTime rows — service falls back to offering's built-in day/time
        when(meetingTimeRepository.findBySubjectOfferingId(100L)).thenReturn(List.of());
        when(meetingTimeRepository.findBySubjectOfferingId(200L)).thenReturn(List.of());
        when(registrationRepository.countBySubjectOfferingIdAndStatusIn(eq(100L), anyList())).thenReturn(5L);

        AddDropService.AddDropResult result = addDropService.addCourse(student, 100L);

        assertThat(result.success()).isFalse();
        assertThat(result.errors()).contains("Schedule conflict detected");
        verify(registrationRepository, never()).save(any());
    }

    @Test
    @DisplayName("addCourse - NO conflict when courses are on different days")
    void addCourse_noConflict_differentDays() {
        Subject otherSubject = Subject.builder().id(20L).code("CS101").name("Intro").credits(3).program(program).build();
        SubjectOffering tuesdayCourse = SubjectOffering.builder()
                .id(200L).subject(otherSubject).semester(semester).capacity(30)
                .dayOfWeek(DayOfWeek.TUESDAY).startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(10, 30))
                .build();
        Registration existingReg = Registration.builder()
                .id(60L).student(student).subjectOffering(tuesdayCourse)
                .status(Registration.RegistrationStatus.CONFIRMED).build();

        when(subjectOfferingRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(offering));
        when(financialService.hasRegistrationLock(student)).thenReturn(false);
        when(holdRepository.findByStudentIdAndActiveTrue(1L)).thenReturn(List.of());
        when(registrationWindowRepository.findBySemesterIdAndTypeAndActiveTrue(1L, RegistrationWindow.WindowType.ADD_DROP))
                .thenReturn(Optional.of(openWindow));
        when(registrationRepository.findByStudentIdAndSubjectOfferingId(1L, 100L)).thenReturn(Optional.empty());
        when(registrationRepository.findActiveByStudentIdWithDetails(1L)).thenReturn(List.of(existingReg));
        when(prerequisiteRepository.findBySubjectId(10L)).thenReturn(List.of());
        when(meetingTimeRepository.findBySubjectOfferingId(100L)).thenReturn(List.of());
        when(meetingTimeRepository.findBySubjectOfferingId(200L)).thenReturn(List.of());
        when(registrationRepository.countBySubjectOfferingIdAndStatusIn(eq(100L), anyList())).thenReturn(5L);
        when(registrationRepository.save(any(Registration.class))).thenAnswer(inv -> inv.getArgument(0));

        AddDropService.AddDropResult result = addDropService.addCourse(student, 100L);

        assertThat(result.success()).isTrue();
    }

    // =========================================================================
    // Prerequisite enforcement
    // =========================================================================

    @Test
    @DisplayName("addCourse - error when required prerequisite has not been passed")
    void addCourse_prerequisiteNotCompleted() {
        Subject prereqSubject = Subject.builder().id(5L).code("CS101").name("Intro CS").credits(6).program(program).build();
        SubjectPrerequisite prereq = SubjectPrerequisite.builder()
                .id(1L).subject(subject).prerequisite(prereqSubject).build();

        when(subjectOfferingRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(offering));
        when(financialService.hasRegistrationLock(student)).thenReturn(false);
        when(holdRepository.findByStudentIdAndActiveTrue(1L)).thenReturn(List.of());
        when(registrationWindowRepository.findBySemesterIdAndTypeAndActiveTrue(1L, RegistrationWindow.WindowType.ADD_DROP))
                .thenReturn(Optional.of(openWindow));
        when(registrationRepository.findByStudentIdAndSubjectOfferingId(1L, 100L)).thenReturn(Optional.empty());
        when(registrationRepository.findActiveByStudentIdWithDetails(1L)).thenReturn(List.of());
        when(prerequisiteRepository.findBySubjectId(10L)).thenReturn(List.of(prereq));
        // No final grades — prerequisite not completed
        when(finalGradeRepository.findByStudentId(1L)).thenReturn(List.of());
        when(meetingTimeRepository.findBySubjectOfferingId(100L)).thenReturn(List.of());
        when(registrationRepository.countBySubjectOfferingIdAndStatusIn(eq(100L), anyList())).thenReturn(5L);

        AddDropService.AddDropResult result = addDropService.addCourse(student, 100L);

        assertThat(result.success()).isFalse();
        assertThat(result.errors()).anyMatch(e -> e.contains("Prerequisite not completed"));
    }

    @Test
    @DisplayName("addCourse - succeeds when prerequisite is completed with grade >= 50")
    void addCourse_prerequisiteCompleted() {
        Subject prereqSubject = Subject.builder().id(5L).code("CS101").name("Intro CS").credits(6).program(program).build();
        SubjectOffering prereqOffering = SubjectOffering.builder().id(500L).subject(prereqSubject).semester(semester).build();
        SubjectPrerequisite prereq = SubjectPrerequisite.builder()
                .id(1L).subject(subject).prerequisite(prereqSubject).build();

        FinalGrade passedGrade = FinalGrade.builder()
                .id(1L).student(student).subjectOffering(prereqOffering)
                .numericValue(75.0).points(3.0).published(true)
                .status(FinalGrade.FinalGradeStatus.PUBLISHED).createdAt(java.time.Instant.now()).build();

        when(subjectOfferingRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(offering));
        when(financialService.hasRegistrationLock(student)).thenReturn(false);
        when(holdRepository.findByStudentIdAndActiveTrue(1L)).thenReturn(List.of());
        when(registrationWindowRepository.findBySemesterIdAndTypeAndActiveTrue(1L, RegistrationWindow.WindowType.ADD_DROP))
                .thenReturn(Optional.of(openWindow));
        when(registrationRepository.findByStudentIdAndSubjectOfferingId(1L, 100L)).thenReturn(Optional.empty());
        when(registrationRepository.findActiveByStudentIdWithDetails(1L)).thenReturn(List.of());
        when(prerequisiteRepository.findBySubjectId(10L)).thenReturn(List.of(prereq));
        when(finalGradeRepository.findByStudentId(1L)).thenReturn(List.of(passedGrade));
        when(meetingTimeRepository.findBySubjectOfferingId(100L)).thenReturn(List.of());
        when(registrationRepository.countBySubjectOfferingIdAndStatusIn(eq(100L), anyList())).thenReturn(5L);
        when(registrationRepository.save(any(Registration.class))).thenAnswer(inv -> inv.getArgument(0));

        AddDropService.AddDropResult result = addDropService.addCourse(student, 100L);

        assertThat(result.success()).isTrue();
    }

    @Test
    @DisplayName("addCourse - prerequisite with grade < 50 is NOT considered completed")
    void addCourse_prerequisiteFailedGrade() {
        Subject prereqSubject = Subject.builder().id(5L).code("CS101").name("Intro CS").credits(6).program(program).build();
        SubjectOffering prereqOffering = SubjectOffering.builder().id(500L).subject(prereqSubject).semester(semester).build();
        SubjectPrerequisite prereq = SubjectPrerequisite.builder()
                .id(1L).subject(subject).prerequisite(prereqSubject).build();

        FinalGrade failedGrade = FinalGrade.builder()
                .id(1L).student(student).subjectOffering(prereqOffering)
                .numericValue(45.0).points(1.0).published(true)   // <50 = failed
                .status(FinalGrade.FinalGradeStatus.PUBLISHED).createdAt(java.time.Instant.now()).build();

        when(subjectOfferingRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(offering));
        when(financialService.hasRegistrationLock(student)).thenReturn(false);
        when(holdRepository.findByStudentIdAndActiveTrue(1L)).thenReturn(List.of());
        when(registrationWindowRepository.findBySemesterIdAndTypeAndActiveTrue(1L, RegistrationWindow.WindowType.ADD_DROP))
                .thenReturn(Optional.of(openWindow));
        when(registrationRepository.findByStudentIdAndSubjectOfferingId(1L, 100L)).thenReturn(Optional.empty());
        when(registrationRepository.findActiveByStudentIdWithDetails(1L)).thenReturn(List.of());
        when(prerequisiteRepository.findBySubjectId(10L)).thenReturn(List.of(prereq));
        when(finalGradeRepository.findByStudentId(1L)).thenReturn(List.of(failedGrade));
        when(meetingTimeRepository.findBySubjectOfferingId(100L)).thenReturn(List.of());
        when(registrationRepository.countBySubjectOfferingIdAndStatusIn(eq(100L), anyList())).thenReturn(5L);

        AddDropService.AddDropResult result = addDropService.addCourse(student, 100L);

        assertThat(result.success()).isFalse();
        assertThat(result.errors()).anyMatch(e -> e.contains("Prerequisite not completed"));
    }

    // =========================================================================
    // Admin override
    // =========================================================================

    @Test
    @DisplayName("adminOverrideEnroll - bypasses closed window, full capacity, and active hold")
    void adminOverrideEnroll_bypassesAllGuards() {
        // adminOverride=true bypasses window check and hold check — do NOT stub those.
        // Capacity is still fetched even in override mode (the guard is !adminOverride && occupied >= cap).
        when(registrationRepository.countBySubjectOfferingIdAndStatusIn(eq(100L), anyList())).thenReturn(30L);

        when(subjectOfferingRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(offering));
        when(registrationRepository.findByStudentIdAndSubjectOfferingId(1L, 100L)).thenReturn(Optional.empty());
        when(registrationRepository.findActiveByStudentIdWithDetails(1L)).thenReturn(List.of());
        when(prerequisiteRepository.findBySubjectId(10L)).thenReturn(List.of());
        when(meetingTimeRepository.findBySubjectOfferingId(100L)).thenReturn(List.of());
        when(registrationRepository.save(any(Registration.class))).thenAnswer(inv -> inv.getArgument(0));

        AddDropService.AddDropResult result = addDropService.adminOverrideEnroll(student, 100L);

        assertThat(result.success()).isTrue();
        verify(registrationRepository).save(any(Registration.class));
    }

    // =========================================================================
    // Drop with closed window
    // =========================================================================

    @Test
    @DisplayName("dropCourse - error when add/drop window is not active")
    void dropCourse_windowClosed_returnsError() {
        Registration reg = Registration.builder()
                .id(50L).student(student).subjectOffering(offering)
                .status(Registration.RegistrationStatus.CONFIRMED).build();

        when(registrationRepository.findByStudentIdAndSubjectOfferingIdWithDetails(1L, 100L))
                .thenReturn(Optional.of(reg));
        when(registrationWindowRepository.findBySemesterIdAndTypeAndActiveTrue(1L, RegistrationWindow.WindowType.ADD_DROP))
                .thenReturn(Optional.empty());

        AddDropService.AddDropResult result = addDropService.dropCourse(student, 100L);

        assertThat(result.success()).isFalse();
        assertThat(result.errors()).contains("Add/drop window is closed");
        verify(registrationRepository, never()).save(any());
    }

    // =========================================================================
    // Academic hold blocks enrollment
    // =========================================================================

    @Test
    @DisplayName("addCourse - error when student has an active ACADEMIC hold")
    void addCourse_academicHold_blocked() {
        Hold academicHold = Hold.builder()
                .id(10L).student(student).type(Hold.HoldType.ACADEMIC).active(true)
                .reason("Academic probation").createdAt(java.time.Instant.now()).build();

        when(subjectOfferingRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(offering));
        when(financialService.hasRegistrationLock(student)).thenReturn(false);
        when(holdRepository.findByStudentIdAndActiveTrue(1L)).thenReturn(List.of(academicHold));
        when(registrationWindowRepository.findBySemesterIdAndTypeAndActiveTrue(1L, RegistrationWindow.WindowType.ADD_DROP))
                .thenReturn(Optional.of(openWindow));
        when(registrationRepository.findByStudentIdAndSubjectOfferingId(1L, 100L)).thenReturn(Optional.empty());
        when(registrationRepository.findActiveByStudentIdWithDetails(1L)).thenReturn(List.of());
        when(prerequisiteRepository.findBySubjectId(10L)).thenReturn(List.of());
        when(meetingTimeRepository.findBySubjectOfferingId(100L)).thenReturn(List.of());
        when(registrationRepository.countBySubjectOfferingIdAndStatusIn(eq(100L), anyList())).thenReturn(5L);

        AddDropService.AddDropResult result = addDropService.addCourse(student, 100L);

        assertThat(result.success()).isFalse();
        assertThat(result.errors()).anyMatch(e -> e.contains("locked") || e.contains("hold") || e.contains("Hold"));
    }
}
