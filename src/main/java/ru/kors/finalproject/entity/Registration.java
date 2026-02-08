package ru.kors.finalproject.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "registrations", uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "subject_offering_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Registration {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_offering_id", nullable = false)
    private SubjectOffering subjectOffering;

    @Enumerated(EnumType.STRING)
    private RegistrationStatus status;

    private Instant createdAt;

    public enum RegistrationStatus { DRAFT, SUBMITTED, CONFIRMED }
}
