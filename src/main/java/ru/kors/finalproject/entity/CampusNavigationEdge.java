package ru.kors.finalproject.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "campus_navigation_edges")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CampusNavigationEdge {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_room_id")
    private CampusRoom fromRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_room_id")
    private CampusRoom toRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_building_id")
    private CampusBuilding fromBuilding;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_building_id")
    private CampusBuilding toBuilding;

    @Column(nullable = false)
    private double distanceMeters;

    @Column(name = "is_accessible", nullable = false)
    @Builder.Default
    private boolean accessible = true;
}
