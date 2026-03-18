package ru.kors.finalproject.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import ru.kors.finalproject.entity.Notification;
import ru.kors.finalproject.entity.User;
import ru.kors.finalproject.repository.NotificationRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for NotificationService:
 *  - persistence of notifications
 *  - WebSocket push via SimpMessagingTemplate
 *  - markRead / markReadForEmail / markAllReadForEmail ownership validation
 *  - null / blank email guards
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private NotificationService notificationService;

    private static final String EMAIL = "a_student@kbtu.kz";

    private Notification savedNotification(long id, String email, boolean read) {
        return Notification.builder()
                .id(id)
                .recipientEmail(email)
                .type(Notification.NotificationType.ENROLLMENT)
                .title("Test")
                .message("msg")
                .read(read)
                .createdAt(Instant.now())
                .build();
    }

    /**
     * Stubs repository.save() and countByRecipientEmailAndReadFalse().
     * Call this at the start of any test that goes through notify() → save → publishUpdate.
     */
    private void stubSaveAndCount() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));
        when(notificationRepository.countByRecipientEmailAndReadFalse(anyString())).thenReturn(0L);
    }

    // -------------------------------------------------------------------------
    // notify / notifyStudent
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("notifyStudent - saves notification with correct fields")
    void notifyStudent_persistsNotification() {
        stubSaveAndCount();

        notificationService.notifyStudent(EMAIL, Notification.NotificationType.GRADE,
                "Grade published", "CS101: 90/100", "/app/journal");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());

        Notification saved = captor.getValue();
        assertThat(saved.getRecipientEmail()).isEqualTo(EMAIL);
        assertThat(saved.getType()).isEqualTo(Notification.NotificationType.GRADE);
        assertThat(saved.getTitle()).isEqualTo("Grade published");
        assertThat(saved.isRead()).isFalse();
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("notifyStudent - pushes WebSocket update to user queue")
    void notifyStudent_publishesWebSocketEvent() {
        stubSaveAndCount();

        notificationService.notifyStudent(EMAIL, Notification.NotificationType.SCHEDULE,
                "Schedule update", "Room changed", "/app/schedule");

        verify(messagingTemplate).convertAndSendToUser(
                eq(EMAIL), eq("/queue/notifications"), any());
    }

    @Test
    @DisplayName("notify - does nothing for null email")
    void notify_nullEmail_isNoOp() {
        notificationService.notify(null, Notification.NotificationType.SYSTEM, "t", "m", "/");
        verifyNoInteractions(notificationRepository, messagingTemplate);
    }

    @Test
    @DisplayName("notify - does nothing for blank email")
    void notify_blankEmail_isNoOp() {
        notificationService.notify("   ", Notification.NotificationType.SYSTEM, "t", "m", "/");
        verifyNoInteractions(notificationRepository, messagingTemplate);
    }

    @Test
    @DisplayName("notifyUser - does nothing when user is null")
    void notifyUser_nullUser_isNoOp() {
        notificationService.notifyUser(null, Notification.NotificationType.SYSTEM, "t", "m", "/");
        verifyNoInteractions(notificationRepository, messagingTemplate);
    }

    @Test
    @DisplayName("notifyUser - sends to user email when user is non-null")
    void notifyUser_sendsToUserEmail() {
        stubSaveAndCount();
        User user = User.builder().id(1L).email(EMAIL).fullName("Test").role(User.UserRole.STUDENT).enabled(true).build();
        notificationService.notifyUser(user, Notification.NotificationType.FINANCE, "Invoice", "msg", "/");
        verify(notificationRepository).save(argThat(n -> EMAIL.equals(n.getRecipientEmail())));
    }

    // -------------------------------------------------------------------------
    // notifyMany
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("notifyMany - deduplicates emails and sends once per unique address")
    void notifyMany_deduplicatesEmails() {
        stubSaveAndCount();
        notificationService.notifyMany(
                List.of(EMAIL, EMAIL, "other@kbtu.kz"),
                Notification.NotificationType.SYSTEM, "t", "m", "/");

        // 2 unique emails → 2 saves
        verify(notificationRepository, times(2)).save(any(Notification.class));
    }

    @Test
    @DisplayName("notifyMany - does nothing for empty collection")
    void notifyMany_emptyCollection_isNoOp() {
        notificationService.notifyMany(List.of(), Notification.NotificationType.SYSTEM, "t", "m", "/");
        verifyNoInteractions(notificationRepository);
    }

    // -------------------------------------------------------------------------
    // markRead / markReadForEmail
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("markRead - sets read=true and pushes WS update")
    void markRead_setsReadTrue() {
        stubSaveAndCount();
        Notification unread = savedNotification(1L, EMAIL, false);
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(unread));

        notificationService.markRead(1L);

        verify(notificationRepository).save(argThat(Notification::isRead));
        verify(messagingTemplate).convertAndSendToUser(eq(EMAIL), eq("/queue/notifications"), any());
    }

    @Test
    @DisplayName("markRead - no-op for unknown notification id")
    void markRead_unknownId_isNoOp() {
        when(notificationRepository.findById(999L)).thenReturn(Optional.empty());
        notificationService.markRead(999L);
        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("markReadForEmail - only marks notification owned by the given email")
    void markReadForEmail_onlyMarksOwned() {
        stubSaveAndCount();
        Notification n = savedNotification(5L, EMAIL, false);
        when(notificationRepository.findById(5L)).thenReturn(Optional.of(n));

        notificationService.markReadForEmail(5L, EMAIL);
        verify(notificationRepository).save(argThat(Notification::isRead));
    }

    @Test
    @DisplayName("markReadForEmail - does NOT mark notification belonging to a different email")
    void markReadForEmail_differentEmailIgnored() {
        Notification n = savedNotification(5L, "someone.else@kbtu.kz", false);
        when(notificationRepository.findById(5L)).thenReturn(Optional.of(n));

        notificationService.markReadForEmail(5L, EMAIL);
        verify(notificationRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // markAllReadForEmail
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("markAllReadForEmail - marks all unread notifications as read")
    void markAllReadForEmail_marksAllUnread() {
        when(notificationRepository.countByRecipientEmailAndReadFalse(anyString())).thenReturn(0L);
        Notification n1 = savedNotification(1L, EMAIL, false);
        Notification n2 = savedNotification(2L, EMAIL, false);
        Notification n3 = savedNotification(3L, EMAIL, true);  // already read
        when(notificationRepository.findByRecipientEmailOrderByCreatedAtDesc(EMAIL))
                .thenReturn(List.of(n1, n2, n3));

        notificationService.markAllReadForEmail(EMAIL);

        verify(notificationRepository).saveAll(argThat(notifications -> {
            @SuppressWarnings("unchecked")
            List<Notification> list = (List<Notification>) notifications;
            return list.stream().allMatch(Notification::isRead);
        }));
    }

    @Test
    @DisplayName("markAllReadForEmail - does nothing for blank email")
    void markAllReadForEmail_blank_isNoOp() {
        notificationService.markAllReadForEmail("  ");
        verifyNoInteractions(notificationRepository);
    }
}
