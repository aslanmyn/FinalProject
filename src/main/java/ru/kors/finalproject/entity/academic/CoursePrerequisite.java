package ru.kors.finalproject.entity.academic;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "course_prerequisite_links", uniqueConstraints = @UniqueConstraint(columnNames = {"course_id", "prerequisite_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoursePrerequisite {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prerequisite_id", nullable = false)
    private Course prerequisite;
}
