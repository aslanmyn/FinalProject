package ru.kors.finalproject.entity.mobility;

import jakarta.persistence.*;
import lombok.*;
import ru.kors.finalproject.entity.user.StudentProfile;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "mobility_applications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MobilityApplication {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_profile_id", nullable = false)
    private StudentProfile studentProfile;

    private String hostUniversity;
    private String hostCountry;

    @Enumerated(EnumType.STRING)
    private MobilityStatus status;

    private Instant createdAt;

    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL)
    @Builder.Default
    private List<MobilityCourseMapping> courseMappings = new ArrayList<>();

    public enum MobilityStatus { DRAFT, SUBMITTED, IN_REVIEW, APPROVED, REJECTED, COMPLETED }
}
