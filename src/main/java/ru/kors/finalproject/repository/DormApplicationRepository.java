package ru.kors.finalproject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.kors.finalproject.entity.DormApplication;

import java.util.List;
import java.util.Optional;

public interface DormApplicationRepository extends JpaRepository<DormApplication, Long> {

    @Query("SELECT a FROM DormApplication a LEFT JOIN FETCH a.dormRoom WHERE a.student.id = :studentId ORDER BY a.createdAt DESC")
    List<DormApplication> findByStudentId(Long studentId);

    @Query("SELECT a FROM DormApplication a LEFT JOIN FETCH a.dormRoom WHERE a.id = :id AND a.student.id = :studentId")
    Optional<DormApplication> findByIdAndStudentId(Long id, Long studentId);

    boolean existsByStudentIdAndStatusIn(Long studentId, List<DormApplication.ApplicationStatus> statuses);
}
