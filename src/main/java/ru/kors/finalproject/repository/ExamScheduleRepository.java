package ru.kors.finalproject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.kors.finalproject.entity.ExamSchedule;

import java.util.List;
import java.util.Optional;

public interface ExamScheduleRepository extends JpaRepository<ExamSchedule, Long> {
    List<ExamSchedule> findBySubjectOffering_SemesterIdOrderByExamDateAsc(Long semesterId);

    @Query("SELECT e FROM ExamSchedule e LEFT JOIN FETCH e.subjectOffering so LEFT JOIN FETCH so.subject WHERE e.id = :id")
    Optional<ExamSchedule> findByIdWithDetails(Long id);

    @Query("SELECT e FROM ExamSchedule e LEFT JOIN FETCH e.subjectOffering so LEFT JOIN FETCH so.subject WHERE so.semester.id = :semesterId ORDER BY e.examDate ASC")
    List<ExamSchedule> findBySemesterIdWithDetails(Long semesterId);

    @Query("SELECT e FROM ExamSchedule e LEFT JOIN FETCH e.subjectOffering so LEFT JOIN FETCH so.subject WHERE so.id IN :subjectOfferingIds ORDER BY e.examDate ASC")
    List<ExamSchedule> findBySubjectOfferingIdInWithDetails(List<Long> subjectOfferingIds);
}
