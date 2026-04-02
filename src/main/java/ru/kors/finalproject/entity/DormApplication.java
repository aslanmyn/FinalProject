package ru.kors.finalproject.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "dorm_applications")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DormApplication {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dorm_room_id")
    private DormRoom dormRoom;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private ApplicationStatus status = ApplicationStatus.DRAFT;

    @Column(nullable = false)
    @Builder.Default
    private int currentStep = 1;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private DormRoom.RoomType roomTypePreference;

    private String sleepSchedule;
    private String studyEnvironment;
    private String preferredRoommateUid;

    @Column(nullable = false)
    @Builder.Default
    private boolean termsAccepted = false;

    private String emergencyContactName;
    private String emergencyContactPhone;

    @Column(columnDefinition = "text")
    private String specialNeeds;

    @Column(nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    public enum ApplicationStatus { DRAFT, SUBMITTED, APPROVED, REJECTED, CANCELLED }
}
