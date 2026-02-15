package ru.kors.finalproject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.kors.finalproject.entity.AuditLog;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}
