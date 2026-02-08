package ru.kors.finalproject.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "subject_prerequisites", uniqueConstraints = @UniqueConstraint(columnNames = {"subject_id", "prerequisite_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubjectPrerequisite {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prerequisite_id", nullable = false)
    private Subject prerequisite;
}
