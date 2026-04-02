package ru.kors.finalproject.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "laundry_machines", uniqueConstraints = @UniqueConstraint(columnNames = {"laundry_room_id", "machine_number"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LaundryMachine {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "laundry_room_id", nullable = false)
    private LaundryRoom laundryRoom;

    @Column(nullable = false)
    private int machineNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private MachineStatus status = MachineStatus.AVAILABLE;

    public enum MachineStatus { AVAILABLE, IN_USE, OUT_OF_ORDER }
}
