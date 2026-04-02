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

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DormServiceTest {

    @Mock private DormApplicationRepository dormApplicationRepository;
    @Mock private DormRoomRepository dormRoomRepository;
    @Mock private DormBuildingRepository dormBuildingRepository;

    @InjectMocks
    private DormService dormService;

    private Student student;

    @BeforeEach
    void setUp() {
        student = Student.builder().id(1L).email("test@kbtu.kz").name("Test Student").build();
    }

    // =========================================================================
    // createApplication
    // =========================================================================

    @Test
    @DisplayName("createApplication - creates draft when no active application exists")
    void createApplication_success() {
        when(dormApplicationRepository.existsByStudentIdAndStatusIn(eq(1L), anyList())).thenReturn(false);
        when(dormApplicationRepository.save(any(DormApplication.class))).thenAnswer(inv -> {
            DormApplication a = inv.getArgument(0);
            a.setId(100L);
            return a;
        });

        DormApplication result = dormService.createApplication(student);

        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getStatus()).isEqualTo(DormApplication.ApplicationStatus.DRAFT);
        assertThat(result.getCurrentStep()).isEqualTo(1);
        verify(dormApplicationRepository).save(any(DormApplication.class));
    }

    @Test
    @DisplayName("createApplication - throws when active application exists")
    void createApplication_alreadyExists() {
        when(dormApplicationRepository.existsByStudentIdAndStatusIn(eq(1L), anyList())).thenReturn(true);

        assertThatThrownBy(() -> dormService.createApplication(student))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already have an active");
    }

    // =========================================================================
    // updateStep2 - room selection
    // =========================================================================

    @Test
    @DisplayName("updateStep2 - sets room type preference and assigns room")
    void updateStep2_success() {
        DormApplication app = DormApplication.builder().id(10L).student(student)
                .status(DormApplication.ApplicationStatus.DRAFT).currentStep(1).build();
        DormRoom room = DormRoom.builder().id(5L).roomType(DormRoom.RoomType.SINGLE_SUITE)
                .capacity(1).occupied(0).pricePerSemester(new BigDecimal("550000")).build();

        when(dormApplicationRepository.findByIdAndStudentId(10L, 1L)).thenReturn(Optional.of(app));
        when(dormRoomRepository.findById(5L)).thenReturn(Optional.of(room));
        when(dormApplicationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DormApplication result = dormService.updateStep2(10L, 1L, DormRoom.RoomType.SINGLE_SUITE, 5L);

        assertThat(result.getRoomTypePreference()).isEqualTo(DormRoom.RoomType.SINGLE_SUITE);
        assertThat(result.getDormRoom()).isEqualTo(room);
        assertThat(result.getCurrentStep()).isGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("updateStep2 - throws when room is full")
    void updateStep2_roomFull() {
        DormApplication app = DormApplication.builder().id(10L).student(student)
                .status(DormApplication.ApplicationStatus.DRAFT).currentStep(1).build();
        DormRoom room = DormRoom.builder().id(5L).capacity(1).occupied(1).build();

        when(dormApplicationRepository.findByIdAndStudentId(10L, 1L)).thenReturn(Optional.of(app));
        when(dormRoomRepository.findById(5L)).thenReturn(Optional.of(room));

        assertThatThrownBy(() -> dormService.updateStep2(10L, 1L, DormRoom.RoomType.SINGLE_SUITE, 5L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("full");
    }

    // =========================================================================
    // updateStep3 - roommate preferences
    // =========================================================================

    @Test
    @DisplayName("updateStep3 - sets roommate preferences")
    void updateStep3_success() {
        DormApplication app = DormApplication.builder().id(10L).student(student)
                .status(DormApplication.ApplicationStatus.DRAFT).currentStep(2).build();

        when(dormApplicationRepository.findByIdAndStudentId(10L, 1L)).thenReturn(Optional.of(app));
        when(dormApplicationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DormApplication result = dormService.updateStep3(10L, 1L, "Early Bird", "Quiet", "UID123");

        assertThat(result.getSleepSchedule()).isEqualTo("Early Bird");
        assertThat(result.getStudyEnvironment()).isEqualTo("Quiet");
        assertThat(result.getPreferredRoommateUid()).isEqualTo("UID123");
    }

    // =========================================================================
    // submitApplication
    // =========================================================================

    @Test
    @DisplayName("submitApplication - transitions to SUBMITTED when valid")
    void submitApplication_success() {
        DormApplication app = DormApplication.builder().id(10L).student(student)
                .status(DormApplication.ApplicationStatus.DRAFT).currentStep(3)
                .roomTypePreference(DormRoom.RoomType.DOUBLE_ROOM).build();

        when(dormApplicationRepository.findByIdAndStudentId(10L, 1L)).thenReturn(Optional.of(app));
        when(dormApplicationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DormApplication result = dormService.submitApplication(10L, 1L, true);

        assertThat(result.getStatus()).isEqualTo(DormApplication.ApplicationStatus.SUBMITTED);
        assertThat(result.isTermsAccepted()).isTrue();
        assertThat(result.getCurrentStep()).isEqualTo(4);
    }

    @Test
    @DisplayName("submitApplication - throws when terms not accepted")
    void submitApplication_noTerms() {
        DormApplication app = DormApplication.builder().id(10L).student(student)
                .status(DormApplication.ApplicationStatus.DRAFT).currentStep(3)
                .roomTypePreference(DormRoom.RoomType.DOUBLE_ROOM).build();

        when(dormApplicationRepository.findByIdAndStudentId(10L, 1L)).thenReturn(Optional.of(app));

        assertThatThrownBy(() -> dormService.submitApplication(10L, 1L, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("terms");
    }

    @Test
    @DisplayName("submitApplication - throws when room type not selected")
    void submitApplication_noRoomType() {
        DormApplication app = DormApplication.builder().id(10L).student(student)
                .status(DormApplication.ApplicationStatus.DRAFT).currentStep(3).build();

        when(dormApplicationRepository.findByIdAndStudentId(10L, 1L)).thenReturn(Optional.of(app));

        assertThatThrownBy(() -> dormService.submitApplication(10L, 1L, true))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Room type");
    }

    // =========================================================================
    // cancelApplication
    // =========================================================================

    @Test
    @DisplayName("cancelApplication - frees room when approved application is cancelled")
    void cancelApplication_freesRoom() {
        DormRoom room = DormRoom.builder().id(5L).capacity(2).occupied(1).build();
        DormApplication app = DormApplication.builder().id(10L).student(student)
                .status(DormApplication.ApplicationStatus.APPROVED).dormRoom(room).build();

        when(dormApplicationRepository.findByIdAndStudentId(10L, 1L)).thenReturn(Optional.of(app));
        when(dormApplicationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DormApplication result = dormService.cancelApplication(10L, 1L);

        assertThat(result.getStatus()).isEqualTo(DormApplication.ApplicationStatus.CANCELLED);
        assertThat(room.getOccupied()).isEqualTo(0);
        verify(dormRoomRepository).save(room);
    }

    // =========================================================================
    // validateDraft - modification after submission
    // =========================================================================

    @Test
    @DisplayName("updateStep1 - throws when application is not in DRAFT")
    void updateStep_notDraft() {
        DormApplication app = DormApplication.builder().id(10L).student(student)
                .status(DormApplication.ApplicationStatus.SUBMITTED).build();

        when(dormApplicationRepository.findByIdAndStudentId(10L, 1L)).thenReturn(Optional.of(app));

        assertThatThrownBy(() -> dormService.updateStep1(10L, 1L, "Name", "Phone", null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DRAFT");
    }
}
