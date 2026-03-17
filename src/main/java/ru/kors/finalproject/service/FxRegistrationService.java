package ru.kors.finalproject.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.kors.finalproject.entity.Charge;
import ru.kors.finalproject.entity.FinalGrade;
import ru.kors.finalproject.entity.FxRegistration;
import ru.kors.finalproject.entity.Notification;
import ru.kors.finalproject.entity.Student;
import ru.kors.finalproject.entity.SubjectOffering;
import ru.kors.finalproject.entity.User;
import ru.kors.finalproject.repository.ChargeRepository;
import ru.kors.finalproject.repository.FinalGradeRepository;
import ru.kors.finalproject.repository.FxRegistrationRepository;
import ru.kors.finalproject.repository.SubjectOfferingRepository;
import ru.kors.finalproject.repository.UserRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class FxRegistrationService {
    private static final BigDecimal DEFAULT_FX_FEE = BigDecimal.valueOf(15000);

    private final FxRegistrationRepository fxRegistrationRepository;
    private final FinalGradeRepository finalGradeRepository;
    private final SubjectOfferingRepository subjectOfferingRepository;
    private final ChargeRepository chargeRepository;
    private final UserRepository userRepository;
    private final WindowPolicyService windowPolicyService;
    private final AddDropService addDropService;
    private final FinancialService financialService;
    private final NotificationService notificationService;
    private final AuditService auditService;

    public List<FxRegistration> listForStudent(Student student) {
        return fxRegistrationRepository.findByStudentIdWithDetailsOrderByCreatedAtDesc(student.getId());
    }

    public List<FxRegistration> listAll() {
        return fxRegistrationRepository.findAllWithDetailsOrderByCreatedAtDesc();
    }

    public List<FxEligibility> listEligible(Student student) {
        return finalGradeRepository.findByStudentIdAndPublishedTrueWithDetails(student.getId()).stream()
                .filter(finalGrade -> finalGrade.getNumericValue() < 50.0)
                .filter(finalGrade -> finalGrade.getSubjectOffering() != null)
                .map(finalGrade -> new FxEligibility(
                        finalGrade.getSubjectOffering().getId(),
                        finalGrade.getSubjectOffering().getSubject().getCode(),
                        finalGrade.getSubjectOffering().getSubject().getName(),
                        finalGrade.getNumericValue(),
                        hasExistingFx(student.getId(), finalGrade.getSubjectOffering().getId())
                ))
                .toList();
    }

    @Transactional
    public FxRegistration submit(Student student, Long subjectOfferingId) {
        if (student.getCurrentSemester() == null
                || !windowPolicyService.isWindowActive(student.getCurrentSemester().getId(), ru.kors.finalproject.entity.RegistrationWindow.WindowType.FX)) {
            throw new IllegalStateException("FX window is closed");
        }
        if (addDropService.hasActiveRegistrationHold(student)) {
            throw new IllegalStateException("FX is blocked due to active holds");
        }
        SubjectOffering offering = subjectOfferingRepository.findByIdWithDetails(subjectOfferingId)
                .orElseThrow(() -> new IllegalArgumentException("Section not found"));

        boolean eligible = finalGradeRepository.findByStudentIdAndPublishedTrueWithDetails(student.getId()).stream()
                .anyMatch(finalGrade -> finalGrade.getSubjectOffering() != null
                        && finalGrade.getSubjectOffering().getId().equals(subjectOfferingId)
                        && finalGrade.getNumericValue() < 50.0);
        if (!eligible) {
            throw new IllegalStateException("Student is not eligible for FX in this course");
        }
        if (hasExistingFx(student.getId(), subjectOfferingId)) {
            throw new IllegalStateException("FX registration already exists for this section");
        }

        FxRegistration saved = fxRegistrationRepository.save(FxRegistration.builder()
                .student(student)
                .subjectOffering(offering)
                .status(FxRegistration.FxStatus.PENDING)
                .createdAt(Instant.now())
                .build());

        notificationService.notifyStudent(
                student.getEmail(),
                Notification.NotificationType.ENROLLMENT,
                "FX request submitted",
                "FX request for " + offering.getSubject().getCode() + " is now pending review",
                "/app/student/registration"
        );
        notificationService.notifyUsers(
                userRepository.findByRoleAndEnabledTrue(User.UserRole.ADMIN),
                Notification.NotificationType.ENROLLMENT,
                "New FX request",
                "Student " + student.getName() + " submitted FX for " + offering.getSubject().getCode(),
                "/app/admin/registration"
        );
        auditService.logStudentAction(student, "FX_REQUEST_CREATED", "FxRegistration", saved.getId(),
                "subjectOfferingId=" + subjectOfferingId);
        return saved;
    }

    @Transactional
    public FxRegistration updateStatus(Long fxRegistrationId, FxRegistration.FxStatus status, User admin) {
        FxRegistration fx = fxRegistrationRepository.findById(fxRegistrationId)
                .orElseThrow(() -> new IllegalArgumentException("FX registration not found"));
        fx.setStatus(status);
        FxRegistration saved = fxRegistrationRepository.save(fx);

        if (status == FxRegistration.FxStatus.APPROVED) {
            ensureFxInvoice(saved);
        }

        notificationService.notifyStudent(
                saved.getStudent().getEmail(),
                Notification.NotificationType.ENROLLMENT,
                "FX status updated",
                "FX for " + saved.getSubjectOffering().getSubject().getCode() + " is now " + status,
                "/app/student/registration"
        );
        auditService.logUserAction(admin, "FX_STATUS_UPDATED", "FxRegistration", saved.getId(),
                "status=" + status);
        return saved;
    }

    private boolean hasExistingFx(Long studentId, Long subjectOfferingId) {
        return fxRegistrationRepository.findByStudentIdWithDetailsOrderByCreatedAtDesc(studentId).stream()
                .anyMatch(fx -> fx.getSubjectOffering() != null && fx.getSubjectOffering().getId().equals(subjectOfferingId));
    }

    private void ensureFxInvoice(FxRegistration fx) {
        String description = buildFxInvoiceDescription(fx);
        boolean exists = chargeRepository.findByStudentId(fx.getStudent().getId()).stream()
                .anyMatch(charge -> description.equalsIgnoreCase(charge.getDescription()));
        if (!exists) {
            financialService.createInvoice(
                    fx.getStudent(),
                    DEFAULT_FX_FEE,
                    description,
                    LocalDate.now().plusDays(7)
            );
        }
    }

    private String buildFxInvoiceDescription(FxRegistration fx) {
        return String.format(
                Locale.US,
                "FX registration fee for %s %s (section #%d)",
                fx.getSubjectOffering().getSubject().getCode(),
                fx.getSubjectOffering().getSubject().getName(),
                fx.getSubjectOffering().getId()
        );
    }

    public record FxEligibility(Long sectionId, String subjectCode, String subjectName, double finalScore, boolean alreadyRequested) {}
}
