package ru.kors.finalproject.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;
import ru.kors.finalproject.entity.User;
import ru.kors.finalproject.service.ChatService;

import java.security.Principal;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat/{roomId}")
    public void handleMessage(@DestinationVariable Long roomId,
                              @Payload Map<String, String> payload,
                              Principal principal) {
        User user = extractUser(principal);
        if (user == null) {
            return;
        }

        String content = payload.get("content");
        if (content == null || content.isBlank()) {
            return;
        }

        ChatService.MessageDto msg = chatService.sendMessage(roomId, user, content);
        messagingTemplate.convertAndSend("/topic/chat/" + roomId, msg);
    }

    private User extractUser(Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken auth) {
            Object p = auth.getPrincipal();
            if (p instanceof User user) {
                return user;
            }
            Object details = auth.getDetails();
            if (details instanceof User user) {
                return user;
            }
        }
        return null;
    }
}
