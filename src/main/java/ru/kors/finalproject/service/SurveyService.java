package ru.kors.finalproject.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.kors.finalproject.entity.*;
import ru.kors.finalproject.repository.SemesterRepository;
import ru.kors.finalproject.repository.SurveyQuestionRepository;
import ru.kors.finalproject.repository.SurveyRepository;
import ru.kors.finalproject.repository.SurveyResponseRepository;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SurveyService {

    private final SurveyRepository surveyRepository;
    private final SurveyQuestionRepository surveyQuestionRepository;
    private final SurveyResponseRepository surveyResponseRepository;
    private final SemesterRepository semesterRepository;
    private final AuditService auditService;

    public List<Survey> listAll() {
        return surveyRepository.findAllWithSemesterOrderByStartDateDesc();
    }

    public List<Survey> listActive() {
        LocalDate today = LocalDate.now();
        return surveyRepository.findByStartDateLessThanEqualAndEndDateGreaterThanEqual(today, today);
    }

    @Transactional
    public Survey create(String title, LocalDate startDate, LocalDate endDate,
                          boolean anonymous, Long semesterId, List<QuestionInput> questions, User actor) {
        Semester semester = semesterId != null
                ? semesterRepository.findById(semesterId).orElse(null)
                : semesterRepository.findByCurrentTrue().orElse(null);

        Survey survey = Survey.builder()
                .title(title)
                .startDate(startDate)
                .endDate(endDate)
                .anonymous(anonymous)
                .semester(semester)
                .build();
        Survey saved = surveyRepository.save(survey);

        if (questions != null) {
            for (QuestionInput q : questions) {
                SurveyQuestion question = SurveyQuestion.builder()
                        .survey(saved)
                        .type(q.type())
                        .text(q.text())
                        .build();
                surveyQuestionRepository.save(question);
            }
        }

        auditService.logUserAction(actor, "SURVEY_CREATED", "Survey", saved.getId(), "title=" + title);
        return saved;
    }

    @Transactional
    public Survey closeSurvey(Long surveyId, User actor) {
        Survey survey = surveyRepository.findByIdWithSemester(surveyId)
                .orElseThrow(() -> new IllegalArgumentException("Survey not found"));
        survey.setEndDate(LocalDate.now().minusDays(1));
        Survey saved = surveyRepository.save(survey);
        auditService.logUserAction(actor, "SURVEY_CLOSED", "Survey", saved.getId(), "title=" + survey.getTitle());
        return saved;
    }

    public List<SurveyResponse> exportResponses(Long surveyId) {
        return surveyResponseRepository.findBySurveyId(surveyId);
    }

    public long responseCount(Long surveyId) {
        return surveyResponseRepository.countBySurveyId(surveyId);
    }

    public record QuestionInput(SurveyQuestion.QuestionType type, String text) {
    }
}
