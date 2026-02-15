package ru.kors.finalproject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.kors.finalproject.entity.MobilityApplication;

import java.util.List;

public interface MobilityApplicationRepository extends JpaRepository<MobilityApplication, Long> {
    List<MobilityApplication> findByStudentIdOrderByCreatedAtDesc(Long studentId);

    @Query("SELECT a FROM MobilityApplication a LEFT JOIN FETCH a.student WHERE a.student.id = :studentId ORDER BY a.createdAt DESC")
    List<MobilityApplication> findByStudentIdWithDetailsOrderByCreatedAtDesc(Long studentId);

    @Query("SELECT a FROM MobilityApplication a LEFT JOIN FETCH a.student ORDER BY a.createdAt DESC")
    List<MobilityApplication> findAllWithStudent();
}
