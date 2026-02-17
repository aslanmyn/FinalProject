package ru.kors.finalproject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.kors.finalproject.entity.Grade;

import java.util.List;

public interface GradeRepository extends JpaRepository<Grade, Long> {
    List<Grade> findByStudentIdAndSubjectOffering_SemesterId(Long studentId, Long semesterId);

    @Query("SELECT g FROM Grade g LEFT JOIN FETCH g.student LEFT JOIN FETCH g.component LEFT JOIN FETCH g.subjectOffering LEFT JOIN FETCH g.subjectOffering.subject WHERE g.student.id = :studentId AND g.subjectOffering.semester.id = :semesterId")
    List<Grade> findByStudentIdAndSemesterIdWithDetails(Long studentId, Long semesterId);

    List<Grade> findByStudentId(Long studentId);

    List<Grade> findBySubjectOfferingId(Long subjectOfferingId);

    @Query("SELECT g FROM Grade g LEFT JOIN FETCH g.student LEFT JOIN FETCH g.component LEFT JOIN FETCH g.subjectOffering WHERE g.subjectOffering.id = :offeringId")
    List<Grade> findBySubjectOfferingIdWithDetails(Long offeringId);

    List<Grade> findByStudentIdAndPublishedTrue(Long studentId);

    @Query("SELECT g FROM Grade g LEFT JOIN FETCH g.subjectOffering so LEFT JOIN FETCH so.subject LEFT JOIN FETCH g.component WHERE g.student.id = :studentId AND g.published = true ORDER BY so.id, g.component.id")
    List<Grade> findByStudentIdAndPublishedTrueWithDetails(Long studentId);
}
