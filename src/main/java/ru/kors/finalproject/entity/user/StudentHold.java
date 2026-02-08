package ru.kors.finalproject.entity.user;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Blocks student actions (registration, FX, exams) when active.
 * StudentHolds must be checked before registration, FX, and exams.
 */
@Entity
@Table(name = "student_holds")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentHold {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_profile_id", nullable = false)
    private StudentProfile studentProfile;

    @Enumerated(EnumType.STRING)
    private HoldType type;

    private String reason;
    private Instant createdAt;
    private boolean active;

    public enum HoldType { FINANCIAL, ACADEMIC, DOCUMENT }
}
