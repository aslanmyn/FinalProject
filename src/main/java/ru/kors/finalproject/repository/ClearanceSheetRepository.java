package ru.kors.finalproject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.kors.finalproject.entity.ClearanceSheet;

import java.util.Optional;

public interface ClearanceSheetRepository extends JpaRepository<ClearanceSheet, Long> {
    Optional<ClearanceSheet> findByStudentId(Long studentId);
}
