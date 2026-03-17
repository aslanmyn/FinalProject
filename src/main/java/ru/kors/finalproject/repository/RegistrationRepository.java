package ru.kors.finalproject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.kors.finalproject.entity.Registration;

import java.util.List;
import java.util.Optional;

public interface RegistrationRepository extends JpaRepository<Registration, Long> {
    List<Registration> findByStudentId(Long studentId);

    @Query("SELECT DISTINCT r FROM Registration r LEFT JOIN FETCH r.subjectOffering so LEFT JOIN FETCH so.subject LEFT JOIN FETCH so.teacher LEFT JOIN FETCH so.semester LEFT JOIN FETCH so.meetingTimes WHERE r.student.id = :studentId")
    List<Registration> findByStudentIdWithDetails(Long studentId);

    @Query("SELECT DISTINCT r FROM Registration r LEFT JOIN FETCH r.subjectOffering so LEFT JOIN FETCH so.subject LEFT JOIN FETCH so.teacher LEFT JOIN FETCH so.semester LEFT JOIN FETCH so.meetingTimes WHERE r.student.id = :studentId AND r.status <> 'DROPPED' AND so.semester.current = true")
    List<Registration> findActiveByStudentIdWithDetails(Long studentId);

    Optional<Registration> findByStudentIdAndSubjectOfferingId(Long studentId, Long subjectOfferingId);

    @Query("SELECT r FROM Registration r " +
           "LEFT JOIN FETCH r.subjectOffering so " +
           "LEFT JOIN FETCH so.subject " +
           "LEFT JOIN FETCH so.teacher " +
           "LEFT JOIN FETCH so.semester " +
           "LEFT JOIN FETCH so.meetingTimes " +
           "WHERE r.student.id = :studentId AND so.id = :subjectOfferingId")
    Optional<Registration> findByStudentIdAndSubjectOfferingIdWithDetails(Long studentId, Long subjectOfferingId);

    boolean existsByStudentIdAndSubjectOfferingId(Long studentId, Long subjectOfferingId);

    long countBySubjectOfferingIdAndStatusIn(Long subjectOfferingId, List<Registration.RegistrationStatus> statuses);

    List<Registration> findBySubjectOfferingIdAndStatusIn(Long subjectOfferingId, List<Registration.RegistrationStatus> statuses);

    @Query("SELECT r FROM Registration r LEFT JOIN FETCH r.student LEFT JOIN FETCH r.subjectOffering so LEFT JOIN FETCH so.subject WHERE r.subjectOffering.id = :offeringId AND r.status IN :statuses")
    List<Registration> findBySubjectOfferingIdAndStatusInWithDetails(Long offeringId, List<Registration.RegistrationStatus> statuses);
}
