package ru.kors.finalproject.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "dorm_rooms", uniqueConstraints = @UniqueConstraint(columnNames = {"dorm_building_id", "room_number"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DormRoom {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dorm_building_id", nullable = false)
    private DormBuilding dormBuilding;

    @Column(nullable = false, length = 50)
    private String roomNumber;

    @Column(nullable = false)
    private int floor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private RoomType roomType;

    @Column(nullable = false)
    private BigDecimal pricePerSemester;

    @Column(nullable = false)
    private int capacity;

    @Column(nullable = false)
    private int occupied;

    private String description;

    public enum RoomType { SINGLE_SUITE, DOUBLE_ROOM }

    public boolean hasSpace() {
        return occupied < capacity;
    }
}
