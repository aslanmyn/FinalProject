package ru.kors.finalproject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.kors.finalproject.entity.LaundryBooking;

import java.time.Instant;
import java.util.List;

public interface LaundryBookingRepository extends JpaRepository<LaundryBooking, Long> {

    @Query("SELECT b FROM LaundryBooking b JOIN FETCH b.machine WHERE b.student.id = :studentId ORDER BY b.timeSlotStart DESC")
    List<LaundryBooking> findByStudentId(Long studentId);

    @Query("SELECT b FROM LaundryBooking b WHERE b.machine.id = :machineId AND b.status IN ('BOOKED','IN_PROGRESS') AND b.timeSlotStart < :end AND b.timeSlotEnd > :start")
    List<LaundryBooking> findConflicting(Long machineId, Instant start, Instant end);

    @Query("SELECT b FROM LaundryBooking b WHERE b.machine.laundryRoom.id = :roomId AND b.status IN ('BOOKED','IN_PROGRESS') AND b.timeSlotStart < :end AND b.timeSlotEnd > :start")
    List<LaundryBooking> findActiveByRoomAndTimeRange(Long roomId, Instant start, Instant end);

    @Query("SELECT b FROM LaundryBooking b JOIN FETCH b.machine JOIN FETCH b.student WHERE b.id = :id")
    java.util.Optional<LaundryBooking> findByIdWithMachineAndStudent(Long id);
}
