package ru.kors.finalproject.entity.mobility;

import jakarta.persistence.*;
import lombok.*;
import ru.kors.finalproject.entity.academic.Course;

@Entity
@Table(name = "mobility_course_mappings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MobilityCourseMapping {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private MobilityApplication application;

    private String externalCourseCode;
    private String externalCourseName;
    private int externalCredits;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "internal_course_id")
    private Course internalCourse;
}
