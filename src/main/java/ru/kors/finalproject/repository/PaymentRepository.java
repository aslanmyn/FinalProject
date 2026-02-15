package ru.kors.finalproject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.kors.finalproject.entity.Payment;

import java.math.BigDecimal;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByStudentIdOrderByDateDesc(Long studentId);

    List<Payment> findByChargeId(Long chargeId);

    /** Sum of all payments for a given charge (returns 0 if none). */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.charge.id = :chargeId")
    BigDecimal sumByChargeId(Long chargeId);

    /**
     * Returns [chargeId, sumAmount] rows for all charges of a student in one query.
     * Eliminates the N+1 problem where each charge needed a separate payment query.
     */
    @Query("SELECT p.charge.id, COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.student.id = :studentId GROUP BY p.charge.id")
    List<Object[]> sumByStudentGroupedByCharge(Long studentId);
}
