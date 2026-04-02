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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LaundryServiceTest {

    @Mock private LaundryRoomRepository laundryRoomRepository;
    @Mock private LaundryMachineRepository laundryMachineRepository;
    @Mock private LaundryBookingRepository laundryBookingRepository;

    @InjectMocks
    private LaundryService laundryService;

    private Student student;
    private LaundryMachine machine;

    @BeforeEach
    void setUp() {
        student = Student.builder().id(1L).email("test@kbtu.kz").name("Test Student").build();
        machine = LaundryMachine.builder().id(1L).machineNumber(1)
                .status(LaundryMachine.MachineStatus.AVAILABLE).build();
    }

    // =========================================================================
    // bookMachine
    // =========================================================================

    @Test
    @DisplayName("bookMachine - creates booking for available slot")
    void bookMachine_success() {
        Instant start = Instant.now().plus(1, ChronoUnit.HOURS);
        when(laundryMachineRepository.findById(1L)).thenReturn(Optional.of(machine));
        when(laundryBookingRepository.findConflicting(eq(1L), any(), any())).thenReturn(List.of());
        when(laundryBookingRepository.save(any(LaundryBooking.class))).thenAnswer(inv -> {
            LaundryBooking b = inv.getArgument(0);
            b.setId(100L);
            return b;
        });

        LaundryBooking result = laundryService.bookMachine(student, 1L, start, 60);

        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getStatus()).isEqualTo(LaundryBooking.BookingStatus.BOOKED);
        assertThat(result.getTimeSlotEnd()).isEqualTo(start.plus(60, ChronoUnit.MINUTES));
    }

    @Test
    @DisplayName("bookMachine - throws when duration is too short")
    void bookMachine_durationTooShort() {
        Instant start = Instant.now().plus(1, ChronoUnit.HOURS);
        assertThatThrownBy(() -> laundryService.bookMachine(student, 1L, start, 20))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("30 and 120");
    }

    @Test
    @DisplayName("bookMachine - throws when duration is too long")
    void bookMachine_durationTooLong() {
        Instant start = Instant.now().plus(1, ChronoUnit.HOURS);
        assertThatThrownBy(() -> laundryService.bookMachine(student, 1L, start, 150))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("30 and 120");
    }

    @Test
    @DisplayName("bookMachine - throws when start time is in the past")
    void bookMachine_pastTime() {
        Instant past = Instant.now().minus(1, ChronoUnit.HOURS);
        assertThatThrownBy(() -> laundryService.bookMachine(student, 1L, past, 60))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("future");
    }

    @Test
    @DisplayName("bookMachine - throws when machine is out of order")
    void bookMachine_outOfOrder() {
        machine.setStatus(LaundryMachine.MachineStatus.OUT_OF_ORDER);
        Instant start = Instant.now().plus(1, ChronoUnit.HOURS);
        when(laundryMachineRepository.findById(1L)).thenReturn(Optional.of(machine));

        assertThatThrownBy(() -> laundryService.bookMachine(student, 1L, start, 60))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("out of order");
    }

    @Test
    @DisplayName("bookMachine - throws when time slot conflicts")
    void bookMachine_conflict() {
        Instant start = Instant.now().plus(1, ChronoUnit.HOURS);
        when(laundryMachineRepository.findById(1L)).thenReturn(Optional.of(machine));
        LaundryBooking existing = LaundryBooking.builder().id(50L).build();
        when(laundryBookingRepository.findConflicting(eq(1L), any(), any())).thenReturn(List.of(existing));

        assertThatThrownBy(() -> laundryService.bookMachine(student, 1L, start, 60))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already booked");
    }

    // =========================================================================
    // cancelBooking
    // =========================================================================

    @Test
    @DisplayName("cancelBooking - cancels own booked slot")
    void cancelBooking_success() {
        LaundryBooking booking = LaundryBooking.builder().id(100L).student(student)
                .machine(machine).status(LaundryBooking.BookingStatus.BOOKED).build();
        when(laundryBookingRepository.findById(100L)).thenReturn(Optional.of(booking));
        when(laundryBookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LaundryBooking result = laundryService.cancelBooking(100L, 1L);
        assertThat(result.getStatus()).isEqualTo(LaundryBooking.BookingStatus.CANCELLED);
    }

    @Test
    @DisplayName("cancelBooking - throws when not the owner")
    void cancelBooking_notOwner() {
        Student other = Student.builder().id(2L).build();
        LaundryBooking booking = LaundryBooking.builder().id(100L).student(other)
                .status(LaundryBooking.BookingStatus.BOOKED).build();
        when(laundryBookingRepository.findById(100L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> laundryService.cancelBooking(100L, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Access denied");
    }

    @Test
    @DisplayName("cancelBooking - throws when booking is not in BOOKED status")
    void cancelBooking_notBooked() {
        LaundryBooking booking = LaundryBooking.builder().id(100L).student(student)
                .status(LaundryBooking.BookingStatus.IN_PROGRESS).build();
        when(laundryBookingRepository.findById(100L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> laundryService.cancelBooking(100L, 1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("booked");
    }

    // =========================================================================
    // getRoomAvailability
    // =========================================================================

    @Test
    @DisplayName("getRoomAvailability - returns correct counts")
    void getRoomAvailability_success() {
        LaundryRoom room = LaundryRoom.builder().id(1L).name("Main Laundry").totalMachines(12).build();
        when(laundryRoomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(laundryMachineRepository.countByLaundryRoomIdAndStatus(1L, LaundryMachine.MachineStatus.AVAILABLE)).thenReturn(4);
        when(laundryMachineRepository.countByLaundryRoomIdAndStatus(1L, LaundryMachine.MachineStatus.IN_USE)).thenReturn(7);
        when(laundryMachineRepository.countByLaundryRoomIdAndStatus(1L, LaundryMachine.MachineStatus.OUT_OF_ORDER)).thenReturn(1);

        LaundryService.RoomAvailability result = laundryService.getRoomAvailability(1L);

        assertThat(result.totalMachines()).isEqualTo(12);
        assertThat(result.availableMachines()).isEqualTo(4);
        assertThat(result.inUse()).isEqualTo(7);
        assertThat(result.outOfOrder()).isEqualTo(1);
    }
}
