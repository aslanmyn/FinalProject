package ru.kors.finalproject.controller.api.v1;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.kors.finalproject.entity.*;
import ru.kors.finalproject.service.LaundryService;
import ru.kors.finalproject.web.api.v1.CurrentUserHelper;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/student/laundry")
@RequiredArgsConstructor
public class LaundryV1Controller {

    private final LaundryService laundryService;
    private final CurrentUserHelper currentUserHelper;

    // ===== Rooms & Machines =====

    @GetMapping("/rooms")
    public ResponseEntity<?> getRooms(@RequestParam(required = false) Long dormBuildingId) {
        List<LaundryRoom> rooms = dormBuildingId != null
                ? laundryService.getRoomsByDorm(dormBuildingId)
                : laundryService.getAllRooms();
        return ResponseEntity.ok(rooms.stream()
                .map(r -> new RoomDto(r.getId(), r.getName(), r.getTotalMachines(),
                        r.getDormBuilding() != null ? r.getDormBuilding().getId() : null))
                .toList());
    }

    @GetMapping("/rooms/{roomId}/availability")
    public ResponseEntity<?> getRoomAvailability(@PathVariable Long roomId) {
        return ResponseEntity.ok(laundryService.getRoomAvailability(roomId));
    }

    @GetMapping("/rooms/{roomId}/machines")
    public ResponseEntity<?> getMachines(@PathVariable Long roomId) {
        return ResponseEntity.ok(laundryService.getMachines(roomId).stream()
                .map(m -> new MachineDto(m.getId(), m.getMachineNumber(), m.getStatus().name()))
                .toList());
    }

    // ===== Bookings =====

    @GetMapping("/bookings")
    public ResponseEntity<?> getBookings(@AuthenticationPrincipal User user) {
        Student student = currentUserHelper.requireStudent(user);
        return ResponseEntity.ok(laundryService.getStudentBookings(student.getId()).stream()
                .map(this::toBookingDto)
                .toList());
    }

    @PostMapping("/bookings")
    public ResponseEntity<?> bookMachine(
            @AuthenticationPrincipal User user,
            @RequestBody BookingBody body) {
        Student student = currentUserHelper.requireStudent(user);
        LaundryBooking booking = laundryService.bookMachine(
                student, body.machineId(), body.startTime(), body.durationMinutes());
        return ResponseEntity.ok(toBookingDto(booking));
    }

    @PostMapping("/bookings/{id}/cancel")
    public ResponseEntity<?> cancelBooking(@AuthenticationPrincipal User user, @PathVariable Long id) {
        Student student = currentUserHelper.requireStudent(user);
        return ResponseEntity.ok(toBookingDto(laundryService.cancelBooking(id, student.getId())));
    }

    // ===== DTOs =====

    private BookingDto toBookingDto(LaundryBooking b) {
        return new BookingDto(b.getId(),
                b.getMachine() != null ? b.getMachine().getId() : null,
                b.getMachine() != null ? b.getMachine().getMachineNumber() : 0,
                b.getStatus().name(), b.getTimeSlotStart(), b.getTimeSlotEnd(), b.getCreatedAt());
    }

    public record RoomDto(Long id, String name, int totalMachines, Long dormBuildingId) {}
    public record MachineDto(Long id, int machineNumber, String status) {}
    public record BookingDto(Long id, Long machineId, int machineNumber, String status,
                             Instant timeSlotStart, Instant timeSlotEnd, Instant createdAt) {}
    public record BookingBody(Long machineId, Instant startTime, int durationMinutes) {}
}
