package ru.kors.finalproject.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.kors.finalproject.entity.FinalGrade;
import ru.kors.finalproject.entity.Semester;
import ru.kors.finalproject.entity.Student;
import ru.kors.finalproject.entity.Subject;
import ru.kors.finalproject.entity.SubjectOffering;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class GpaCalculationTest {

    private static final double EPSILON = 0.001;
    private final GpaCalculationService gpaCalculationService = new GpaCalculationService();

    private FinalGrade grade(double numericValue, double points, int credits, boolean published) {
        Subject subject = Subject.builder().id((long) (Math.random() * 1000)).credits(credits).code("X").build();
        Semester semester = Semester.builder().id(1L).name("S1").build();
        SubjectOffering offering = SubjectOffering.builder().id((long) (Math.random() * 1000))
                .subject(subject).semester(semester).build();
        Student student = Student.builder().id(1L).email("a@b.kz").build();
        return FinalGrade.builder()
                .id((long) (Math.random() * 1000))
                .student(student)
                .subjectOffering(offering)
                .numericValue(numericValue)
                .points(points)
                .published(published)
                .status(published ? FinalGrade.FinalGradeStatus.PUBLISHED : FinalGrade.FinalGradeStatus.CALCULATED)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("GPA - 4.0 when all courses have perfect grade points")
    void gpa_allA() {
        List<FinalGrade> grades = List.of(
                grade(95, 4.0, 6, true),
                grade(97, 4.0, 6, true),
                grade(92, 4.0, 3, true)
        );

        assertThat(gpaCalculationService.calculatePublishedGpa(grades)).isCloseTo(4.0, within(EPSILON));
    }

    @Test
    @DisplayName("GPA - 0.0 when all courses fail")
    void gpa_allF() {
        List<FinalGrade> grades = List.of(
                grade(30, 0.0, 6, true),
                grade(20, 0.0, 6, true)
        );

        assertThat(gpaCalculationService.calculatePublishedGpa(grades)).isCloseTo(0.0, within(EPSILON));
    }

    @Test
    @DisplayName("GPA - credit-weighted average for mixed grades")
    void gpa_weightedAverage() {
        List<FinalGrade> grades = List.of(
                grade(90, 4.0, 6, true),
                grade(65, 2.0, 3, true)
        );

        assertThat(gpaCalculationService.calculatePublishedGpa(grades)).isCloseTo(30.0 / 9.0, within(EPSILON));
    }

    @Test
    @DisplayName("GPA - 0.0 for empty transcript")
    void gpa_emptyTranscript() {
        assertThat(gpaCalculationService.calculatePublishedGpa(List.of())).isCloseTo(0.0, within(EPSILON));
    }

    @Test
    @DisplayName("GPA - unpublished grades are excluded from computation")
    void gpa_unpublishedGradesExcluded() {
        List<FinalGrade> grades = List.of(
                grade(95, 4.0, 6, true),
                grade(0, 0.0, 6, false)
        );

        assertThat(gpaCalculationService.calculatePublishedGpa(grades)).isCloseTo(4.0, within(EPSILON));
    }

    @Test
    @DisplayName("GPA - single course GPA equals that course's points")
    void gpa_singleCourse() {
        List<FinalGrade> grades = List.of(grade(78, 3.0, 6, true));

        assertThat(gpaCalculationService.calculatePublishedGpa(grades)).isCloseTo(3.0, within(EPSILON));
    }

    @Test
    @DisplayName("Weighted GPA helper ignores zero-credit rows")
    void weightedGpa_ignoresZeroCreditRows() {
        double gpa = gpaCalculationService.calculateWeightedGpa(List.of(
                new GpaCalculationService.CreditPoints(4.0, 6),
                new GpaCalculationService.CreditPoints(1.0, 0)
        ));

        assertThat(gpa).isCloseTo(4.0, within(EPSILON));
    }

    @Test
    @DisplayName("Weighted GPA helper returns 0.0 when all rows have no effective credits")
    void weightedGpa_zeroWhenNoCredits() {
        double gpa = gpaCalculationService.calculateWeightedGpa(List.of(
                new GpaCalculationService.CreditPoints(4.0, 0),
                new GpaCalculationService.CreditPoints(2.0, 0)
        ));

        assertThat(gpa).isCloseTo(0.0, within(EPSILON));
    }

    @Test
    @DisplayName("Risk detection - GPA below 2.0 stays under the threshold")
    void riskDetection_belowTwoPointZero() {
        List<FinalGrade> grades = List.of(
                grade(40, 0.0, 6, true),
                grade(60, 2.0, 6, true)
        );

        double gpa = gpaCalculationService.calculatePublishedGpa(grades);

        assertThat(gpa).isLessThan(2.0);
        assertThat(gpa).isCloseTo(1.0, within(EPSILON));
    }
}
