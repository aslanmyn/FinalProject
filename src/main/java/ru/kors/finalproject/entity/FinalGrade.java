package ru.kors.finalproject.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "final_grades", uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "subject_offering_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinalGrade {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_offering_id", nullable = false)
    private SubjectOffering subjectOffering;

    @Column(nullable = false)
    private double numericValue;

    private String letterValue;

    @Column(nullable = false)
    private double points;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private FinalGradeStatus status = FinalGradeStatus.CALCULATED;

    @Column(nullable = false)
    private boolean published;

    private Instant publishedAt;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant updatedAt;

    public enum FinalGradeStatus {
        CALCULATED,
        PUBLISHED,
        LOCKED
    }
}
