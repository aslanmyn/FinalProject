package ru.kors.finalproject.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.kors.finalproject.entity.DormApplication;
import ru.kors.finalproject.entity.DormBuilding;
import ru.kors.finalproject.entity.DormRoom;
import ru.kors.finalproject.entity.Student;
import ru.kors.finalproject.repository.DormApplicationRepository;
import ru.kors.finalproject.repository.DormBuildingRepository;
import ru.kors.finalproject.repository.DormRoomRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DormServiceTest {

    @Mock
    private DormApplicationRepository dormApplicationRepository;
    @Mock
    private DormRoomRepository dormRoomRepository;
    @Mock
    private DormBuildingRepository dormBuildingRepository;

    @InjectMocks
    private DormService dormService;

    private Student student;
    private DormRoom availableRoom;
    private DormRoom fullRoom;
    private DormApplication draftApplication;

    @BeforeEach
    void setUp() {
        student = Student.builder()
                .id(10L)
                .email("a_student@kbtu.kz")
                .name("Student Example")
                .build();

        DormBuilding building = DormBuilding.builder()
                .id(1L)
                .name("North Residence")
                .build();

        availableRoom = DormRoom.builder()
                .id(100L)
                .dormBuilding(building)
                .roomNumber("203")
                .floor(2)
                .roomType(DormRoom.RoomType.SINGLE_SUITE)
                .pricePerSemester(BigDecimal.valueOf(690000))
                .capacity(1)
                .occupied(0)
                .build();

        fullRoom = DormRoom.builder()
                .id(101L)
                .dormBuilding(building)
                .roomNumber("101")
                .floor(1)
                .roomType(DormRoom.RoomType.DOUBLE_ROOM)
                .pricePerSemester(BigDecimal.valueOf(420000))
                .capacity(2)
                .occupied(2)
                .build();

        draftApplication = DormApplication.builder()
                .id(77L)
                .student(student)
                .status(DormApplication.ApplicationStatus.DRAFT)
                .currentStep(1)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("createApplication rejects a second active application")
    void createApplication_rejectsDuplicateActiveApplication() {
        when(dormApplicationRepository.existsByStudentIdAndStatusIn(
                student.getId(),
                List.of(
                        DormApplication.ApplicationStatus.DRAFT,
                        DormApplication.ApplicationStatus.SUBMITTED,
                        DormApplication.ApplicationStatus.APPROVED
                )))
                .thenReturn(true);

        assertThatThrownBy(() -> dormService.createApplication(student))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("active dorm application");
    }

    @Test
    @DisplayName("update steps advance wizard progress through all dorm stages")
    void updateSteps_advanceCurrentStep() {
        when(dormApplicationRepository.findByIdAndStudentId(77L, student.getId()))
                .thenReturn(Optional.of(draftApplication));
        when(dormRoomRepository.findById(availableRoom.getId()))
                .thenReturn(Optional.of(availableRoom));
        when(dormApplicationRepository.save(any(DormApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        int step1Progress = dormService.updateStep1(77L, student.getId(), "Mother", "+77001234567", "No").getCurrentStep();
        DormApplication step2 = dormService.updateStep2(77L, student.getId(), DormRoom.RoomType.SINGLE_SUITE, availableRoom.getId());
        int step2Progress = step2.getCurrentStep();
        DormApplication step3 = dormService.updateStep3(77L, student.getId(), "Early Bird", "Quiet", "22B030406");

        assertThat(step1Progress).isEqualTo(2);
        assertThat(step2Progress).isEqualTo(3);
        assertThat(step2.getDormRoom()).isEqualTo(availableRoom);
        assertThat(step3.getCurrentStep()).isEqualTo(4);
        assertThat(step3.getPreferredRoommateUid()).isEqualTo("22B030406");
    }

    @Test
    @DisplayName("updateStep2 rejects a full room")
    void updateStep2_rejectsFullRoom() {
        when(dormApplicationRepository.findByIdAndStudentId(77L, student.getId()))
                .thenReturn(Optional.of(draftApplication));
        when(dormRoomRepository.findById(fullRoom.getId()))
                .thenReturn(Optional.of(fullRoom));

        assertThatThrownBy(() -> dormService.updateStep2(
                77L,
                student.getId(),
                DormRoom.RoomType.DOUBLE_ROOM,
                fullRoom.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("full");
    }

    @Test
    @DisplayName("submitApplication requires accepted terms and room type preference")
    void submitApplication_requiresTermsAndRoomPreference() {
        when(dormApplicationRepository.findByIdAndStudentId(77L, student.getId()))
                .thenReturn(Optional.of(draftApplication));

        assertThatThrownBy(() -> dormService.submitApplication(77L, student.getId(), false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("accept the terms");

        draftApplication.setTermsAccepted(false);
        draftApplication.setRoomTypePreference(null);

        assertThatThrownBy(() -> dormService.submitApplication(77L, student.getId(), true))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Room type preference");
    }

    @Test
    @DisplayName("cancelApplication frees occupancy for an approved assigned room")
    void cancelApplication_releasesRoomOccupancy() {
        availableRoom.setOccupied(1);
        DormApplication approved = DormApplication.builder()
                .id(88L)
                .student(student)
                .dormRoom(availableRoom)
                .status(DormApplication.ApplicationStatus.APPROVED)
                .currentStep(4)
                .build();

        when(dormApplicationRepository.findByIdAndStudentId(88L, student.getId()))
                .thenReturn(Optional.of(approved));
        when(dormRoomRepository.save(any(DormRoom.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(dormApplicationRepository.save(any(DormApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DormApplication cancelled = dormService.cancelApplication(88L, student.getId());

        assertThat(cancelled.getStatus()).isEqualTo(DormApplication.ApplicationStatus.CANCELLED);
        assertThat(availableRoom.getOccupied()).isZero();
        verify(dormRoomRepository).save(availableRoom);
    }
}
