package ru.kors.finalproject.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.kors.finalproject.entity.RefreshToken;
import ru.kors.finalproject.entity.User;
import ru.kors.finalproject.repository.RefreshTokenRepository;
import ru.kors.finalproject.web.api.v1.ApiUnauthorizedException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${app.jwt.refresh-expiration-days:30}")
    private long refreshExpirationDays;

    @Transactional
    public String issue(User user) {
        String rawToken = generateTokenValue();
        RefreshToken token = RefreshToken.builder()
                .tokenHash(hash(rawToken))
                .user(user)
                .expiresAt(Instant.now().plus(refreshExpirationDays, ChronoUnit.DAYS))
                .revoked(false)
                .createdAt(Instant.now())
                .build();
        refreshTokenRepository.save(token);
        return rawToken;
    }

    @Transactional
    public RefreshRotation rotate(String rawToken) {
        RefreshToken current = findValid(rawToken);
        current.setRevoked(true);
        current.setRevokedAt(Instant.now());
        refreshTokenRepository.save(current);

        String nextRaw = issue(current.getUser());
        return new RefreshRotation(current.getUser(), nextRaw);
    }

    @Transactional
    public void revoke(String rawToken) {
        RefreshToken token = refreshTokenRepository.findByTokenHashAndRevokedFalse(hash(rawToken)).orElse(null);
        if (token == null) {
            return;
        }
        token.setRevoked(true);
        token.setRevokedAt(Instant.now());
        refreshTokenRepository.save(token);
    }

    @Transactional
    public void revokeAllForUser(Long userId) {
        var active = refreshTokenRepository.findByUserIdAndRevokedFalse(userId);
        Instant now = Instant.now();
        active.forEach(t -> {
            t.setRevoked(true);
            t.setRevokedAt(now);
        });
        refreshTokenRepository.saveAll(active);
    }

    public long getRefreshExpirationDays() {
        return refreshExpirationDays;
    }

    private RefreshToken findValid(String rawToken) {
        RefreshToken token = refreshTokenRepository.findByTokenHashAndRevokedFalse(hash(rawToken))
                .orElseThrow(() -> new ApiUnauthorizedException("Invalid refresh token"));
        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new ApiUnauthorizedException("Refresh token expired");
        }
        if (token.getUser() == null || !token.getUser().isEnabled()) {
            throw new ApiUnauthorizedException("User account is disabled");
        }
        return token;
    }

    private static String generateTokenValue() {
        byte[] bytes = new byte[48];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public record RefreshRotation(User user, String refreshToken) {
    }
}
