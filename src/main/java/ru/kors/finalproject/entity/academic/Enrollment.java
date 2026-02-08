package ru.kors.finalproject.entity.academic;

import jakarta.persistence.*;
import lombok.*;
import ru.kors.finalproject.entity.user.StudentProfile;

import java.time.Instant;

/**
 * Unique per student, section, and term.
 * Enrollment is the registration record.
 */
@Entity
@Table(name = "enrollments", uniqueConstraints = @UniqueConstraint(columnNames = {"student_profile_id", "course_section_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Enrollment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_profile_id", nullable = false)
    private StudentProfile studentProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_section_id", nullable = false)
    private CourseSection courseSection;

    @Enumerated(EnumType.STRING)
    private EnrollmentStatus status;

    private Instant createdAt;

    public enum EnrollmentStatus { DRAFT, SUBMITTED, CONFIRMED }
}
