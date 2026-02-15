package ru.kors.finalproject.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.kors.finalproject.entity.Hold;
import ru.kors.finalproject.entity.Student;
import ru.kors.finalproject.entity.User;
import ru.kors.finalproject.repository.HoldRepository;
import ru.kors.finalproject.repository.StudentRepository;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HoldService {

    private final HoldRepository holdRepository;
    private final StudentRepository studentRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;

    public List<Hold> listActiveHolds(Long studentId) {
        return holdRepository.findByStudentIdAndActiveTrue(studentId);
    }

    public List<Hold> listAllActiveHolds() {
        return holdRepository.findActiveWithStudent();
    }

    @Transactional
    public Hold createHold(Long studentId, Hold.HoldType type, String reason, User actor) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));

        boolean alreadyExists = holdRepository.existsByStudentIdAndTypeAndActiveTrue(studentId, type);
        if (alreadyExists) {
            throw new IllegalStateException("Active hold of type " + type + " already exists for student");
        }

        Hold hold = Hold.builder()
                .student(student)
                .type(type)
                .active(true)
                .reason(reason)
                .createdAt(Instant.now())
                .build();
        Hold saved = holdRepository.save(hold);

        notificationService.notifyStudent(
                student.getEmail(),
                ru.kors.finalproject.entity.Notification.NotificationType.SYSTEM,
                "Hold placed on your account",
                "A " + type + " hold has been placed: " + reason,
                "/portal/student-information"
        );
        auditService.logUserAction(actor, "HOLD_CREATED", "Hold", saved.getId(),
                "studentId=" + studentId + ", type=" + type + ", reason=" + reason);
        return saved;
    }

    @Transactional
    public Hold removeHold(Long holdId, String removalReason, User actor) {
        Hold hold = holdRepository.findById(holdId)
                .orElseThrow(() -> new IllegalArgumentException("Hold not found"));
        if (!hold.isActive()) {
            throw new IllegalStateException("Hold is already resolved");
        }

        hold.setActive(false);
        hold.setResolvedAt(Instant.now());
        Hold saved = holdRepository.save(hold);

        notificationService.notifyStudent(
                hold.getStudent().getEmail(),
                ru.kors.finalproject.entity.Notification.NotificationType.SYSTEM,
                "Hold removed from your account",
                "A " + hold.getType() + " hold has been removed: " + removalReason,
                "/portal/student-information"
        );
        auditService.logUserAction(actor, "HOLD_REMOVED", "Hold", saved.getId(),
                "studentId=" + hold.getStudent().getId() + ", type=" + hold.getType()
                        + ", removalReason=" + removalReason);
        return saved;
    }

    public boolean hasActiveHold(Long studentId, Hold.HoldType type) {
        return holdRepository.existsByStudentIdAndTypeAndActiveTrue(studentId, type);
    }

    public boolean hasAnyActiveHold(Long studentId) {
        return !holdRepository.findByStudentIdAndActiveTrue(studentId).isEmpty();
    }
}
