package ru.kors.finalproject.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
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
    void professorDashboard_isAccessible_whenProfessorSessionExists() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userEmail", "z.professor@kbtu.kz");
        session.setAttribute("userRole", "PROFESSOR");
        session.setAttribute("fullName", "Dr. Z. Professor");

        mockMvc.perform(get("/professor/dashboard").session(session))
                .andExpect(status().isOk());
    }

    @Test
    void professorCourses_isAccessible_whenProfessorSessionExists() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userEmail", "z.professor@kbtu.kz");
        session.setAttribute("userRole", "PROFESSOR");

        mockMvc.perform(get("/professor/courses").session(session))
                .andExpect(status().isOk());
    }

    @Test
    void professorCourseDetails_isAccessible_whenProfessorSessionExists() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userEmail", "z.professor@kbtu.kz");
        session.setAttribute("userRole", "PROFESSOR");

        mockMvc.perform(get("/professor/course/1").session(session))
                .andExpect(status().isOk());
    }

    @Test
    void professorExportGrades_isAccessible_whenProfessorSessionExists() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userEmail", "z.professor@kbtu.kz");
        session.setAttribute("userRole", "PROFESSOR");

        mockMvc.perform(get("/professor/course/1/export-grades").session(session))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("attachment; filename=grades_")))
                .andExpect(content().contentTypeCompatibleWith(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
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

    @Test
    void home_redirectsToStudentPortal_whenStudentSessionExists() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userEmail", "a_mustafayev@kbtu.kz");
        session.setAttribute("userRole", "STUDENT");

        mockMvc.perform(get("/").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/news"));
    }

    @Test
    void portalPage_isAccessible_whenStudentSessionExists() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userEmail", "a_mustafayev@kbtu.kz");
        session.setAttribute("userRole", "STUDENT");

        mockMvc.perform(get("/portal/student-information").session(session))
                .andExpect(status().isOk());
    }
}
