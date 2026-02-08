package ru.kors.finalproject.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "mobility_applications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MobilityApplication {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    private String universityName;
    @Column(columnDefinition = "TEXT")
    private String disciplinesMapping;
    @Enumerated(EnumType.STRING)
    private MobilityStatus status;
    private Instant createdAt;

    public enum MobilityStatus { DRAFT, SUBMITTED, IN_REVIEW, APPROVED, REJECTED }
}
