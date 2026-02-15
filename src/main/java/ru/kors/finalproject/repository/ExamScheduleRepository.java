package ru.kors.finalproject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.kors.finalproject.entity.ExamSchedule;

import java.util.List;

public interface ExamScheduleRepository extends JpaRepository<ExamSchedule, Long> {
    List<ExamSchedule> findBySubjectOffering_SemesterIdOrderByExamDateAsc(Long semesterId);

    @Query("SELECT e FROM ExamSchedule e LEFT JOIN FETCH e.subjectOffering so LEFT JOIN FETCH so.subject WHERE so.semester.id = :semesterId ORDER BY e.examDate ASC")
    List<ExamSchedule> findBySemesterIdWithDetails(Long semesterId);
}
