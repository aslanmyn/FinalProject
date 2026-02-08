package ru.kors.finalproject.entity.assessment;

import jakarta.persistence.*;
import lombok.*;
import ru.kors.finalproject.entity.academic.CourseSection;
import ru.kors.finalproject.entity.user.StudentProfile;

import java.time.Instant;

/**
 * FX = Final Exam / Retake.
 * FXApplication may require payment before approval.
 */
@Entity
@Table(name = "fx_applications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FXApplication {
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
    private FXStatus status;

    private boolean paymentRequired;
    private boolean paymentCompleted;

    private Instant createdAt;

    public enum FXStatus { PENDING, PAYMENT_REQUIRED, APPROVED, REJECTED, COMPLETED }
}
