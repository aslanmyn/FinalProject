package ru.kors.finalproject.entity.survey;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "survey_options")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SurveyOption {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private SurveyQuestion question;

    private String optionText;
    private int sortOrder;
}
