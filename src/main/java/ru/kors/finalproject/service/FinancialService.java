package ru.kors.finalproject.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.kors.finalproject.entity.Charge;
import ru.kors.finalproject.entity.Hold;
import ru.kors.finalproject.entity.Payment;
import ru.kors.finalproject.entity.Student;
import ru.kors.finalproject.entity.Notification;
import ru.kors.finalproject.repository.ChargeRepository;
import ru.kors.finalproject.repository.HoldRepository;
import ru.kors.finalproject.repository.PaymentRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FinancialService {
    private final ChargeRepository chargeRepository;
    private final PaymentRepository paymentRepository;
    private final HoldRepository holdRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;

    public List<Charge> getCharges(Student student) {
        var charges = chargeRepository.findByStudentIdOrderByDueDateDesc(student.getId());
        charges.forEach(this::refreshChargeStatus);
        return charges;
    }

    public List<Payment> getPayments(Student student) {
        return paymentRepository.findByStudentIdOrderByDateDesc(student.getId());
    }

    public BigDecimal getBalance(Student student) {
        BigDecimal totalCharges = chargeRepository.findByStudentId(student.getId()).stream()
                .map(Charge::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPayments = paymentRepository.findByStudentIdOrderByDateDesc(student.getId()).stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return totalCharges.subtract(totalPayments);
    }

    public BigDecimal getOutstandingAmount(Charge charge) {
        BigDecimal paid = paymentRepository.sumByChargeId(charge.getId());
        return charge.getAmount().subtract(paid);
    }

    @Transactional
    public Charge createInvoice(Student student, BigDecimal amount, String description, LocalDate dueDate) {
        Charge charge = Charge.builder()
                .student(student)
                .amount(amount)
                .description(description)
                .dueDate(dueDate)
                .status(Charge.ChargeStatus.PENDING)
                .build();
        Charge saved = chargeRepository.save(charge);
        refreshFinancialHold(student);
        auditService.logStudentAction(student, "INVOICE_CREATED", "Charge", saved.getId(), "amount=" + amount + ", dueDate=" + dueDate);
        notificationService.notifyStudent(student.getEmail(), Notification.NotificationType.FINANCE, "New invoice issued",
                description + " | Amount: " + amount, "/app/student/financial");
        return saved;
    }

    @Transactional
    public Payment registerPayment(Student student, Long chargeId, BigDecimal amount, LocalDate paymentDate) {
        Charge charge = chargeRepository.findById(chargeId)
                .orElseThrow(() -> new IllegalArgumentException("Charge not found"));
        Payment payment = Payment.builder()
                .student(student)
                .charge(charge)
                .amount(amount)
                .date(paymentDate != null ? paymentDate : LocalDate.now())
                .build();
        Payment saved = paymentRepository.save(payment);
        refreshChargeStatus(charge);
        refreshFinancialHold(student);
        auditService.logStudentAction(student, "PAYMENT_REGISTERED", "Payment", saved.getId(), "amount=" + amount + ", chargeId=" + chargeId);
        notificationService.notifyStudent(student.getEmail(), Notification.NotificationType.FINANCE, "Payment recorded",
                "Payment of " + amount + " was recorded", "/app/student/financial");
        return saved;
    }

    public boolean hasOverdueInvoices(Student student) {
        // Load payment sums in one query to avoid N+1
        var paymentSums = buildPaymentSumMap(student.getId());
        return chargeRepository.findByStudentId(student.getId()).stream()
                .anyMatch(c -> c.getDueDate() != null
                        && c.getDueDate().isBefore(LocalDate.now())
                        && c.getAmount().subtract(paymentSums.getOrDefault(c.getId(), BigDecimal.ZERO))
                                .compareTo(BigDecimal.ZERO) > 0);
    }

    public boolean hasDebt(Student student) {
        // Load payment sums in one query to avoid N+1
        var paymentSums = buildPaymentSumMap(student.getId());
        return chargeRepository.findByStudentId(student.getId()).stream()
                .anyMatch(c -> c.getAmount().subtract(paymentSums.getOrDefault(c.getId(), BigDecimal.ZERO))
                        .compareTo(BigDecimal.ZERO) > 0);
    }

    /**
     * Returns a map of chargeId → total paid amount for all charges of a student.
     * Single query instead of one per charge (fixes N+1).
     */
    private java.util.Map<Long, BigDecimal> buildPaymentSumMap(Long studentId) {
        java.util.Map<Long, BigDecimal> map = new java.util.HashMap<>();
        for (Object[] row : paymentRepository.sumByStudentGroupedByCharge(studentId)) {
            map.put((Long) row[0], (BigDecimal) row[1]);
        }
        return map;
    }

    public boolean hasRegistrationLock(Student student) {
        refreshFinancialHold(student);
        return holdRepository.existsByStudentIdAndTypeAndActiveTrue(student.getId(), Hold.HoldType.FINANCIAL);
    }

    public void refreshFinancialHold(Student student) {
        boolean overdue = hasOverdueInvoices(student);
        var existing = holdRepository.findByStudentIdAndTypeAndActiveTrue(student.getId(), Hold.HoldType.FINANCIAL);

        if (overdue && existing.isEmpty()) {
            Hold hold = Hold.builder()
                    .student(student)
                    .type(Hold.HoldType.FINANCIAL)
                    .active(true)
                    .reason("Overdue invoice")
                    .createdAt(java.time.Instant.now())
                    .build();
            holdRepository.save(hold);
            auditService.logStudentAction(student, "HOLD_CREATED", "Hold", hold.getId(), "type=FINANCIAL");
            notificationService.notifyStudent(student.getEmail(), Notification.NotificationType.FINANCE, "Financial hold activated",
                    "You have overdue invoices. Registration and FX are blocked until payment.", "/app/student/financial");
        }

        if (!overdue && existing.isPresent()) {
            Hold hold = existing.get();
            hold.setActive(false);
            hold.setResolvedAt(java.time.Instant.now());
            holdRepository.save(hold);
            auditService.logStudentAction(student, "HOLD_RESOLVED", "Hold", hold.getId(), "type=FINANCIAL");
            notificationService.notifyStudent(student.getEmail(), Notification.NotificationType.FINANCE, "Financial hold removed",
                    "All overdue invoices are cleared.", "/app/student/financial");
        }
    }

    private void refreshChargeStatus(Charge charge) {
        BigDecimal outstanding = getOutstandingAmount(charge);
        Charge.ChargeStatus newStatus;
        if (outstanding.compareTo(BigDecimal.ZERO) <= 0) {
            newStatus = Charge.ChargeStatus.PAID;
        } else if (charge.getDueDate() != null && charge.getDueDate().isBefore(LocalDate.now())) {
            newStatus = Charge.ChargeStatus.OVERDUE;
        } else if (outstanding.compareTo(charge.getAmount()) < 0) {
            newStatus = Charge.ChargeStatus.PARTIAL;
        } else {
            newStatus = Charge.ChargeStatus.PENDING;
        }
        if (charge.getStatus() != newStatus) {
            charge.setStatus(newStatus);
            chargeRepository.save(charge);
        }
    }
}
