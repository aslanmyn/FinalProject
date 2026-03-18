package ru.kors.finalproject.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import ru.kors.finalproject.entity.User;
import ru.kors.finalproject.repository.UserRepository;

import java.util.EnumSet;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for /api/v1/auth/** endpoints.
 *
 * Uses H2 in-memory, creates a real User through the repo, then tests:
 *   - login happy path + token structure
 *   - login with wrong password
 *   - login with non-existent account
 *   - login validation (blank fields, invalid email)
 *   - register happy path (student role auto-detected from email pattern)
 *   - register – duplicate email
 *   - register – password mismatch
 *   - register – unknown email pattern
 *   - logout
 *   - refresh token rotation
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthApiIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String STUDENT_EMAIL = "a_test@kbtu.kz";
    private static final String PASSWORD      = "secret123";

    @BeforeEach
    void seedUser() {
        // Persist a student user directly (bypasses registration flow)
        if (!userRepository.existsByEmail(STUDENT_EMAIL)) {
            userRepository.save(User.builder()
                    .email(STUDENT_EMAIL)
                    .password(passwordEncoder.encode(PASSWORD))
                    .fullName("Test Student")
                    .role(User.UserRole.STUDENT)
                    .adminPermissions(EnumSet.noneOf(User.AdminPermission.class))
                    .enabled(true)
                    .build());
        }
    }

    // -------------------------------------------------------------------------
    // login
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("POST /auth/login - 200 with access & refresh tokens for valid credentials")
    void login_validCredentials_returns200WithTokens() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", STUDENT_EMAIL, "password", PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", not(emptyString())))
                .andExpect(jsonPath("$.refreshToken", not(emptyString())))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.role").value("STUDENT"));
    }

    @Test
    @DisplayName("POST /auth/login - 401 for correct email but wrong password")
    void login_wrongPassword_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", STUDENT_EMAIL, "password", "WRONGPASSWORD")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /auth/login - 401 for unknown email")
    void login_unknownEmail_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", "nobody@kbtu.kz", "password", PASSWORD)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /auth/login - 400 for blank email (Bean Validation)")
    void login_blankEmail_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", "", "password", PASSWORD)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /auth/login - 400 for invalid email format")
    void login_invalidEmailFormat_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", "not-an-email", "password", PASSWORD)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /auth/login - 400 for password shorter than minimum length")
    void login_shortPassword_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", STUDENT_EMAIL, "password", "x")))
                .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------------------
    // register
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("POST /auth/register - 200 and creates user for valid student email")
    void register_studentEmail_creates200() throws Exception {
        String newEmail = "a_newstudent@kbtu.kz";

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRegister(newEmail, "password123", "password123", "New Student")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(newEmail))
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.status").value("registered"));

        assertThat(userRepository.existsByEmail(newEmail)).isTrue();
    }

    @Test
    @DisplayName("POST /auth/register - 200 and creates PROFESSOR for professor email pattern")
    void register_professorEmail_createsProfessor() throws Exception {
        String profEmail = "z.testprof@kbtu.kz";

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRegister(profEmail, "password123", "password123", "Prof Test")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("PROFESSOR"));
    }

    @Test
    @DisplayName("POST /auth/register - 4xx for duplicate email")
    void register_duplicateEmail_returnsError() throws Exception {
        // STUDENT_EMAIL already exists from @BeforeEach
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRegister(STUDENT_EMAIL, PASSWORD, PASSWORD, "Duplicate")))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("POST /auth/register - 4xx when passwords do not match")
    void register_passwordMismatch_returnsError() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRegister("a_mismatch@kbtu.kz", "password123", "different123", "Mismatch")))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("POST /auth/register - 4xx for unknown email format (neither student/professor/admin pattern)")
    void register_unknownEmailPattern_returnsError() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRegister("unknown@kbtu.kz", "password123", "password123", "Unknown")))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("POST /auth/register - 400 for blank fullName")
    void register_blankFullName_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRegister("a_nofullname@kbtu.kz", "password123", "password123", "")))
                .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------------------
    // logout + refresh
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("POST /auth/logout - 200 ok")
    void logout_withValidRefreshToken_returns200() throws Exception {
        // First obtain a real refresh token
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", STUDENT_EMAIL, "password", PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();

        String body = loginResult.getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        Map<String, Object> tokens = objectMapper.readValue(body, Map.class);
        String refreshToken = (String) tokens.get("refreshToken");

        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    @DisplayName("POST /auth/refresh - returns new tokens for valid refresh token")
    void refresh_withValidToken_returnsNewAccessToken() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", STUDENT_EMAIL, "password", PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();

        @SuppressWarnings("unchecked")
        Map<String, Object> tokens = objectMapper.readValue(loginResult.getResponse().getContentAsString(), Map.class);
        String refreshToken = (String) tokens.get("refreshToken");
        String originalAccess = (String) tokens.get("accessToken");

        // Small sleep to ensure different iat
        Thread.sleep(1001);

        MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", not(emptyString())))
                .andReturn();

        @SuppressWarnings("unchecked")
        Map<String, Object> newTokens = objectMapper.readValue(refreshResult.getResponse().getContentAsString(), Map.class);
        // New access token should be different (new iat)
        assertThat(newTokens.get("accessToken")).isNotEqualTo(originalAccess);
    }

    @Test
    @DisplayName("POST /auth/refresh - 4xx for invalid/unknown refresh token")
    void refresh_withInvalidToken_returnsError() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"completely-fake-token-that-does-not-exist\"}"))
                .andExpect(status().is4xxClientError());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static String json(String k1, String v1, String k2, String v2) {
        return String.format("{\"" + k1 + "\":\"%s\",\"" + k2 + "\":\"%s\"}", v1, v2);
    }

    private static String jsonRegister(String email, String pass, String confirm, String name) {
        return String.format(
                "{\"email\":\"%s\",\"password\":\"%s\",\"confirmPassword\":\"%s\",\"fullName\":\"%s\"}",
                email, pass, confirm, name);
    }
}
