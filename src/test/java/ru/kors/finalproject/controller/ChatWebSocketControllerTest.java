package ru.kors.finalproject.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import ru.kors.finalproject.entity.User;
import ru.kors.finalproject.service.ChatService;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ChatWebSocketController.handleMessage.
 *
 * Tests:
 *   - valid message is saved and broadcast to /topic/chat/{roomId}
 *   - blank content is silently dropped (no save, no broadcast)
 *   - null content is silently dropped
 *   - null principal results in no-op
 *   - principal without User details results in no-op
 *   - broadcast destination is exactly /topic/chat/{roomId}
 */
@ExtendWith(MockitoExtension.class)
class ChatWebSocketControllerTest {

    @Mock private ChatService chatService;
    @Mock private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private ChatWebSocketController controller;

    private User student;
    private Principal principal;

    @BeforeEach
    void setUp() {
        student = User.builder()
                .id(1L).email("a_ws@kbtu.kz").fullName("WS Student")
                .role(User.UserRole.STUDENT).enabled(true).build();

        // Principal backed by UsernamePasswordAuthenticationToken with User as principal
        principal = new UsernamePasswordAuthenticationToken(student, null, List.of());
    }

    // =========================================================================
    // Happy path
    // =========================================================================

    @Test
    @DisplayName("handleMessage - saves message and broadcasts to correct topic")
    void handleMessage_savesAndBroadcasts() {
        ChatService.MessageDto dto = new ChatService.MessageDto(
                42L, 10L, 1L, "WS Student", "STUDENT", "Hello room", "2025-01-01T00:00:00Z");
        when(chatService.sendMessage(10L, student, "Hello room")).thenReturn(dto);

        controller.handleMessage(10L, Map.of("content", "Hello room"), principal);

        verify(chatService).sendMessage(10L, student, "Hello room");

        ArgumentCaptor<String> destCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate).convertAndSend(destCaptor.capture(), (Object) payloadCaptor.capture());

        assertThat(destCaptor.getValue()).isEqualTo("/topic/chat/10");
        assertThat(payloadCaptor.getValue()).isSameAs(dto);
    }

    @Test
    @DisplayName("handleMessage - content is trimmed / whitespace preserved in service call")
    void handleMessage_contentPassedAsIs() {
        ChatService.MessageDto dto = new ChatService.MessageDto(
                1L, 5L, 1L, "WS Student", "STUDENT", "  padded  ", "2025-01-01T00:00:00Z");
        when(chatService.sendMessage(5L, student, "  padded  ")).thenReturn(dto);

        controller.handleMessage(5L, Map.of("content", "  padded  "), principal);

        verify(chatService).sendMessage(5L, student, "  padded  ");
        verify(messagingTemplate).convertAndSend(eq("/topic/chat/5"), (Object) any());
    }

    // =========================================================================
    // Blank / null content — silently dropped
    // =========================================================================

    @Test
    @DisplayName("handleMessage - blank content is silently dropped (no chatService call)")
    void handleMessage_blankContent_noOp() {
        controller.handleMessage(10L, Map.of("content", "   "), principal);

        verifyNoInteractions(chatService);
        verifyNoInteractions(messagingTemplate);
    }

    @Test
    @DisplayName("handleMessage - empty string content is silently dropped")
    void handleMessage_emptyContent_noOp() {
        controller.handleMessage(10L, Map.of("content", ""), principal);

        verifyNoInteractions(chatService);
        verifyNoInteractions(messagingTemplate);
    }

    @Test
    @DisplayName("handleMessage - missing 'content' key is silently dropped")
    void handleMessage_missingContentKey_noOp() {
        controller.handleMessage(10L, Map.of(), principal);

        verifyNoInteractions(chatService);
        verifyNoInteractions(messagingTemplate);
    }

    // =========================================================================
    // Invalid principals — silently dropped
    // =========================================================================

    @Test
    @DisplayName("handleMessage - null principal results in no-op")
    void handleMessage_nullPrincipal_noOp() {
        controller.handleMessage(10L, Map.of("content", "Hello"), null);

        verifyNoInteractions(chatService);
        verifyNoInteractions(messagingTemplate);
    }

    @Test
    @DisplayName("handleMessage - principal that is not UsernamePasswordAuthenticationToken is silently dropped")
    void handleMessage_unknownPrincipalType_noOp() {
        Principal fakePrincipal = () -> "some-name";

        controller.handleMessage(10L, Map.of("content", "Hello"), fakePrincipal);

        verifyNoInteractions(chatService);
        verifyNoInteractions(messagingTemplate);
    }

    @Test
    @DisplayName("handleMessage - UsernamePasswordAuthenticationToken whose principal is a String (not User) is dropped")
    void handleMessage_stringPrincipal_noOp() {
        // Some contexts may put a username String as principal instead of User object
        Principal stringPrincipal = new UsernamePasswordAuthenticationToken("just-a-string", null, List.of());

        controller.handleMessage(10L, Map.of("content", "Hello"), stringPrincipal);

        verifyNoInteractions(chatService);
        verifyNoInteractions(messagingTemplate);
    }

    // =========================================================================
    // User in details (fallback extraction)
    // =========================================================================

    @Test
    @DisplayName("handleMessage - extracts User from token details when principal field is not User")
    void handleMessage_userInDetails_broadcastsSuccessfully() {
        // Construct auth token where principal is a String but details holds the User
        UsernamePasswordAuthenticationToken authWithDetails =
                new UsernamePasswordAuthenticationToken("email@x.kz", null, List.of());
        authWithDetails.setDetails(student);

        ChatService.MessageDto dto = new ChatService.MessageDto(
                1L, 7L, 1L, "WS Student", "STUDENT", "from details", "2025-01-01T00:00:00Z");
        when(chatService.sendMessage(7L, student, "from details")).thenReturn(dto);

        controller.handleMessage(7L, Map.of("content", "from details"), authWithDetails);

        verify(chatService).sendMessage(7L, student, "from details");
        verify(messagingTemplate).convertAndSend(eq("/topic/chat/7"), (Object) any());
    }
}
