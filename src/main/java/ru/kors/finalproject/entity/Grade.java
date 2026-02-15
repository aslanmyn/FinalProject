package ru.kors.finalproject.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "grades")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Grade {
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

    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assessment_component_id")
    private AssessmentComponent component;

    @Enumerated(EnumType.STRING)
    @Column(name = "grade_type")
    private GradeType type;
    
    @Column(name = "grade_value", nullable = false)
    private double gradeValue;
    
    @Column(name = "max_grade_value", nullable = false)
    private double maxGradeValue;
    
    @Column(length = 500)
    private String comment;

    @Column(nullable = false)
    private boolean published;
    
    @Column(name = "created_at")
    private Instant createdAt;

    public enum GradeType { QUIZ, MIDTERM, FINAL, LAB }
}
