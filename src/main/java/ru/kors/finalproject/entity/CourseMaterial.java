package ru.kors.finalproject.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "course_materials")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseMaterial {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_offering_id", nullable = false)
    private SubjectOffering subjectOffering;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by_teacher_id", nullable = false)
    private Teacher uploadedBy;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String originalFileName;

    @Column(nullable = false)
    private String storagePath;

    private String contentType;

    private long sizeBytes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private MaterialVisibility visibility = MaterialVisibility.ENROLLED_ONLY;

    @Builder.Default
    private boolean published = true;

    private Instant createdAt;

    private Instant updatedAt;

    public enum MaterialVisibility {
        ENROLLED_ONLY,
        PUBLIC
    }
}
