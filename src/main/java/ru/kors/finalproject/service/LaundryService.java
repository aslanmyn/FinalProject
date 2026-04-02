package ru.kors.finalproject.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.kors.finalproject.entity.*;
import ru.kors.finalproject.repository.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LaundryService {

    private final LaundryRoomRepository laundryRoomRepository;
    private final LaundryMachineRepository laundryMachineRepository;
    private final LaundryBookingRepository laundryBookingRepository;

    public List<LaundryRoom> getAllRooms() {
        return laundryRoomRepository.findAll();
    }

    public List<LaundryRoom> getRoomsByDorm(Long dormBuildingId) {
        return laundryRoomRepository.findByDormBuildingId(dormBuildingId);
    }

    public List<LaundryMachine> getMachines(Long roomId) {
        return laundryMachineRepository.findByLaundryRoomId(roomId);
    }

    public int getAvailableMachineCount(Long roomId) {
        return laundryMachineRepository.countByLaundryRoomIdAndStatus(roomId, LaundryMachine.MachineStatus.AVAILABLE);
    }

    public record RoomAvailability(Long roomId, String roomName, int totalMachines, int availableMachines, int inUse, int outOfOrder) {}

    public RoomAvailability getRoomAvailability(Long roomId) {
        LaundryRoom room = laundryRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Laundry room not found"));
        int available = laundryMachineRepository.countByLaundryRoomIdAndStatus(roomId, LaundryMachine.MachineStatus.AVAILABLE);
        int inUse = laundryMachineRepository.countByLaundryRoomIdAndStatus(roomId, LaundryMachine.MachineStatus.IN_USE);
        int outOfOrder = laundryMachineRepository.countByLaundryRoomIdAndStatus(roomId, LaundryMachine.MachineStatus.OUT_OF_ORDER);
        return new RoomAvailability(room.getId(), room.getName(), room.getTotalMachines(), available, inUse, outOfOrder);
    }

    public List<LaundryBooking> getStudentBookings(Long studentId) {
        return laundryBookingRepository.findByStudentId(studentId);
    }

    @Transactional
    public LaundryBooking bookMachine(Student student, Long machineId, Instant startTime, int durationMinutes) {
        if (durationMinutes < 30 || durationMinutes > 120) {
            throw new IllegalArgumentException("Duration must be between 30 and 120 minutes");
        }
        if (startTime.isBefore(Instant.now())) {
            throw new IllegalArgumentException("Start time must be in the future");
        }

        LaundryMachine machine = laundryMachineRepository.findById(machineId)
                .orElseThrow(() -> new IllegalArgumentException("Machine not found"));

        if (machine.getStatus() == LaundryMachine.MachineStatus.OUT_OF_ORDER) {
            throw new IllegalStateException("Machine is out of order");
        }

        Instant endTime = startTime.plus(durationMinutes, ChronoUnit.MINUTES);

        List<LaundryBooking> conflicts = laundryBookingRepository.findConflicting(machineId, startTime, endTime);
        if (!conflicts.isEmpty()) {
            throw new IllegalStateException("Time slot is already booked for this machine");
        }

        LaundryBooking booking = LaundryBooking.builder()
                .student(student)
                .machine(machine)
                .timeSlotStart(startTime)
                .timeSlotEnd(endTime)
                .status(LaundryBooking.BookingStatus.BOOKED)
                .createdAt(Instant.now())
                .build();
        return laundryBookingRepository.save(booking);
    }

    @Transactional
    public LaundryBooking cancelBooking(Long bookingId, Long studentId) {
        LaundryBooking booking = laundryBookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
        if (!booking.getStudent().getId().equals(studentId)) {
            throw new IllegalArgumentException("Access denied");
        }
        if (booking.getStatus() != LaundryBooking.BookingStatus.BOOKED) {
            throw new IllegalStateException("Only booked slots can be cancelled");
        }
        booking.setStatus(LaundryBooking.BookingStatus.CANCELLED);
        return laundryBookingRepository.save(booking);
    }
}
