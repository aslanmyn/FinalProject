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
    private final WorkflowEngineService workflowEngineService;
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

        workflowEngineService.assertMobilityTransition(app.getStatus(), newStatus);

        MobilityApplication.MobilityStatus oldStatus = app.getStatus();
        app.setStatus(newStatus);
        MobilityApplication saved = mobilityApplicationRepository.save(app);

        notificationService.notifyStudent(
                app.getStudent().getEmail(),
                Notification.NotificationType.MOBILITY,
                "Mobility application status updated",
                "Your mobility application to " + app.getUniversityName()
                        + " has been updated to " + newStatus,
                "/app/student"
        );
        auditService.logUserAction(actor, "MOBILITY_STATUS_CHANGED", "MobilityApplication", saved.getId(),
                "oldStatus=" + oldStatus + ", newStatus=" + newStatus);
        return saved;
    }

}
