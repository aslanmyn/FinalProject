package ru.kors.finalproject.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.kors.finalproject.entity.Charge;
import ru.kors.finalproject.entity.Hold;
import ru.kors.finalproject.entity.Payment;
import ru.kors.finalproject.entity.Student;
import ru.kors.finalproject.repository.ChargeRepository;
import ru.kors.finalproject.repository.HoldRepository;
import ru.kors.finalproject.repository.PaymentRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FinancialServiceTest {

    @Mock
    private ChargeRepository chargeRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private HoldRepository holdRepository;
    @Mock
    private AuditService auditService;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private FinancialService financialService;

    private Student student;

    @BeforeEach
    void setUp() {
        student = Student.builder()
                .id(1L)
                .email("student@example.com")
                .name("Jane Doe")
                .course(2)
                .creditsEarned(30)
                .build();
    }

    // -------------------------------------------------------------------------
    // getBalance tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getBalance - returns ZERO when no charges and no payments")
    void getBalance_noChargesNoPayments() {
        when(chargeRepository.findByStudentId(1L)).thenReturn(List.of());
        when(paymentRepository.findByStudentIdOrderByDateDesc(1L)).thenReturn(List.of());

        BigDecimal balance = financialService.getBalance(student);

        assertThat(balance).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("getBalance - returns correct balance with charges and payments")
    void getBalance_withChargesAndPayments() {
        Charge charge1 = Charge.builder().id(1L).student(student).amount(new BigDecimal("500.00")).build();
        Charge charge2 = Charge.builder().id(2L).student(student).amount(new BigDecimal("300.00")).build();
        when(chargeRepository.findByStudentId(1L)).thenReturn(List.of(charge1, charge2));

        Payment payment1 = Payment.builder().id(1L).student(student).amount(new BigDecimal("200.00")).build();
        when(paymentRepository.findByStudentIdOrderByDateDesc(1L)).thenReturn(List.of(payment1));

        BigDecimal balance = financialService.getBalance(student);

        // 500 + 300 - 200 = 600
        assertThat(balance).isEqualByComparingTo(new BigDecimal("600.00"));
    }

    // -------------------------------------------------------------------------
    // createInvoice tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("createInvoice - saves charge and returns it")
    void createInvoice_savesAndReturns() {
        BigDecimal amount = new BigDecimal("1000.00");
        LocalDate dueDate = LocalDate.now().plusDays(30);

        // Stub for refreshFinancialHold called inside createInvoice
        when(chargeRepository.findByStudentId(1L)).thenReturn(List.of());
        when(paymentRepository.sumByStudentGroupedByCharge(1L)).thenReturn(List.of());
        when(holdRepository.findByStudentIdAndTypeAndActiveTrue(1L, Hold.HoldType.FINANCIAL)).thenReturn(Optional.empty());

        when(chargeRepository.save(any(Charge.class))).thenAnswer(inv -> {
            Charge c = inv.getArgument(0);
            c.setId(10L);
            return c;
        });

        Charge result = financialService.createInvoice(student, amount, "Tuition fee", dueDate);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getAmount()).isEqualByComparingTo(amount);
        assertThat(result.getStatus()).isEqualTo(Charge.ChargeStatus.PENDING);

        verify(chargeRepository).save(any(Charge.class));
        verify(auditService).logStudentAction(eq(student), eq("INVOICE_CREATED"), eq("Charge"), eq(10L), any());
        verify(notificationService).notifyStudent(eq("student@example.com"), any(), any(), any(), any());
    }

    // -------------------------------------------------------------------------
    // registerPayment tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("registerPayment - saves payment and refreshes charge status")
    void registerPayment_success() {
        Charge charge = Charge.builder()
                .id(5L)
                .student(student)
                .amount(new BigDecimal("500.00"))
                .dueDate(LocalDate.now().plusDays(10))
                .status(Charge.ChargeStatus.PENDING)
                .build();

        when(chargeRepository.findById(5L)).thenReturn(Optional.of(charge));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId(20L);
            return p;
        });

        // Stubs for refreshChargeStatus (getOutstandingAmount)
        when(paymentRepository.sumByChargeId(5L)).thenReturn(new BigDecimal("500.00"));

        // Stubs for refreshFinancialHold
        when(chargeRepository.findByStudentId(1L)).thenReturn(List.of(charge));
        when(paymentRepository.sumByStudentGroupedByCharge(1L))
                .thenReturn(Collections.singletonList(new Object[]{5L, new BigDecimal("500.00")}));
        when(holdRepository.findByStudentIdAndTypeAndActiveTrue(1L, Hold.HoldType.FINANCIAL)).thenReturn(Optional.empty());

        LocalDate paymentDate = LocalDate.now();
        Payment result = financialService.registerPayment(student, 5L, new BigDecimal("500.00"), paymentDate);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(20L);
        assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("500.00"));

        verify(paymentRepository).save(any(Payment.class));
        verify(auditService).logStudentAction(eq(student), eq("PAYMENT_REGISTERED"), eq("Payment"), eq(20L), any());
        verify(notificationService).notifyStudent(eq("student@example.com"), any(), any(), any(), any());
    }

    // -------------------------------------------------------------------------
    // hasOverdueInvoices tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("hasOverdueInvoices - returns true when charge is overdue and not fully paid")
    void hasOverdueInvoices_withOverdue() {
        Charge overdueCharge = Charge.builder()
                .id(1L)
                .student(student)
                .amount(new BigDecimal("1000.00"))
                .dueDate(LocalDate.now().minusDays(5))
                .status(Charge.ChargeStatus.OVERDUE)
                .build();

        when(chargeRepository.findByStudentId(1L)).thenReturn(List.of(overdueCharge));
        // Only 200 paid out of 1000
        when(paymentRepository.sumByStudentGroupedByCharge(1L))
                .thenReturn(Collections.singletonList(new Object[]{1L, new BigDecimal("200.00")}));

        boolean result = financialService.hasOverdueInvoices(student);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("hasOverdueInvoices - returns false when all charges are fully paid")
    void hasOverdueInvoices_allPaid() {
        Charge paidCharge = Charge.builder()
                .id(1L)
                .student(student)
                .amount(new BigDecimal("500.00"))
                .dueDate(LocalDate.now().minusDays(10))
                .status(Charge.ChargeStatus.PAID)
                .build();

        when(chargeRepository.findByStudentId(1L)).thenReturn(List.of(paidCharge));
        when(paymentRepository.sumByStudentGroupedByCharge(1L))
                .thenReturn(Collections.singletonList(new Object[]{1L, new BigDecimal("500.00")}));

        boolean result = financialService.hasOverdueInvoices(student);

        assertThat(result).isFalse();
    }

    // -------------------------------------------------------------------------
    // refreshFinancialHold tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("refreshFinancialHold - creates FINANCIAL hold when student has overdue invoices")
    void refreshFinancialHold_createsHold_whenOverdue() {
        Charge overdueCharge = Charge.builder()
                .id(1L)
                .student(student)
                .amount(new BigDecimal("1000.00"))
                .dueDate(LocalDate.now().minusDays(5))
                .status(Charge.ChargeStatus.OVERDUE)
                .build();

        // hasOverdueInvoices returns true
        when(chargeRepository.findByStudentId(1L)).thenReturn(List.of(overdueCharge));
        when(paymentRepository.sumByStudentGroupedByCharge(1L)).thenReturn(List.of());

        // no existing financial hold
        when(holdRepository.findByStudentIdAndTypeAndActiveTrue(1L, Hold.HoldType.FINANCIAL)).thenReturn(Optional.empty());

        when(holdRepository.save(any(Hold.class))).thenAnswer(inv -> {
            Hold h = inv.getArgument(0);
            h.setId(99L);
            return h;
        });

        financialService.refreshFinancialHold(student);

        ArgumentCaptor<Hold> holdCaptor = ArgumentCaptor.forClass(Hold.class);
        verify(holdRepository).save(holdCaptor.capture());

        Hold savedHold = holdCaptor.getValue();
        assertThat(savedHold.getType()).isEqualTo(Hold.HoldType.FINANCIAL);
        assertThat(savedHold.isActive()).isTrue();
        assertThat(savedHold.getReason()).isEqualTo("Overdue invoice");
        assertThat(savedHold.getStudent()).isEqualTo(student);

        verify(auditService).logStudentAction(eq(student), eq("HOLD_CREATED"), eq("Hold"), any(), any());
        verify(notificationService).notifyStudent(eq("student@example.com"), any(), eq("Financial hold activated"), any(), any());
    }

    @Test
    @DisplayName("refreshFinancialHold - removes hold when all invoices are paid")
    void refreshFinancialHold_removesHold_whenPaid() {
        Charge paidCharge = Charge.builder()
                .id(1L)
                .student(student)
                .amount(new BigDecimal("500.00"))
                .dueDate(LocalDate.now().minusDays(10))
                .status(Charge.ChargeStatus.PAID)
                .build();

        // hasOverdueInvoices returns false (fully paid)
        when(chargeRepository.findByStudentId(1L)).thenReturn(List.of(paidCharge));
        when(paymentRepository.sumByStudentGroupedByCharge(1L))
                .thenReturn(Collections.singletonList(new Object[]{1L, new BigDecimal("500.00")}));

        // existing financial hold present
        Hold existingHold = Hold.builder()
                .id(99L)
                .student(student)
                .type(Hold.HoldType.FINANCIAL)
                .active(true)
                .reason("Overdue invoice")
                .createdAt(Instant.now().minusSeconds(86400))
                .build();
        when(holdRepository.findByStudentIdAndTypeAndActiveTrue(1L, Hold.HoldType.FINANCIAL))
                .thenReturn(Optional.of(existingHold));

        when(holdRepository.save(any(Hold.class))).thenAnswer(inv -> inv.getArgument(0));

        financialService.refreshFinancialHold(student);

        ArgumentCaptor<Hold> holdCaptor = ArgumentCaptor.forClass(Hold.class);
        verify(holdRepository).save(holdCaptor.capture());

        Hold savedHold = holdCaptor.getValue();
        assertThat(savedHold.isActive()).isFalse();
        assertThat(savedHold.getResolvedAt()).isNotNull();

        verify(auditService).logStudentAction(eq(student), eq("HOLD_RESOLVED"), eq("Hold"), eq(99L), any());
        verify(notificationService).notifyStudent(eq("student@example.com"), any(), eq("Financial hold removed"), any(), any());
    }
}
