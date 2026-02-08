package ru.kors.finalproject.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "clearance_checkpoints")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClearanceCheckpoint {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clearance_sheet_id", nullable = false)
    private ClearanceSheet clearanceSheet;

    private String department;
    @Enumerated(EnumType.STRING)
    private CheckpointStatus status;
    private String comment;

    public enum CheckpointStatus { PENDING, APPROVED, REJECTED }
}
