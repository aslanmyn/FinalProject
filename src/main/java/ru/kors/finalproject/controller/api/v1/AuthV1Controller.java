package ru.kors.finalproject.controller.api.v1;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.kors.finalproject.entity.User;
import ru.kors.finalproject.repository.UserRepository;
import ru.kors.finalproject.service.JwtService;
import ru.kors.finalproject.web.api.v1.ApiUnauthorizedException;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthV1Controller {
    private final UserRepository userRepository;
    private final JwtService jwtService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ApiUnauthorizedException("Invalid credentials"));
        if (!user.isEnabled() || !user.validatePassword(request.password())) {
            throw new ApiUnauthorizedException("Invalid credentials");
        }
        String token = jwtService.generate(user);
        return ResponseEntity.ok(Map.of(
                "tokenType", "Bearer",
                "accessToken", token,
                "role", user.getRole(),
                "permissions", user.getAdminPermissions()
        ));
    }

    public record LoginRequest(String email, String password) {
    }
}
