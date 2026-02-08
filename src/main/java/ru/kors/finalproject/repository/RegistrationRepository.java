package ru.kors.finalproject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.kors.finalproject.entity.Registration;

import java.util.List;
import java.util.Optional;

public interface RegistrationRepository extends JpaRepository<Registration, Long> {
    List<Registration> findByStudentId(Long studentId);

    @Query("SELECT r FROM Registration r LEFT JOIN FETCH r.subjectOffering so LEFT JOIN FETCH so.subject WHERE r.student.id = :studentId")
    List<Registration> findByStudentIdWithDetails(Long studentId);

    Optional<Registration> findByStudentIdAndSubjectOfferingId(Long studentId, Long subjectOfferingId);

    boolean existsByStudentIdAndSubjectOfferingId(Long studentId, Long subjectOfferingId);

    long countBySubjectOfferingIdAndStatusIn(Long subjectOfferingId, List<Registration.RegistrationStatus> statuses);
}
