package ru.kors.finalproject.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "charges")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Charge {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    private BigDecimal amount;
    private String description;
    private LocalDate dueDate;
    @Enumerated(EnumType.STRING)
    private ChargeStatus status;

    @OneToMany(mappedBy = "charge", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Payment> payments = new ArrayList<>();

    public enum ChargeStatus { PENDING, PAID, PARTIAL, OVERDUE }
}
