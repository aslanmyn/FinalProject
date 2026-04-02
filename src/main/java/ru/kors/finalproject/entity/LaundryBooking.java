package ru.kors.finalproject.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "laundry_bookings")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LaundryBooking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "machine_id", nullable = false)
    private LaundryMachine machine;

    @Column(nullable = false)
    private Instant timeSlotStart;

    @Column(nullable = false)
    private Instant timeSlotEnd;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private BookingStatus status = BookingStatus.BOOKED;

    @Column(nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    public enum BookingStatus { BOOKED, IN_PROGRESS, COMPLETED, CANCELLED }
}
