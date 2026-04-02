package ru.kors.finalproject.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "laundry_rooms")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LaundryRoom {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dorm_building_id")
    private DormBuilding dormBuilding;

    @Column(nullable = false)
    @Builder.Default
    private int totalMachines = 12;
}
