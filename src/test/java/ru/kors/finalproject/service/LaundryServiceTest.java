package ru.kors.finalproject.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.kors.finalproject.entity.LaundryBooking;
import ru.kors.finalproject.entity.LaundryMachine;
import ru.kors.finalproject.entity.LaundryRoom;
import ru.kors.finalproject.entity.Student;
import ru.kors.finalproject.repository.LaundryBookingRepository;
import ru.kors.finalproject.repository.LaundryMachineRepository;
import ru.kors.finalproject.repository.LaundryRoomRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LaundryServiceTest {

    @Mock
    private LaundryRoomRepository laundryRoomRepository;
    @Mock
    private LaundryMachineRepository laundryMachineRepository;
    @Mock
    private LaundryBookingRepository laundryBookingRepository;

    @InjectMocks
    private LaundryService laundryService;

    private Student student;
    private LaundryMachine machine;

    @BeforeEach
    void setUp() {
        student = Student.builder().id(20L).email("a_student@kbtu.kz").name("Student Example").build();
        LaundryRoom room = LaundryRoom.builder().id(1L).name("North Residence Laundry").totalMachines(6).build();
        machine = LaundryMachine.builder()
                .id(50L)
                .laundryRoom(room)
                .machineNumber(3)
                .status(LaundryMachine.MachineStatus.AVAILABLE)
                .build();
    }

    @Test
    @DisplayName("bookMachine validates duration boundaries")
    void bookMachine_validatesDuration() {
        assertThatThrownBy(() -> laundryService.bookMachine(student, 50L, Instant.now().plus(1, ChronoUnit.HOURS), 20))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("between 30 and 120");
    }

    @Test
    @DisplayName("bookMachine rejects past booking times")
    void bookMachine_rejectsPastTime() {
        assertThatThrownBy(() -> laundryService.bookMachine(student, 50L, Instant.now().minus(1, ChronoUnit.MINUTES), 60))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("future");
    }

    @Test
    @DisplayName("bookMachine rejects out-of-order machines")
    void bookMachine_rejectsOutOfOrderMachine() {
        machine.setStatus(LaundryMachine.MachineStatus.OUT_OF_ORDER);
        when(laundryMachineRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(machine));

        assertThatThrownBy(() -> laundryService.bookMachine(student, 50L, Instant.now().plus(2, ChronoUnit.HOURS), 60))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("out of order");
    }

    @Test
    @DisplayName("bookMachine rejects conflicting time slots")
    void bookMachine_rejectsConflict() {
        when(laundryMachineRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(machine));
        when(laundryBookingRepository.findConflicting(any(), any(), any()))
                .thenReturn(List.of(LaundryBooking.builder().id(1L).build()));

        assertThatThrownBy(() -> laundryService.bookMachine(student, 50L, Instant.now().plus(2, ChronoUnit.HOURS), 60))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already booked");
    }

    @Test
    @DisplayName("bookMachine creates a booked slot with calculated end time")
    void bookMachine_success() {
        Instant start = Instant.now().plus(2, ChronoUnit.HOURS).truncatedTo(ChronoUnit.MINUTES);
        when(laundryMachineRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(machine));
        when(laundryBookingRepository.findConflicting(any(), any(), any())).thenReturn(List.of());
        when(laundryBookingRepository.save(any(LaundryBooking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LaundryBooking booking = laundryService.bookMachine(student, 50L, start, 90);

        assertThat(booking.getStatus()).isEqualTo(LaundryBooking.BookingStatus.BOOKED);
        assertThat(booking.getTimeSlotStart()).isEqualTo(start);
        assertThat(booking.getTimeSlotEnd()).isEqualTo(start.plus(90, ChronoUnit.MINUTES));
    }

    @Test
    @DisplayName("cancelBooking enforces ownership")
    void cancelBooking_enforcesOwnership() {
        Student anotherStudent = Student.builder().id(99L).build();
        LaundryBooking booking = LaundryBooking.builder()
                .id(5L)
                .student(anotherStudent)
                .machine(machine)
                .status(LaundryBooking.BookingStatus.BOOKED)
                .build();
        when(laundryBookingRepository.findByIdWithMachineAndStudent(5L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> laundryService.cancelBooking(5L, student.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Access denied");
    }
}
