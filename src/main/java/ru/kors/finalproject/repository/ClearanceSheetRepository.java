package ru.kors.finalproject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.kors.finalproject.entity.ClearanceSheet;

import java.util.List;
import java.util.Optional;

public interface ClearanceSheetRepository extends JpaRepository<ClearanceSheet, Long> {
    Optional<ClearanceSheet> findByStudentId(Long studentId);

    @Query("SELECT s FROM ClearanceSheet s LEFT JOIN FETCH s.student LEFT JOIN FETCH s.checkpoints WHERE s.student.id = :studentId")
    Optional<ClearanceSheet> findByStudentIdWithDetails(Long studentId);

    @Query("SELECT DISTINCT s FROM ClearanceSheet s LEFT JOIN FETCH s.student LEFT JOIN FETCH s.checkpoints")
    List<ClearanceSheet> findAllWithStudentAndCheckpoints();
}
