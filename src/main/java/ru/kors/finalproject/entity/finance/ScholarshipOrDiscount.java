package ru.kors.finalproject.entity.finance;

import jakarta.persistence.*;
import lombok.*;
import ru.kors.finalproject.entity.user.StudentProfile;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "scholarships_discounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScholarshipOrDiscount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_profile_id", nullable = false)
    private StudentProfile studentProfile;

    @Enumerated(EnumType.STRING)
    private Type type;

    private String description;
    private BigDecimal amountOrPercent;
    private LocalDate validFrom;
    private LocalDate validTo;

    public enum Type { SCHOLARSHIP, DISCOUNT }
}
