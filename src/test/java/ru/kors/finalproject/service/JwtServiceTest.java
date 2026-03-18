package ru.kors.finalproject.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.kors.finalproject.entity.User;

import java.util.EnumSet;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for JwtService: token generation, claim extraction, and signature validation.
 * Uses a fixed test secret — no Spring context needed.
 */
class JwtServiceTest {

    private static final String TEST_SECRET = "test-secret-key-that-is-long-enough-for-hmac-sha256-algorithm-needs-32-bytes";
    private static final long EXPIRY_MINUTES = 60;

    private JwtService jwtService;

    private User studentUser;
    private User professorUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(TEST_SECRET, EXPIRY_MINUTES);

        studentUser = User.builder()
                .id(1L)
                .email("a_student@kbtu.kz")
                .fullName("Student One")
                .role(User.UserRole.STUDENT)
                .adminPermissions(EnumSet.noneOf(User.AdminPermission.class))
                .enabled(true)
                .build();

        professorUser = User.builder()
                .id(2L)
                .email("z.professor@kbtu.kz")
                .fullName("Prof Z")
                .role(User.UserRole.PROFESSOR)
                .adminPermissions(EnumSet.noneOf(User.AdminPermission.class))
                .enabled(true)
                .build();

        adminUser = User.builder()
                .id(3L)
                .email("admin@kbtu.kz")
                .fullName("Admin")
                .role(User.UserRole.ADMIN)
                .adminPermissions(EnumSet.allOf(User.AdminPermission.class))
                .enabled(true)
                .build();
    }

    // -------------------------------------------------------------------------
    // Token generation
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("generateAccessToken - sets subject to user ID string")
    void generateAccessToken_subjectIsUserId() {
        String token = jwtService.generateAccessToken(studentUser);
        Claims claims = jwtService.parse(token);
        assertThat(claims.getSubject()).isEqualTo("1");
    }

    @Test
    @DisplayName("generateAccessToken - embeds email claim")
    void generateAccessToken_containsEmailClaim() {
        String token = jwtService.generateAccessToken(studentUser);
        Claims claims = jwtService.parse(token);
        assertThat(claims.get("email", String.class)).isEqualTo("a_student@kbtu.kz");
    }

    @Test
    @DisplayName("generateAccessToken - embeds role claim")
    void generateAccessToken_containsRoleClaim() {
        String token = jwtService.generateAccessToken(professorUser);
        Claims claims = jwtService.parse(token);
        assertThat(claims.get("role", String.class)).isEqualTo("PROFESSOR");
    }

    @Test
    @DisplayName("generateAccessToken - expiration is set to configured minutes from now")
    void generateAccessToken_expirationWithinExpectedWindow() {
        long before = System.currentTimeMillis();
        String token = jwtService.generateAccessToken(studentUser);
        long after = System.currentTimeMillis();

        Claims claims = jwtService.parse(token);
        long expMs = claims.getExpiration().getTime();

        long expectedMinExpiry = before + (EXPIRY_MINUTES - 1) * 60_000L;
        long expectedMaxExpiry = after  + (EXPIRY_MINUTES + 1) * 60_000L;

        assertThat(expMs).isBetween(expectedMinExpiry, expectedMaxExpiry);
    }

    @Test
    @DisplayName("getAccessExpirationSeconds - returns configured minutes * 60")
    void getAccessExpirationSeconds_isConfiguredValue() {
        assertThat(jwtService.getAccessExpirationSeconds()).isEqualTo(EXPIRY_MINUTES * 60);
    }

    // -------------------------------------------------------------------------
    // Token parsing / validation
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("parse - returns valid claims for a freshly generated token")
    void parse_validToken() {
        String token = jwtService.generateAccessToken(adminUser);
        Claims claims = jwtService.parse(token);
        assertThat(claims.getSubject()).isEqualTo("3");
        assertThat(claims.get("role", String.class)).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("parse - throws for tampered token")
    void parse_tamperedToken() {
        String token = jwtService.generateAccessToken(studentUser);
        // Replace the signature segment entirely with a fake one
        int lastDot = token.lastIndexOf('.');
        String tampered = token.substring(0, lastDot + 1) + "ZmFrZXNpZ25hdHVyZXRoYXR3aWxsbmV2ZXJtYXRjaA";
        assertThatThrownBy(() -> jwtService.parse(tampered))
                .isInstanceOfAny(SignatureException.class, io.jsonwebtoken.JwtException.class);
    }

    @Test
    @DisplayName("parse - throws for token signed with a different key")
    void parse_wrongSecret() {
        JwtService otherService = new JwtService("completely-different-secret-key-for-testing-purposes-long", 60);
        String token = otherService.generateAccessToken(studentUser);
        assertThatThrownBy(() -> jwtService.parse(token))
                .isInstanceOfAny(SignatureException.class, io.jsonwebtoken.JwtException.class);
    }

    @Test
    @DisplayName("generate - delegates to generateAccessToken (backwards-compat alias)")
    void generate_delegatesToGenerateAccessToken() {
        String t1 = jwtService.generate(studentUser);
        Claims c1 = jwtService.parse(t1);
        assertThat(c1.getSubject()).isEqualTo("1");
    }
}
