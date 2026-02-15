package ru.kors.finalproject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.kors.finalproject.entity.Hold;

import java.util.List;
import java.util.Optional;

public interface HoldRepository extends JpaRepository<Hold, Long> {
    List<Hold> findByStudentIdAndActiveTrue(Long studentId);

    boolean existsByStudentIdAndTypeAndActiveTrue(Long studentId, Hold.HoldType type);

    Optional<Hold> findByStudentIdAndTypeAndActiveTrue(Long studentId, Hold.HoldType type);
}
