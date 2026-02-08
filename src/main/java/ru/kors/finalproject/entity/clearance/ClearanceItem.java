package ru.kors.finalproject.entity.clearance;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "clearance_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClearanceItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clearance_process_id", nullable = false)
    private ClearanceProcess clearanceProcess;

    private String department;
    @Enumerated(EnumType.STRING)
    private ItemStatus status;
    private String comment;

    public enum ItemStatus { PENDING, APPROVED, REJECTED }
}
