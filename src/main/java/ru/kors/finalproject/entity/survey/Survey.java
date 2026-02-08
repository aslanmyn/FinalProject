package ru.kors.finalproject.entity.survey;

import jakarta.persistence.*;
import lombok.*;
import ru.kors.finalproject.entity.academic.AcademicTerm;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "surveys")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Survey {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean anonymous;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "term_id")
    private AcademicTerm term;

    @OneToMany(mappedBy = "survey", cascade = CascadeType.ALL)
    @Builder.Default
    private List<SurveyQuestion> questions = new ArrayList<>();
}
