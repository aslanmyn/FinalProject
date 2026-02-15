package ru.kors.finalproject.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.kors.finalproject.entity.AuditLog;
import ru.kors.finalproject.entity.Student;
import ru.kors.finalproject.entity.User;
import ru.kors.finalproject.repository.AuditLogRepository;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuditService {
    private final AuditLogRepository auditLogRepository;

    public void logUserAction(User user, String action, String entityType, Long entityId, String details) {
        AuditLog log = AuditLog.builder()
                .actorUserId(user != null ? user.getId() : null)
                .actorEmail(user != null ? user.getEmail() : "SYSTEM")
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .details(details)
                .createdAt(Instant.now())
                .build();
        auditLogRepository.save(log);
    }

    public void logStudentAction(Student student, String action, String entityType, Long entityId, String details) {
        AuditLog log = AuditLog.builder()
                .actorEmail(student != null ? student.getEmail() : "SYSTEM")
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .details(details)
                .createdAt(Instant.now())
                .build();
        auditLogRepository.save(log);
    }
}
