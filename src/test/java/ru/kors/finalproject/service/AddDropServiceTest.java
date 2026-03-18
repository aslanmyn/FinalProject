package ru.kors.finalproject.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AddDropServiceTest {

    @Mock
    private SubjectOfferingRepository subjectOfferingRepository;
    @Mock
    private RegistrationRepository registrationRepository;
    @Mock
    private RegistrationWindowRepository registrationWindowRepository;
    @Mock
    private SubjectPrerequisiteRepository prerequisiteRepository;
    @Mock
    private MeetingTimeRepository meetingTimeRepository;
    @Mock
    private HoldRepository holdRepository;
    @Mock
    private FinalGradeRepository finalGradeRepository;
    @Mock
    private WorkflowEngineService workflowEngineService;
    @Mock
    private FinancialService financialService;
    @Mock
    private AuditService auditService;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private AddDropService addDropService;

    private Student student;
    private Program program;
    private Semester semester;
    private Subject subject;
    private SubjectOffering offering;

    @BeforeEach
    void setUp() {
        program = Program.builder()
                .id(1L)
                .name("Computer Science")
                .creditLimit(30)
                .build();

        semester = Semester.builder()
                .id(1L)
                .name("Fall 2025")
                .build();

        student = Student.builder()
                .id(1L)
                .email("student@example.com")
                .name("John Doe")
                .course(2)
                .program(program)
                .currentSemester(semester)
                .creditsEarned(60)
                .build();

        subject = Subject.builder()
                .id(10L)
                .code("CS201")
                .name("Data Structures")
                .credits(6)
                .program(program)
                .build();

        offering = SubjectOffering.builder()
                .id(100L)
                .subject(subject)
                .semester(semester)
                .capacity(30)
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 30))
                .build();
    }

    // -------------------------------------------------------------------------
    // addCourse tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("addCourse - success when offering found, window active, no conflicts")
    void addCourse_success() {
        // offering found
        when(subjectOfferingRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(offering));

        // no financial hold
        when(financialService.hasRegistrationLock(student)).thenReturn(false);
        when(holdRepository.findByStudentIdAndActiveTrue(1L)).thenReturn(List.of());

        // add/drop window active
        RegistrationWindow window = RegistrationWindow.builder()
                .id(1L)
                .semester(semester)
                .type(RegistrationWindow.WindowType.ADD_DROP)
                .startDate(LocalDate.now().minusDays(1))
                .endDate(LocalDate.now().plusDays(5))
                .active(true)
                .build();
        when(registrationWindowRepository.findBySemesterIdAndTypeAndActiveTrue(1L, RegistrationWindow.WindowType.ADD_DROP))
                .thenReturn(Optional.of(window));

        // not already registered
        when(registrationRepository.findByStudentIdAndSubjectOfferingId(1L, 100L)).thenReturn(Optional.empty());

        // no existing registrations (0 credits used)
        when(registrationRepository.findActiveByStudentIdWithDetails(1L)).thenReturn(List.of());

        // no prerequisites
        when(prerequisiteRepository.findBySubjectId(10L)).thenReturn(List.of());

        // no meeting time conflicts (no existing meeting times for target)
        when(meetingTimeRepository.findBySubjectOfferingId(100L)).thenReturn(List.of());

        // capacity not full
        when(registrationRepository.countBySubjectOfferingIdAndStatusIn(eq(100L), anyList())).thenReturn(10L);

        // save returns the registration
        when(registrationRepository.save(any(Registration.class))).thenAnswer(inv -> inv.getArgument(0));

        AddDropService.AddDropResult result = addDropService.addCourse(student, 100L);

        assertThat(result.success()).isTrue();
        assertThat(result.message()).isEqualTo("Subject added successfully");
        assertThat(result.errors()).isEmpty();

        ArgumentCaptor<Registration> regCaptor = ArgumentCaptor.forClass(Registration.class);
        verify(registrationRepository).save(regCaptor.capture());
        assertThat(regCaptor.getValue().getStatus()).isEqualTo(Registration.RegistrationStatus.CONFIRMED);
        verify(notificationService).notifyStudent(eq("student@example.com"), eq(Notification.NotificationType.ENROLLMENT),
                any(), any(), any());
        verify(auditService).logStudentAction(eq(student), eq("ENROLLMENT_CONFIRMED"), any(), any(), any());
    }

    @Test
    @DisplayName("addCourse - error when subject offering not found")
    void addCourse_subjectNotFound() {
        when(subjectOfferingRepository.findByIdForUpdate(999L)).thenReturn(Optional.empty());

        AddDropService.AddDropResult result = addDropService.addCourse(student, 999L);

        assertThat(result.success()).isFalse();
        assertThat(result.message()).isEqualTo("Subject not found");
        assertThat(result.errors()).containsExactly("Subject not found");
        verify(registrationRepository, never()).save(any());
    }

    @Test
    @DisplayName("addCourse - error when add/drop window is closed")
    void addCourse_windowClosed() {
        when(subjectOfferingRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(offering));

        // no holds
        when(financialService.hasRegistrationLock(student)).thenReturn(false);
        when(holdRepository.findByStudentIdAndActiveTrue(1L)).thenReturn(List.of());

        // window not found
        when(registrationWindowRepository.findBySemesterIdAndTypeAndActiveTrue(1L, RegistrationWindow.WindowType.ADD_DROP))
                .thenReturn(Optional.empty());

        // not already registered
        when(registrationRepository.findByStudentIdAndSubjectOfferingId(1L, 100L)).thenReturn(Optional.empty());

        // no existing registrations
        when(registrationRepository.findActiveByStudentIdWithDetails(1L)).thenReturn(List.of());

        // no prerequisites
        when(prerequisiteRepository.findBySubjectId(10L)).thenReturn(List.of());

        // no schedule conflict
        when(meetingTimeRepository.findBySubjectOfferingId(100L)).thenReturn(List.of());

        // capacity ok
        when(registrationRepository.countBySubjectOfferingIdAndStatusIn(eq(100L), anyList())).thenReturn(0L);

        AddDropService.AddDropResult result = addDropService.addCourse(student, 100L);

        assertThat(result.success()).isFalse();
        assertThat(result.errors()).contains("ADD_DROP window is not active");
        verify(registrationRepository, never()).save(any());
    }

    @Test
    @DisplayName("addCourse - error when already registered for this subject")
    void addCourse_alreadyRegistered() {
        when(subjectOfferingRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(offering));

        // no holds
        when(financialService.hasRegistrationLock(student)).thenReturn(false);
        when(holdRepository.findByStudentIdAndActiveTrue(1L)).thenReturn(List.of());

        // window active
        RegistrationWindow window = RegistrationWindow.builder()
                .id(1L)
                .semester(semester)
                .type(RegistrationWindow.WindowType.ADD_DROP)
                .startDate(LocalDate.now().minusDays(1))
                .endDate(LocalDate.now().plusDays(5))
                .active(true)
                .build();
        when(registrationWindowRepository.findBySemesterIdAndTypeAndActiveTrue(1L, RegistrationWindow.WindowType.ADD_DROP))
                .thenReturn(Optional.of(window));

        // already registered with CONFIRMED status
        Registration existingReg = Registration.builder()
                .id(50L)
                .student(student)
                .subjectOffering(offering)
                .status(Registration.RegistrationStatus.CONFIRMED)
                .build();
        when(registrationRepository.findByStudentIdAndSubjectOfferingId(1L, 100L)).thenReturn(Optional.of(existingReg));

        // no existing registrations for credit calc
        when(registrationRepository.findActiveByStudentIdWithDetails(1L)).thenReturn(List.of());

        // no prerequisites
        when(prerequisiteRepository.findBySubjectId(10L)).thenReturn(List.of());

        // no schedule conflict
        when(meetingTimeRepository.findBySubjectOfferingId(100L)).thenReturn(List.of());

        // capacity ok
        when(registrationRepository.countBySubjectOfferingIdAndStatusIn(eq(100L), anyList())).thenReturn(5L);

        AddDropService.AddDropResult result = addDropService.addCourse(student, 100L);

        assertThat(result.success()).isFalse();
        assertThat(result.errors()).contains("Already registered for this subject");
        verify(registrationRepository, never()).save(any());
    }

    @Test
    @DisplayName("addCourse - error when credit limit would be exceeded")
    void addCourse_creditLimitExceeded() {
        // Set program credit limit to 12
        program.setCreditLimit(12);

        when(subjectOfferingRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(offering));

        // no holds
        when(financialService.hasRegistrationLock(student)).thenReturn(false);
        when(holdRepository.findByStudentIdAndActiveTrue(1L)).thenReturn(List.of());

        // window active
        RegistrationWindow window = RegistrationWindow.builder()
                .id(1L)
                .semester(semester)
                .type(RegistrationWindow.WindowType.ADD_DROP)
                .startDate(LocalDate.now().minusDays(1))
                .endDate(LocalDate.now().plusDays(5))
                .active(true)
                .build();
        when(registrationWindowRepository.findBySemesterIdAndTypeAndActiveTrue(1L, RegistrationWindow.WindowType.ADD_DROP))
                .thenReturn(Optional.of(window));

        // not already registered
        when(registrationRepository.findByStudentIdAndSubjectOfferingId(1L, 100L)).thenReturn(Optional.empty());

        // already registered for 9 credits worth of courses
        Subject otherSubject = Subject.builder().id(20L).code("CS101").name("Intro").credits(9).program(program).build();
        SubjectOffering otherOffering = SubjectOffering.builder()
                .id(200L).subject(otherSubject).semester(semester).capacity(30)
                .dayOfWeek(DayOfWeek.WEDNESDAY).startTime(LocalTime.of(14, 0)).endTime(LocalTime.of(15, 30))
                .build();
        Registration existingReg = Registration.builder()
                .id(60L).student(student).subjectOffering(otherOffering)
                .status(Registration.RegistrationStatus.CONFIRMED).build();
        when(registrationRepository.findActiveByStudentIdWithDetails(1L)).thenReturn(List.of(existingReg));

        // no prerequisites
        when(prerequisiteRepository.findBySubjectId(10L)).thenReturn(List.of());

        // no schedule conflict (different days)
        when(meetingTimeRepository.findBySubjectOfferingId(100L)).thenReturn(List.of());
        when(meetingTimeRepository.findBySubjectOfferingId(200L)).thenReturn(List.of());

        // capacity ok
        when(registrationRepository.countBySubjectOfferingIdAndStatusIn(eq(100L), anyList())).thenReturn(5L);

        // 9 existing + 6 new = 15 > 12 limit
        AddDropService.AddDropResult result = addDropService.addCourse(student, 100L);

        assertThat(result.success()).isFalse();
        assertThat(result.errors()).contains("Credit limit exceeded");
        verify(registrationRepository, never()).save(any());
    }

    @Test
    @DisplayName("addCourse - error when capacity is full")
    void addCourse_capacityFull() {
        offering.setCapacity(30);

        when(subjectOfferingRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(offering));

        // no holds
        when(financialService.hasRegistrationLock(student)).thenReturn(false);
        when(holdRepository.findByStudentIdAndActiveTrue(1L)).thenReturn(List.of());

        // window active
        RegistrationWindow window = RegistrationWindow.builder()
                .id(1L)
                .semester(semester)
                .type(RegistrationWindow.WindowType.ADD_DROP)
                .startDate(LocalDate.now().minusDays(1))
                .endDate(LocalDate.now().plusDays(5))
                .active(true)
                .build();
        when(registrationWindowRepository.findBySemesterIdAndTypeAndActiveTrue(1L, RegistrationWindow.WindowType.ADD_DROP))
                .thenReturn(Optional.of(window));

        // not already registered
        when(registrationRepository.findByStudentIdAndSubjectOfferingId(1L, 100L)).thenReturn(Optional.empty());

        // no existing registrations
        when(registrationRepository.findActiveByStudentIdWithDetails(1L)).thenReturn(List.of());

        // no prerequisites
        when(prerequisiteRepository.findBySubjectId(10L)).thenReturn(List.of());

        // no schedule conflict
        when(meetingTimeRepository.findBySubjectOfferingId(100L)).thenReturn(List.of());

        // capacity full: 30 occupied out of 30
        when(registrationRepository.countBySubjectOfferingIdAndStatusIn(eq(100L), anyList())).thenReturn(30L);

        AddDropService.AddDropResult result = addDropService.addCourse(student, 100L);

        assertThat(result.success()).isFalse();
        assertThat(result.errors()).contains("No places available");
        verify(registrationRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // dropCourse tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("dropCourse - success when registration exists")
    void dropCourse_success() {
        Registration reg = Registration.builder()
                .id(50L)
                .student(student)
                .subjectOffering(offering)
                .status(Registration.RegistrationStatus.CONFIRMED)
                .build();

        when(registrationRepository.findByStudentIdAndSubjectOfferingIdWithDetails(1L, 100L)).thenReturn(Optional.of(reg));

        // add/drop window active
        RegistrationWindow window = RegistrationWindow.builder()
                .id(1L)
                .semester(semester)
                .type(RegistrationWindow.WindowType.ADD_DROP)
                .startDate(LocalDate.now().minusDays(1))
                .endDate(LocalDate.now().plusDays(5))
                .active(true)
                .build();
        when(registrationWindowRepository.findBySemesterIdAndTypeAndActiveTrue(1L, RegistrationWindow.WindowType.ADD_DROP))
                .thenReturn(Optional.of(window));

        when(registrationRepository.save(any(Registration.class))).thenAnswer(inv -> inv.getArgument(0));

        AddDropService.AddDropResult result = addDropService.dropCourse(student, 100L);

        assertThat(result.success()).isTrue();
        assertThat(result.message()).isEqualTo("Subject dropped successfully");

        ArgumentCaptor<Registration> captor = ArgumentCaptor.forClass(Registration.class);
        verify(registrationRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(Registration.RegistrationStatus.DROPPED);
        assertThat(captor.getValue().getDroppedAt()).isNotNull();

        verify(notificationService).notifyStudent(eq("student@example.com"), eq(Notification.NotificationType.ENROLLMENT),
                any(), any(), any());
        verify(auditService).logStudentAction(eq(student), eq("ENROLLMENT_DROPPED"), any(), any(), any());
    }

    @Test
    @DisplayName("dropCourse - error when not registered")
    void dropCourse_notRegistered() {
        // dropCourse uses the WithDetails variant to eagerly load the offering
        when(registrationRepository.findByStudentIdAndSubjectOfferingIdWithDetails(1L, 100L)).thenReturn(Optional.empty());

        AddDropService.AddDropResult result = addDropService.dropCourse(student, 100L);

        assertThat(result.success()).isFalse();
        assertThat(result.message()).isEqualTo("Not registered for this subject");
        assertThat(result.errors()).containsExactly("Not registered for this subject");
        verify(registrationRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // hasActiveRegistrationHold tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("hasActiveRegistrationHold - returns true when student has financial debt")
    void hasActiveRegistrationHold_withFinancialDebt() {
        when(financialService.hasRegistrationLock(student)).thenReturn(true);

        boolean result = addDropService.hasActiveRegistrationHold(student);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("hasActiveRegistrationHold - returns false when no holds exist")
    void hasActiveRegistrationHold_noHolds() {
        when(financialService.hasRegistrationLock(student)).thenReturn(false);
        when(holdRepository.findByStudentIdAndActiveTrue(1L)).thenReturn(List.of());

        boolean result = addDropService.hasActiveRegistrationHold(student);

        assertThat(result).isFalse();
    }
}
