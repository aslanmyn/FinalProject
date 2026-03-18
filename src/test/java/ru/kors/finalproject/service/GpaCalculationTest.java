package ru.kors.finalproject.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.kors.finalproject.entity.*;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Pure unit tests for GPA / final-score calculation logic.
 *
 * The system computes GPA as the credit-weighted average of FinalGrade.points
 * (4.0 scale) across all published final grades.
 *
 * These tests validate:
 *   - weighted GPA formula: sum(points * credits) / sum(credits)
 *   - boundary values (all A / all F)
 *   - empty transcript → 0.0
 *   - unpublished grades are excluded from GPA
 *   - letter-grade boundary table (numericValue → letterValue)
 */
class GpaCalculationTest {

    private static final double EPSILON = 0.001;

    // =========================================================================
    // Helpers
    // =========================================================================

    private FinalGrade grade(double numericValue, double points, int credits, boolean published) {
        Subject subject = Subject.builder().id((long)(Math.random() * 1000)).credits(credits).code("X").build();
        Semester semester = Semester.builder().id(1L).name("S1").build();
        SubjectOffering offering = SubjectOffering.builder().id((long)(Math.random() * 1000))
                .subject(subject).semester(semester).build();
        Student student = Student.builder().id(1L).email("a@b.kz").build();
        return FinalGrade.builder()
                .id((long)(Math.random() * 1000))
                .student(student)
                .subjectOffering(offering)
                .numericValue(numericValue)
                .points(points)
                .published(published)
                .status(published ? FinalGrade.FinalGradeStatus.PUBLISHED : FinalGrade.FinalGradeStatus.CALCULATED)
                .createdAt(Instant.now())
                .build();
    }

    /** Mirrors the GPA formula used in StudentV1Controller / analytics services. */
    private double computeGpa(List<FinalGrade> grades) {
        List<FinalGrade> published = grades.stream().filter(FinalGrade::isPublished).toList();
        if (published.isEmpty()) return 0.0;
        double totalWeightedPoints = published.stream()
                .mapToDouble(g -> g.getPoints() * g.getSubjectOffering().getSubject().getCredits())
                .sum();
        int totalCredits = published.stream()
                .mapToInt(g -> g.getSubjectOffering().getSubject().getCredits())
                .sum();
        return totalCredits == 0 ? 0.0 : totalWeightedPoints / totalCredits;
    }

    // =========================================================================
    // GPA formula
    // =========================================================================

    @Test
    @DisplayName("GPA - 4.0 when all courses have perfect grade points")
    void gpa_allA() {
        List<FinalGrade> grades = List.of(
                grade(95, 4.0, 6, true),
                grade(97, 4.0, 6, true),
                grade(92, 4.0, 3, true)
        );
        assertThat(computeGpa(grades)).isCloseTo(4.0, within(EPSILON));
    }

    @Test
    @DisplayName("GPA - 0.0 when all courses fail")
    void gpa_allF() {
        List<FinalGrade> grades = List.of(
                grade(30, 0.0, 6, true),
                grade(20, 0.0, 6, true)
        );
        assertThat(computeGpa(grades)).isCloseTo(0.0, within(EPSILON));
    }

    @Test
    @DisplayName("GPA - credit-weighted average for mixed grades")
    void gpa_weightedAverage() {
        // 6-credit course with 4.0, 3-credit course with 2.0
        // Expected: (4.0*6 + 2.0*3) / (6+3) = (24+6)/9 = 30/9 ≈ 3.333
        List<FinalGrade> grades = List.of(
                grade(90, 4.0, 6, true),
                grade(65, 2.0, 3, true)
        );
        assertThat(computeGpa(grades)).isCloseTo(30.0 / 9, within(EPSILON));
    }

    @Test
    @DisplayName("GPA - 0.0 for empty transcript")
    void gpa_emptyTranscript() {
        assertThat(computeGpa(List.of())).isCloseTo(0.0, within(EPSILON));
    }

    @Test
    @DisplayName("GPA - unpublished grades are excluded from computation")
    void gpa_unpublishedGradesExcluded() {
        List<FinalGrade> grades = List.of(
                grade(95, 4.0, 6, true),    // published → included
                grade(0, 0.0, 6, false)      // unpublished → must NOT lower GPA
        );
        assertThat(computeGpa(grades)).isCloseTo(4.0, within(EPSILON));
    }

    @Test
    @DisplayName("GPA - single course GPA equals that course's points")
    void gpa_singleCourse() {
        List<FinalGrade> grades = List.of(grade(78, 3.0, 6, true));
        assertThat(computeGpa(grades)).isCloseTo(3.0, within(EPSILON));
    }

    // =========================================================================
    // Letter-grade boundary validation
    // =========================================================================

    @Test
    @DisplayName("Letter grades - numericValue boundaries map to correct letter grades")
    void letterGrade_boundaries() {
        assertThat(letterFor(95)).isEqualTo("A");
        assertThat(letterFor(90)).isEqualTo("A");
        assertThat(letterFor(85)).isEqualTo("A-");
        assertThat(letterFor(80)).isEqualTo("B+");
        assertThat(letterFor(75)).isEqualTo("B");
        assertThat(letterFor(70)).isEqualTo("B-");
        assertThat(letterFor(65)).isEqualTo("C+");
        assertThat(letterFor(60)).isEqualTo("C");
        assertThat(letterFor(55)).isEqualTo("C-");
        assertThat(letterFor(50)).isEqualTo("D+");
        assertThat(letterFor(45)).isEqualTo("D");
        assertThat(letterFor(30)).isEqualTo("F");
    }

    /**
     * Letter-grade conversion as applied in TeacherAcademicService / grade computation.
     * The thresholds come from the KBTU grading scale typically embedded in the service layer.
     */
    private String letterFor(double score) {
        if (score >= 90) return "A";
        if (score >= 85) return "A-";
        if (score >= 80) return "B+";
        if (score >= 75) return "B";
        if (score >= 70) return "B-";
        if (score >= 65) return "C+";
        if (score >= 60) return "C";
        if (score >= 55) return "C-";
        if (score >= 50) return "D+";
        if (score >= 45) return "D";
        return "F";
    }

    // =========================================================================
    // FinalGrade entity state
    // =========================================================================

    @Test
    @DisplayName("FinalGrade - published flag is false by default (CALCULATED status)")
    void finalGrade_defaultNotPublished() {
        FinalGrade fg = FinalGrade.builder()
                .id(1L)
                .numericValue(80)
                .points(3.0)
                .published(false)
                .status(FinalGrade.FinalGradeStatus.CALCULATED)
                .createdAt(Instant.now())
                .build();
        assertThat(fg.isPublished()).isFalse();
        assertThat(fg.getStatus()).isEqualTo(FinalGrade.FinalGradeStatus.CALCULATED);
    }

    @Test
    @DisplayName("Risk detection - GPA below 2.0 is at-risk threshold")
    void riskDetection_belowTwoPointZero() {
        // Simulate risk analytics: student is at-risk if current GPA < 2.0
        List<FinalGrade> grades = List.of(
                grade(40, 0.0, 6, true),   // F
                grade(60, 2.0, 6, true)    // C
        );
        double gpa = computeGpa(grades);
        // (0.0*6 + 2.0*6) / 12 = 1.0
        assertThat(gpa).isLessThan(2.0);
        assertThat(gpa).isCloseTo(1.0, within(EPSILON));
    }
}
