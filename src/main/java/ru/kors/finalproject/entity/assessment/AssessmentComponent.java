package ru.kors.finalproject.entity.assessment;

import jakarta.persistence.*;
import lombok.*;
import ru.kors.finalproject.entity.academic.CourseSection;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_section_id", nullable = false)
    private CourseSection courseSection;

    @Enumerated(EnumType.STRING)
    private ComponentType type;

    private double weightPercent;  // e.g. 20 for 20%
    private double maxPoints;

    public enum ComponentType { QUIZ, MIDTERM, FINAL, LAB, ATTENDANCE }
}
