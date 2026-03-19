package ru.kors.finalproject.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "attendance_sessions", uniqueConstraints = @UniqueConstraint(columnNames = {"subject_offering_id", "class_date"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_offering_id", nullable = false)
    private SubjectOffering subjectOffering;

    @Column(name = "class_date", nullable = false)
    private LocalDate classDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_teacher_id")
    private Teacher createdBy;

    @Column(nullable = false)
    private boolean locked;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SessionStatus status = SessionStatus.CLOSED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CheckInMode checkInMode = CheckInMode.ONE_CLICK;

    @Column(nullable = false)
    @Builder.Default
    private boolean allowTeacherOverride = true;

    private String checkInCode;
    private Instant attendanceCloseAt;
    private Instant openedAt;
    private Instant closedAt;

    @Column(nullable = false)
    private Instant createdAt;

    public enum SessionStatus { DRAFT, OPEN, CLOSED }

    public enum CheckInMode { ONE_CLICK, CODE }
}
