package ru.kors.finalproject.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "attendances", uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "subject_offering_id", "date"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Attendance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_offering_id", nullable = false)
    private SubjectOffering subjectOffering;

    private LocalDate date;
    @Enumerated(EnumType.STRING)
    private AttendanceStatus status;
    private String reason;

    public enum AttendanceStatus { PRESENT, LATE, ABSENT }
}
