package ru.kors.finalproject.controller.api.v1;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.kors.finalproject.entity.User;
import ru.kors.finalproject.repository.UserRepository;
import ru.kors.finalproject.service.JwtService;
import ru.kors.finalproject.service.RefreshTokenService;
import ru.kors.finalproject.web.api.v1.ApiUnauthorizedException;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthV1Controller {
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ApiUnauthorizedException("Invalid credentials"));
        if (!user.isEnabled() || !user.validatePassword(request.password())) {
            throw new ApiUnauthorizedException("Invalid credentials");
        }
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = refreshTokenService.issue(user);
        return ResponseEntity.ok(tokenResponse(user, accessToken, refreshToken));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@Valid @RequestBody RefreshRequest request) {
        var rotated = refreshTokenService.rotate(request.refreshToken());
        String accessToken = jwtService.generateAccessToken(rotated.user());
        return ResponseEntity.ok(tokenResponse(rotated.user(), accessToken, rotated.refreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@Valid @RequestBody RefreshRequest request) {
        refreshTokenService.revoke(request.refreshToken());
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    private Map<String, Object> tokenResponse(User user, String accessToken, String refreshToken) {
        return Map.of(
                "tokenType", "Bearer",
                "accessToken", accessToken,
                "accessTokenExpiresInSeconds", jwtService.getAccessExpirationSeconds(),
                "refreshToken", refreshToken,
                "refreshTokenExpiresInDays", refreshTokenService.getRefreshExpirationDays(),
                "role", user.getRole(),
                "permissions", user.getAdminPermissions()
        );
    }

    public record LoginRequest(
            @jakarta.validation.constraints.NotBlank(message = "Email is required")
            @jakarta.validation.constraints.Email
            String email,
            @jakarta.validation.constraints.NotBlank(message = "Password is required")
            @jakarta.validation.constraints.Size(min = 6)
            String password) {
    }

    public record RefreshRequest(
            @jakarta.validation.constraints.NotBlank(message = "Refresh token is required")
            String refreshToken) {
    }
}
