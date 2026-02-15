package ru.kors.finalproject.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.kors.finalproject.entity.MobilityApplication;
import ru.kors.finalproject.entity.Notification;
import ru.kors.finalproject.entity.User;
import ru.kors.finalproject.repository.MobilityApplicationRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MobilityService {

    private final MobilityApplicationRepository mobilityApplicationRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;

    public List<MobilityApplication> listAll() {
        return mobilityApplicationRepository.findAllWithStudent();
    }

    public List<MobilityApplication> listByStatus(MobilityApplication.MobilityStatus status) {
        return mobilityApplicationRepository.findAll().stream()
                .filter(a -> a.getStatus() == status)
                .toList();
    }

    public List<MobilityApplication> listByStudent(Long studentId) {
        return mobilityApplicationRepository.findByStudentIdOrderByCreatedAtDesc(studentId);
    }

    @Transactional
    public MobilityApplication updateStatus(Long applicationId, MobilityApplication.MobilityStatus newStatus,
                                             User actor) {
        MobilityApplication app = mobilityApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Mobility application not found"));

        validateStatusTransition(app.getStatus(), newStatus);

        MobilityApplication.MobilityStatus oldStatus = app.getStatus();
        app.setStatus(newStatus);
        MobilityApplication saved = mobilityApplicationRepository.save(app);

        notificationService.notifyStudent(
                app.getStudent().getEmail(),
                Notification.NotificationType.MOBILITY,
                "Mobility application status updated",
                "Your mobility application to " + app.getUniversityName()
                        + " has been updated to " + newStatus,
                "/portal/academic-mobility"
        );
        auditService.logUserAction(actor, "MOBILITY_STATUS_CHANGED", "MobilityApplication", saved.getId(),
                "oldStatus=" + oldStatus + ", newStatus=" + newStatus);
        return saved;
    }

    private void validateStatusTransition(MobilityApplication.MobilityStatus current,
                                           MobilityApplication.MobilityStatus next) {
        boolean valid = switch (current) {
            case DRAFT -> next == MobilityApplication.MobilityStatus.SUBMITTED;
            case SUBMITTED -> next == MobilityApplication.MobilityStatus.IN_REVIEW
                    || next == MobilityApplication.MobilityStatus.REJECTED;
            case IN_REVIEW -> next == MobilityApplication.MobilityStatus.APPROVED
                    || next == MobilityApplication.MobilityStatus.REJECTED;
            case APPROVED, REJECTED -> false;
        };
        if (!valid) {
            throw new IllegalStateException("Invalid status transition from " + current + " to " + next);
        }
    }
}
