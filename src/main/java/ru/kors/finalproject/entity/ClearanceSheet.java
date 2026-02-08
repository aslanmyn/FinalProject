package ru.kors.finalproject.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "clearance_sheets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClearanceSheet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Enumerated(EnumType.STRING)
    private ClearanceStatus status;

    @OneToMany(mappedBy = "clearanceSheet", cascade = CascadeType.ALL)
    @Builder.Default
    private List<ClearanceCheckpoint> checkpoints = new ArrayList<>();

    public enum ClearanceStatus { IN_PROGRESS, CLEARED }
}
