package ru.kors.finalproject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.kors.finalproject.entity.Survey;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SurveyRepository extends JpaRepository<Survey, Long> {
    @Query("SELECT s FROM Survey s LEFT JOIN FETCH s.semester ORDER BY s.startDate DESC")
    List<Survey> findAllWithSemesterOrderByStartDateDesc();

    @Query("SELECT s FROM Survey s LEFT JOIN FETCH s.semester WHERE s.id = :id")
    Optional<Survey> findByIdWithSemester(Long id);

    List<Survey> findByStartDateLessThanEqualAndEndDateGreaterThanEqual(LocalDate end, LocalDate start);
}
