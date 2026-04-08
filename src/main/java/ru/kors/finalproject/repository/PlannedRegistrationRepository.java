package ru.kors.finalproject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import ru.kors.finalproject.entity.PlannedRegistration;

import java.util.List;
import java.util.Optional;

public interface PlannedRegistrationRepository extends JpaRepository<PlannedRegistration, Long> {
    @Query("""
            SELECT DISTINCT pr
            FROM PlannedRegistration pr
            LEFT JOIN FETCH pr.semester sem
            LEFT JOIN FETCH pr.student st
            LEFT JOIN FETCH pr.subjectOffering so
            LEFT JOIN FETCH so.subject
            LEFT JOIN FETCH so.teacher
            LEFT JOIN FETCH so.meetingTimes
            WHERE st.id = :studentId
              AND sem.id = :semesterId
            ORDER BY pr.createdAt ASC
            """)
    List<PlannedRegistration> findByStudentIdAndSemesterIdWithDetails(Long studentId, Long semesterId);

    long countByStudentIdAndSemesterId(Long studentId, Long semesterId);

    long countBySubjectOfferingId(Long subjectOfferingId);

    boolean existsByStudentIdAndSubjectOfferingId(Long studentId, Long subjectOfferingId);

    @Query("""
            SELECT pr
            FROM PlannedRegistration pr
            LEFT JOIN FETCH pr.semester sem
            LEFT JOIN FETCH pr.student st
            LEFT JOIN FETCH pr.subjectOffering so
            LEFT JOIN FETCH so.subject
            LEFT JOIN FETCH so.teacher
            WHERE st.id = :studentId
              AND so.id = :subjectOfferingId
            """)
    Optional<PlannedRegistration> findByStudentIdAndSubjectOfferingId(Long studentId, Long subjectOfferingId);

    @Modifying
    @Query("""
            DELETE FROM PlannedRegistration pr
            WHERE pr.student.id = :studentId
              AND pr.subjectOffering.id = :subjectOfferingId
            """)
    int deleteByStudentIdAndSubjectOfferingId(Long studentId, Long subjectOfferingId);
}
