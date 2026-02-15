package ru.kors.finalproject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.kors.finalproject.entity.FxRegistration;

import java.util.List;

public interface FxRegistrationRepository extends JpaRepository<FxRegistration, Long> {
    List<FxRegistration> findByStudentIdOrderByCreatedAtDesc(Long studentId);

    @Query("SELECT fx FROM FxRegistration fx LEFT JOIN FETCH fx.student LEFT JOIN FETCH fx.subjectOffering so LEFT JOIN FETCH so.subject WHERE fx.student.id = :studentId ORDER BY fx.createdAt DESC")
    List<FxRegistration> findByStudentIdWithDetailsOrderByCreatedAtDesc(Long studentId);
}
