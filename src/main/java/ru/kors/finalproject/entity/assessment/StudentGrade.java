package ru.kors.finalproject.entity.assessment;

import jakarta.persistence.*;
import lombok.*;
import ru.kors.finalproject.entity.academic.CourseSection;
import ru.kors.finalproject.entity.user.StudentProfile;

import java.time.Instant;

@Entity
@Table(name = "student_grades")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentGrade {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_profile_id", nullable = false)
    private StudentProfile studentProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_section_id", nullable = false)
    private CourseSection courseSection;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "component_id", nullable = false)
    private AssessmentComponent component;

    private double points;
    private String comment;
    private Instant recordedAt;
}
