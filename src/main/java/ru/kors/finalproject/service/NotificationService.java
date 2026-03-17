package ru.kors.finalproject.service;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import ru.kors.finalproject.entity.Notification;
import ru.kors.finalproject.entity.User;
import ru.kors.finalproject.repository.NotificationRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public void notifyStudent(String email, Notification.NotificationType type, String title, String message, String link) {
        notify(email, type, title, message, link);
    }

    public void notifyUser(User user, Notification.NotificationType type, String title, String message, String link) {
        if (user == null) {
            return;
        }
        notify(user.getEmail(), type, title, message, link);
    }

    public void notifyMany(Collection<String> emails, Notification.NotificationType type, String title, String message, String link) {
        if (emails == null || emails.isEmpty()) {
            return;
        }
        emails.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(email -> !email.isBlank())
                .distinct()
                .forEach(email -> notify(email, type, title, message, link));
    }

    public void notifyUsers(Collection<User> users, Notification.NotificationType type, String title, String message, String link) {
        if (users == null || users.isEmpty()) {
            return;
        }
        notifyMany(users.stream().map(User::getEmail).toList(), type, title, message, link);
    }

    public void notify(String email, Notification.NotificationType type, String title, String message, String link) {
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
        Notification saved = notificationRepository.save(notification);
        publishUpdate(email, saved.getId());
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
            Notification saved = notificationRepository.save(n);
            publishUpdate(saved.getRecipientEmail(), saved.getId());
        });
    }

    public void markReadForEmail(Long id, String email) {
        notificationRepository.findById(id)
                .filter(notification -> notification.getRecipientEmail() != null
                        && notification.getRecipientEmail().equalsIgnoreCase(email))
                .ifPresent(notification -> {
                    notification.setRead(true);
                    Notification saved = notificationRepository.save(notification);
                    publishUpdate(email, saved.getId());
                });
    }

    public void markAllReadForEmail(String email) {
        if (email == null || email.isBlank()) {
            return;
        }
        List<Notification> notifications = notificationRepository.findByRecipientEmailOrderByCreatedAtDesc(email);
        notifications.stream()
                .filter(notification -> !notification.isRead())
                .forEach(notification -> notification.setRead(true));
        notificationRepository.saveAll(notifications);
        publishUpdate(email, null);
    }

    private void publishUpdate(String email, Long notificationId) {
        if (email == null || email.isBlank()) {
            return;
        }
        messagingTemplate.convertAndSendToUser(
                email,
                "/queue/notifications",
                new NotificationRealtimeEvent(
                        "NOTIFICATIONS_UPDATED",
                        unreadCount(email),
                        notificationId,
                        Instant.now()
                )
        );
    }

    public record NotificationRealtimeEvent(String eventType, long unreadCount, Long notificationId, Instant timestamp) {}
}
