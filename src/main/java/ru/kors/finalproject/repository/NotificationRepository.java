package ru.kors.finalproject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.kors.finalproject.entity.Notification;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByRecipientEmailOrderByCreatedAtDesc(String recipientEmail);

    long countByRecipientEmailAndReadFalse(String recipientEmail);
}
