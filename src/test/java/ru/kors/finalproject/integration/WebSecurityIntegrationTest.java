package ru.kors.finalproject.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests verifying web security configuration for the API-only backend.
 *
 * The application is a stateless JWT REST API. Non-API routes redirect to the
 * frontend SPA. Session-based login pages are NOT served by this backend.
 *
 * Tests verify:
 * - Non-API root routes redirect to the frontend (3xx), not 404
 * - Actuator / Swagger are accessible without a token
 * - /api/v1/auth/** endpoints are public (return 4xx for bad input, never 403)
 * - /api/v1/student/** blocks unauthenticated requests (403)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WebSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    // -------------------------------------------------------------------------
    // Non-API routes → redirect to frontend SPA
    // -------------------------------------------------------------------------

    @Test
    void homePage_redirectsToFrontend() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void loginRoute_redirectsToFrontend() throws Exception {
        // The backend does not serve a login page; it redirects to the SPA
        mockMvc.perform(get("/login"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void registerRoute_redirectsToFrontend() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void newsRoute_redirectsToFrontend() throws Exception {
        mockMvc.perform(get("/news"))
                .andExpect(status().is3xxRedirection());
    }

    // -------------------------------------------------------------------------
    // Infrastructure endpoints — accessible without auth
    // -------------------------------------------------------------------------

    @Test
    void actuatorHealth_isAccessible() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void swaggerUi_isAccessible() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }

    // -------------------------------------------------------------------------
    // Auth API — public, returns 4xx for bad input (never 403)
    // -------------------------------------------------------------------------

    @Test
    void apiV1Auth_login_returnsClientError_forInvalidCredentials() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content("{\"email\":\"nonexistent@test.com\",\"password\":\"wrong123\"}"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void apiV1Auth_login_isNotForbidden_withoutToken() throws Exception {
        // Auth endpoint must be publicly accessible — must NOT return 403
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content("{\"email\":\"a@b.kz\",\"password\":\"123456\"}"))
                .andExpect(result ->
                        org.assertj.core.api.Assertions.assertThat(
                                result.getResponse().getStatus()).isNotEqualTo(403));
    }

    // -------------------------------------------------------------------------
    // Protected API endpoints — 403 without token
    // -------------------------------------------------------------------------

    @Test
    void apiV1Student_returnsForbidden_withoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/student/profile"))
                .andExpect(status().isForbidden());
    }

    @Test
    void apiV1Teacher_returnsForbidden_withoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/teacher/sections"))
                .andExpect(status().isForbidden());
    }

    @Test
    void apiV1Admin_returnsForbidden_withoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    void apiV1Chat_returnsForbidden_withoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/chat/rooms"))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------
    // Public API endpoints — accessible without auth
    // -------------------------------------------------------------------------

    @Test
    void apiV1Public_news_isAccessible_withoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/public/news"))
                .andExpect(status().isOk());
    }
}
