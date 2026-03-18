package ru.kors.finalproject.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for UserRoleDetector email-based role detection logic.
 * Covers student (a_name@), professor (z.name@), admin (admin@), and unknown patterns.
 */
class UserRoleDetectorTest {

    private final UserRoleDetector detector = new UserRoleDetector();

    // -------------------------------------------------------------------------
    // Student detection
    // -------------------------------------------------------------------------

    @ParameterizedTest(name = "student email [{0}]")
    @CsvSource({
            "a_mustafayev@kbtu.kz",
            "b_smith@example.com",
            "z_jones@university.edu",
            "A_UPPER@kbtu.kz",          // uppercase normalised to lowercase
    })
    @DisplayName("detectRole - returns STUDENT for emails with underscore as second char")
    void detectRole_student(String email) {
        assertThat(detector.detectRole(email)).isEqualTo(UserRole.STUDENT);
    }

    // -------------------------------------------------------------------------
    // Professor detection
    // -------------------------------------------------------------------------

    @ParameterizedTest(name = "professor email [{0}]")
    @CsvSource({
            "z.professor@kbtu.kz",
            "a.smith@example.com",
            "D.jones@university.edu",
    })
    @DisplayName("detectRole - returns PROFESSOR for emails with period as second char")
    void detectRole_professor(String email) {
        assertThat(detector.detectRole(email)).isEqualTo(UserRole.PROFESSOR);
    }

    // -------------------------------------------------------------------------
    // Admin detection
    // -------------------------------------------------------------------------

    @ParameterizedTest(name = "admin email [{0}]")
    @CsvSource({
            "admin@kbtu.kz",
            "admin.registrar@kbtu.kz",
            "ADMIN@kbtu.kz",
    })
    @DisplayName("detectRole - returns ADMIN for emails starting with 'admin'")
    void detectRole_admin(String email) {
        assertThat(detector.detectRole(email)).isEqualTo(UserRole.ADMIN);
    }

    // -------------------------------------------------------------------------
    // Unknown / invalid patterns
    // -------------------------------------------------------------------------

    @ParameterizedTest(name = "unknown email [{0}]")
    @CsvSource({
            "x@kbtu.kz",          // single-char local part (length < 2)
            "someone@kbtu.kz",    // no separator after first char
            "foo+bar@kbtu.kz",
    })
    @DisplayName("detectRole - returns UNKNOWN for emails without recognised pattern")
    void detectRole_unknown(String email) {
        assertThat(detector.detectRole(email)).isEqualTo(UserRole.UNKNOWN);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("detectRole - returns UNKNOWN for null or blank input")
    void detectRole_nullOrBlank(String email) {
        assertThat(detector.detectRole(email)).isEqualTo(UserRole.UNKNOWN);
    }

    @Test
    @DisplayName("detectRole - handles email without domain (@-less string)")
    void detectRole_noDomain() {
        // Just a local-part string without @: treated as the full local part
        assertThat(detector.detectRole("a_name")).isEqualTo(UserRole.STUDENT);
        assertThat(detector.detectRole("z.prof")).isEqualTo(UserRole.PROFESSOR);
    }
}
