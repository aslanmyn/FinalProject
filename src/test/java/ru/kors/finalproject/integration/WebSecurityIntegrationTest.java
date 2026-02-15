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
 * Integration tests verifying web security configuration:
 * - Public pages accessible without login
 * - Protected pages redirect to login
 * - Login/register endpoints work
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WebSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void homePage_isAccessible() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk());
    }

    @Test
    void loginPage_isAccessible() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk());
    }

    @Test
    void registerPage_isAccessible() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk());
    }

    @Test
    void newsPage_isAccessible() throws Exception {
        mockMvc.perform(get("/news"))
                .andExpect(status().isOk());
    }

    @Test
    void actuatorHealth_isAccessible() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void portalPage_redirectsToLogin_whenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/portal/student-information"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void adminDashboard_redirectsToLogin_whenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void professorDashboard_redirectsToLogin_whenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/professor/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void apiV1Auth_login_returnsClientError_forInvalidCredentials() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content("{\"email\":\"nonexistent@test.com\",\"password\":\"wrong\"}"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void apiV1Student_returnsForbidden_withoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/student/profile"))
                .andExpect(status().isForbidden());
    }

    @Test
    void swaggerUi_isAccessible() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }

    @Test
    void cssFile_isAccessible() throws Exception {
        mockMvc.perform(get("/css/style.css"))
                .andExpect(status().isOk());
    }

    @Test
    void jsFile_isAccessible() throws Exception {
        mockMvc.perform(get("/js/app.js"))
                .andExpect(status().isOk());
    }
}
