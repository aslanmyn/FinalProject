package ru.kors.finalproject.config;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import ru.kors.finalproject.entity.User;
import ru.kors.finalproject.repository.UserRepository;
import ru.kors.finalproject.service.JwtService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(WebSocketAuthInterceptor.class);

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("WebSocket CONNECT rejected: missing or invalid Authorization header");
                throw new org.springframework.security.access.AccessDeniedException("Missing authentication token");
            }

            try {
                String token = authHeader.substring(7);
                var claims = jwtService.parse(token);
                Long userId = Long.parseLong(claims.getSubject());

                User user = userRepository.findById(userId).orElse(null);
                if (user == null || !user.isEnabled()) {
                    log.warn("WebSocket CONNECT rejected: user not found or disabled (id={})", userId);
                    throw new org.springframework.security.access.AccessDeniedException("User not found or disabled");
                }

                List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
                if (user.getRole() == User.UserRole.ADMIN && user.getAdminPermissions() != null) {
                    for (User.AdminPermission perm : user.getAdminPermissions()) {
                        authorities.add(new SimpleGrantedAuthority("PERM_" + perm.name()));
                    }
                }
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(user, null, authorities);
                accessor.setUser(auth);
            } catch (org.springframework.security.access.AccessDeniedException ex) {
                throw ex;
            } catch (Exception ex) {
                log.warn("WebSocket CONNECT rejected: token validation failed - {}", ex.getMessage());
                throw new org.springframework.security.access.AccessDeniedException("Invalid authentication token");
            }
        }

        return message;
    }
}

