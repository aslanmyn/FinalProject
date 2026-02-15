package ru.kors.finalproject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.kors.finalproject.entity.Charge;

import java.util.List;

public interface ChargeRepository extends JpaRepository<Charge, Long> {
    List<Charge> findByStudentIdOrderByDueDateDesc(Long studentId);

    @Query("SELECT c FROM Charge c LEFT JOIN FETCH c.student WHERE c.student.id = :studentId ORDER BY c.dueDate DESC")
    List<Charge> findByStudentIdWithDetailsOrderByDueDateDesc(Long studentId);

    List<Charge> findByStudentId(Long studentId);
}
