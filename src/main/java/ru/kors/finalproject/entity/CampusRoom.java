package ru.kors.finalproject.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "campus_rooms", uniqueConstraints = @UniqueConstraint(columnNames = {"building_id", "room_number"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CampusRoom {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "building_id", nullable = false)
    private CampusBuilding building;

    @Column(nullable = false, length = 50)
    private String roomNumber;

    @Column(nullable = false)
    private int floor;

    @Enumerated(EnumType.STRING)
    @Column(length = 100)
    private CampusRoomType roomType;

    private String name;
    private String description;
    private Integer capacity;
    private Double latitude;
    private Double longitude;

    public enum CampusRoomType {
        CLASSROOM, LECTURE_HALL, LAB, OFFICE, LIBRARY, COMPUTER_CENTER, CANTEEN, OTHER
    }
}
