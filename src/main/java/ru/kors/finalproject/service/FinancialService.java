package ru.kors.finalproject.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.kors.finalproject.entity.Charge;
import ru.kors.finalproject.entity.Payment;
import ru.kors.finalproject.entity.Student;
import ru.kors.finalproject.repository.ChargeRepository;
import ru.kors.finalproject.repository.PaymentRepository;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FinancialService {
    private final ChargeRepository chargeRepository;
    private final PaymentRepository paymentRepository;

    public List<Charge> getCharges(Student student) {
        return chargeRepository.findByStudentIdOrderByDueDateDesc(student.getId());
    }

    public List<Payment> getPayments(Student student) {
        return paymentRepository.findByStudentIdOrderByDateDesc(student.getId());
    }

    public BigDecimal getBalance(Student student) {
        BigDecimal totalCharges = chargeRepository.findByStudentIdOrderByDueDateDesc(student.getId()).stream()
                .map(Charge::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPayments = paymentRepository.findByStudentIdOrderByDateDesc(student.getId()).stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return totalCharges.subtract(totalPayments);
    }

    public boolean hasDebt(Student student) {
        return getBalance(student).compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean hasRegistrationLock(Student student) {
        return hasDebt(student);
    }
}
