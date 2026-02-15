package ru.kors.finalproject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.kors.finalproject.entity.Hold;

import java.util.List;
import java.util.Optional;

public interface HoldRepository extends JpaRepository<Hold, Long> {
    List<Hold> findByStudentIdAndActiveTrue(Long studentId);

    @Query("SELECT h FROM Hold h LEFT JOIN FETCH h.student WHERE h.active = true")
    List<Hold> findActiveWithStudent();

    boolean existsByStudentIdAndTypeAndActiveTrue(Long studentId, Hold.HoldType type);

    Optional<Hold> findByStudentIdAndTypeAndActiveTrue(Long studentId, Hold.HoldType type);
}
