package ru.kors.finalproject.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "campus_buildings")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CampusBuilding {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 50)
    private String code;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(length = 100)
    private BuildingType buildingType;

    private Double latitude;
    private Double longitude;

    @Column(nullable = false)
    @Builder.Default
    private int floorCount = 1;

    private String imageUrl;

    public enum BuildingType {
        ACADEMIC, LIBRARY, LECTURE_HALL, LAB, ADMIN, DORM, CANTEEN, SPORT, OTHER
    }
}
