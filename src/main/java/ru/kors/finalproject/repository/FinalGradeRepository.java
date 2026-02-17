package ru.kors.finalproject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.kors.finalproject.entity.FinalGrade;

import java.util.List;
import java.util.Optional;

public interface FinalGradeRepository extends JpaRepository<FinalGrade, Long> {
    List<FinalGrade> findByStudentId(Long studentId);

    List<FinalGrade> findByStudentIdAndPublishedTrue(Long studentId);

    @Query("SELECT f FROM FinalGrade f LEFT JOIN FETCH f.student LEFT JOIN FETCH f.subjectOffering so LEFT JOIN FETCH so.subject LEFT JOIN FETCH so.semester WHERE f.student.id = :studentId AND f.published = true ORDER BY so.semester.startDate DESC, so.id")
    List<FinalGrade> findByStudentIdAndPublishedTrueWithDetails(Long studentId);

    List<FinalGrade> findBySubjectOfferingId(Long subjectOfferingId);

    @Query("SELECT f FROM FinalGrade f LEFT JOIN FETCH f.student LEFT JOIN FETCH f.subjectOffering WHERE f.subjectOffering.id = :offeringId")
    List<FinalGrade> findBySubjectOfferingIdWithDetails(Long offeringId);

    Optional<FinalGrade> findByStudentIdAndSubjectOfferingId(Long studentId, Long subjectOfferingId);
}
