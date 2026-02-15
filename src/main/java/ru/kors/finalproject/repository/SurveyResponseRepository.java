package ru.kors.finalproject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.kors.finalproject.entity.SurveyResponse;

import java.util.List;
import java.util.Optional;

public interface SurveyResponseRepository extends JpaRepository<SurveyResponse, Long> {
    Optional<SurveyResponse> findBySurveyIdAndStudentId(Long surveyId, Long studentId);

    List<SurveyResponse> findBySurveyId(Long surveyId);

    long countBySurveyId(Long surveyId);
}
