package ru.kors.finalproject.service;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.kors.finalproject.entity.User;
import ru.kors.finalproject.repository.UserRepository;
import ru.kors.finalproject.web.api.v1.ApiForbiddenException;
import ru.kors.finalproject.web.api.v1.ApiUnauthorizedException;

@Service
@RequiredArgsConstructor
public class MobileApiAuthService {
    private final JwtService jwtService;
    private final UserRepository userRepository;

    public User requireUser(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ApiUnauthorizedException("Missing bearer token");
        }
        String token = authHeader.substring("Bearer ".length());
        Claims claims;
        try {
            claims = jwtService.parse(token);
        } catch (Exception ex) {
            throw new ApiUnauthorizedException("Invalid token");
        }

        Long userId;
        try {
            userId = Long.parseLong(claims.getSubject());
        } catch (Exception ex) {
            throw new ApiUnauthorizedException("Invalid token subject");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiUnauthorizedException("User not found for token"));
        if (!user.isEnabled()) {
            throw new ApiUnauthorizedException("User account is disabled");
        }
        return user;
    }

    public User requireRole(String authHeader, User.UserRole role) {
        User user = requireUser(authHeader);
        if (user.getRole() != role) {
            throw new ApiForbiddenException("Required role: " + role);
        }
        return user;
    }

    public User requireAdminPermission(String authHeader, User.AdminPermission permission) {
        User user = requireRole(authHeader, User.UserRole.ADMIN);
        if (!user.hasPermission(permission)) {
            throw new ApiForbiddenException("Missing admin permission: " + permission);
        }
        return user;
    }
}
