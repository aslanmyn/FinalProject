package ru.kors.finalproject.entity.clearance;

import jakarta.persistence.*;
import lombok.*;
import ru.kors.finalproject.entity.user.StudentProfile;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "clearance_processes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClearanceProcess {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_profile_id", nullable = false)
    private StudentProfile studentProfile;

    @Enumerated(EnumType.STRING)
    private ClearanceStatus status;

    @OneToMany(mappedBy = "clearanceProcess", cascade = CascadeType.ALL)
    @Builder.Default
    private List<ClearanceItem> items = new ArrayList<>();

    public enum ClearanceStatus { IN_PROGRESS, CLEARED }
}
