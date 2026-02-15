package ru.kors.finalproject.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "holds")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Hold {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HoldType type;

    @Column(nullable = false)
    private boolean active;

    @Column(length = 500)
    private String reason;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant resolvedAt;

    public enum HoldType {
        FINANCIAL,
        ACADEMIC,
        DISCIPLINARY,
        MANUAL
    }
}
