package ru.kors.finalproject.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "assessment_components")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssessmentComponent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_offering_id", nullable = false)
    private SubjectOffering subjectOffering;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ComponentType type;

    @Column(nullable = false)
    private double weightPercent;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ComponentStatus status = ComponentStatus.DRAFT;

    @Column(nullable = false)
    private boolean published;

    @Column(nullable = false)
    private boolean locked;

    @Column(nullable = false)
    private Instant createdAt;

    public enum ComponentType {
        QUIZ,
        MIDTERM,
        FINAL,
        LAB,
        PROJECT,
        OTHER
    }

    public enum ComponentStatus {
        DRAFT,
        PUBLISHED,
        LOCKED
    }
}
