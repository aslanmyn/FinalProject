package ru.kors.finalproject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.kors.finalproject.entity.SurveyQuestion;

import java.util.List;

public interface SurveyQuestionRepository extends JpaRepository<SurveyQuestion, Long> {

    List<SurveyQuestion> findBySurveyIdOrderByIdAsc(Long surveyId);
}
