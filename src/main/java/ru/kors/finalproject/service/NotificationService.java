package ru.kors.finalproject.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.kors.finalproject.entity.Notification;
import ru.kors.finalproject.repository.NotificationRepository;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;

    public void notifyStudent(String email, Notification.NotificationType type, String title, String message, String link) {
        if (email == null || email.isBlank()) {
            return;
        }
        Notification notification = Notification.builder()
                .recipientEmail(email)
                .type(type)
                .title(title)
                .message(message)
                .link(link)
                .read(false)
                .createdAt(Instant.now())
                .build();
        notificationRepository.save(notification);
    }

    public List<Notification> listForEmail(String email) {
        return notificationRepository.findByRecipientEmailOrderByCreatedAtDesc(email);
    }

    public long unreadCount(String email) {
        return notificationRepository.countByRecipientEmailAndReadFalse(email);
    }

    public void markRead(Long id) {
        notificationRepository.findById(id).ifPresent(n -> {
            n.setRead(true);
            notificationRepository.save(n);
        });
    }
}
