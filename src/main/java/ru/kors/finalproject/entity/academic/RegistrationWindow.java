package ru.kors.finalproject.entity.academic;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Defines when students can add/drop courses.
 * A student cannot register outside an active RegistrationWindow.
 */
@Entity
@Table(name = "registration_windows")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistrationWindow {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "term_id", nullable = false)
    private AcademicTerm term;

    private LocalDate addStart;
    private LocalDate addEnd;
    private LocalDate dropEnd;
}
