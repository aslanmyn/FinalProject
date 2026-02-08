package ru.kors.finalproject.entity.assessment;

import jakarta.persistence.*;
import lombok.*;
import ru.kors.finalproject.entity.academic.CourseSection;
import ru.kors.finalproject.entity.user.StudentProfile;

/**
 * Calculated from StudentGrade components.
 */
@Entity
@Table(name = "final_grades", uniqueConstraints = @UniqueConstraint(columnNames = {"student_profile_id", "course_section_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinalGrade {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_profile_id", nullable = false)
    private StudentProfile studentProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_section_id", nullable = false)
    private CourseSection courseSection;

    private double gradePoints;  // e.g. 4.0 GPA scale
    private String letterGrade;  // e.g. A, B, C
    private boolean passed;
}
