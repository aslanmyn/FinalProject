package ru.kors.finalproject.integration;

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
import org.springframework.transaction.annotation.Transactional;
import ru.kors.finalproject.entity.User;
import ru.kors.finalproject.repository.UserRepository;
import ru.kors.finalproject.service.JwtService;

import java.util.EnumSet;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for Spring Security role-based access control on /api/v1/** endpoints.
 *
 * Covers:
 *   - Unauthenticated requests → 403 (Spring Security returns 403 for missing credentials on stateless chain)
 *   - Student token → can access /api/v1/student/**, blocked from /api/v1/teacher/** and /api/v1/admin/**
 *   - Professor token → can access /api/v1/teacher/**, blocked from /api/v1/student/** and /api/v1/admin/**
 *   - Admin token → can access /api/v1/admin/**, blocked from /api/v1/student/** and /api/v1/teacher/**
 *   - Public endpoints (/api/v1/public/**, /api/v1/auth/**) are accessible without token
 *   - Chat endpoints require STUDENT or PROFESSOR role
 *   - Admin permission checks (User.hasPermission)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SecurityRoleEnforcementTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;

    private String studentToken;
    private String professorToken;
    private String adminToken;

    @BeforeEach
    void createUsers() {
        User studentUser = createUser("a_sectest@kbtu.kz", "pass123", User.UserRole.STUDENT, EnumSet.noneOf(User.AdminPermission.class));
        User professorUser = createUser("z.sectest@kbtu.kz", "pass123", User.UserRole.PROFESSOR, EnumSet.noneOf(User.AdminPermission.class));
        User adminUser = createUser("admin.sectest@kbtu.kz", "pass123", User.UserRole.ADMIN, EnumSet.allOf(User.AdminPermission.class));
        studentToken  = "Bearer " + jwtService.generateAccessToken(studentUser);
        professorToken = "Bearer " + jwtService.generateAccessToken(professorUser);
        adminToken    = "Bearer " + jwtService.generateAccessToken(adminUser);
    }

    private User createUser(String email, String rawPassword, User.UserRole role, EnumSet<User.AdminPermission> perms) {
        return userRepository.findByEmail(email).orElseGet(() ->
                userRepository.save(User.builder()
                        .email(email)
                        .password(passwordEncoder.encode(rawPassword))
                        .fullName("Test " + role)
                        .role(role)
                        .adminPermissions(perms)
                        .enabled(true)
                        .build()));
    }

    // =========================================================================
    // Unauthenticated access
    // =========================================================================

    @Test
    @DisplayName("GET /api/v1/student/profile - 403 without token")
    void student_profile_noToken_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/student/profile"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/teacher/sections - 403 without token")
    void teacher_sections_noToken_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/teacher/sections"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/admin/users - 403 without token")
    void admin_users_noToken_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/chat/rooms - 403 without token")
    void chat_rooms_noToken_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/chat/rooms"))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // Public endpoints — no token needed
    // =========================================================================

    @Test
    @DisplayName("GET /api/v1/public/news - 200 without token")
    void publicNews_noToken_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/public/news"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - accessible without token (returns 4xx not 403)")
    void authLogin_accessible_withoutToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nobody@x.com\",\"password\":\"123456\"}"))
                .andExpect(status().is4xxClientError())
                // Must NOT be 403 — the endpoint is public
                .andExpect(result -> {
                    int s = result.getResponse().getStatus();
                    org.assertj.core.api.Assertions.assertThat(s).isNotEqualTo(403);
                });
    }

    // =========================================================================
    // Student token — authorised only for /student/**
    // =========================================================================

    @Test
    @DisplayName("Student token - GET /api/v1/student/profile returns non-403")
    void student_canAccess_studentEndpoint() throws Exception {
        // Will be 200, 404 (no Student record), or 401 — but NOT 403 (security passed)
        mockMvc.perform(get("/api/v1/student/profile")
                        .header("Authorization", studentToken))
                .andExpect(result -> {
                    int s = result.getResponse().getStatus();
                    org.assertj.core.api.Assertions.assertThat(s).isNotEqualTo(403);
                });
    }

    @Test
    @DisplayName("Student token - GET /api/v1/teacher/sections returns 403")
    void student_blockedFrom_teacherEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/teacher/sections")
                        .header("Authorization", studentToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Student token - GET /api/v1/admin/users returns 403")
    void student_blockedFrom_adminEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", studentToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Student token - GET /api/v1/chat/rooms passes security (200 or 2xx)")
    void student_canAccess_chatEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/chat/rooms")
                        .header("Authorization", studentToken))
                .andExpect(status().isOk());
    }

    // =========================================================================
    // Professor token — authorised only for /teacher/**
    // =========================================================================

    @Test
    @DisplayName("Professor token - GET /api/v1/teacher/sections returns non-403")
    void professor_canAccess_teacherEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/teacher/sections")
                        .header("Authorization", professorToken))
                .andExpect(result -> {
                    int s = result.getResponse().getStatus();
                    org.assertj.core.api.Assertions.assertThat(s).isNotEqualTo(403);
                });
    }

    @Test
    @DisplayName("Professor token - GET /api/v1/student/profile returns 403")
    void professor_blockedFrom_studentEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/student/profile")
                        .header("Authorization", professorToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Professor token - GET /api/v1/admin/users returns 403")
    void professor_blockedFrom_adminEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", professorToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Professor token - GET /api/v1/chat/rooms passes security (200 or 2xx)")
    void professor_canAccess_chatEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/chat/rooms")
                        .header("Authorization", professorToken))
                .andExpect(status().isOk());
    }

    // =========================================================================
    // Admin token — authorised only for /admin/**
    // =========================================================================

    @Test
    @DisplayName("Admin token - GET /api/v1/admin/users returns non-403")
    void admin_canAccess_adminEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", adminToken))
                .andExpect(result -> {
                    int s = result.getResponse().getStatus();
                    org.assertj.core.api.Assertions.assertThat(s).isNotEqualTo(403);
                });
    }

    @Test
    @DisplayName("Admin token - GET /api/v1/student/profile returns 403")
    void admin_blockedFrom_studentEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/student/profile")
                        .header("Authorization", adminToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin token - GET /api/v1/teacher/sections returns 403")
    void admin_blockedFrom_teacherEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/teacher/sections")
                        .header("Authorization", adminToken))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // Token tampering / expired
    // =========================================================================

    @Test
    @DisplayName("Tampered JWT - GET /api/v1/student/profile returns 403 (token rejected)")
    void tamperedToken_returns403() throws Exception {
        String tampered = studentToken + "GARBAGE";
        mockMvc.perform(get("/api/v1/student/profile")
                        .header("Authorization", tampered))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Completely fake JWT - GET /api/v1/student/profile returns 403")
    void fakeToken_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/student/profile")
                        .header("Authorization", "Bearer totally.fake.token"))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // User.hasPermission unit checks (no HTTP — just the entity logic)
    // =========================================================================

    @Test
    @DisplayName("hasPermission - SUPER grants all admin permissions without explicit list")
    void hasPermission_superGrantsAll() {
        User admin = User.builder().id(1L).email("admin@x.kz").fullName("A")
                .role(User.UserRole.ADMIN)
                .adminPermissions(EnumSet.of(User.AdminPermission.SUPER))
                .enabled(true).build();

        for (User.AdminPermission perm : User.AdminPermission.values()) {
            org.assertj.core.api.Assertions.assertThat(admin.hasPermission(perm))
                    .as("SUPER should grant " + perm)
                    .isTrue();
        }
    }

    @Test
    @DisplayName("hasPermission - non-ADMIN role always returns false")
    void hasPermission_nonAdmin_alwaysFalse() {
        User student = User.builder().id(1L).email("a_s@kbtu.kz").fullName("S")
                .role(User.UserRole.STUDENT)
                .adminPermissions(EnumSet.allOf(User.AdminPermission.class))
                .enabled(true).build();

        for (User.AdminPermission perm : User.AdminPermission.values()) {
            org.assertj.core.api.Assertions.assertThat(student.hasPermission(perm)).isFalse();
        }
    }

    @Test
    @DisplayName("hasPermission - specific permission granted without SUPER")
    void hasPermission_specificPermission() {
        User admin = User.builder().id(1L).email("admin@x.kz").fullName("A")
                .role(User.UserRole.ADMIN)
                .adminPermissions(EnumSet.of(User.AdminPermission.FINANCE))
                .enabled(true).build();

        org.assertj.core.api.Assertions.assertThat(admin.hasPermission(User.AdminPermission.FINANCE)).isTrue();
        org.assertj.core.api.Assertions.assertThat(admin.hasPermission(User.AdminPermission.REGISTRAR)).isFalse();
    }
}
