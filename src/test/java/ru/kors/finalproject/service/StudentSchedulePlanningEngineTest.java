package ru.kors.finalproject.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StudentSchedulePlanningEngineTest {

    @Test
    @DisplayName("Generic Russian 'after 12' preference should not be mistaken for Thursday")
    void genericRussianAfterNoonPreferenceAppliesToWholeSchedule() {
        List<StudentSchedulePlanningEngine.CourseOption> sections = List.of(
                new StudentSchedulePlanningEngine.CourseOption(
                        1L,
                        "CSCI2104",
                        "Databases",
                        "Professor Rustam Serikbayev",
                        "LECTURE",
                        25,
                        List.of(new StudentSchedulePlanningEngine.MeetingOption("MONDAY", "08:00", "10:00", "L-427"))
                ),
                new StudentSchedulePlanningEngine.CourseOption(
                        2L,
                        "CSCI2104",
                        "Databases",
                        "Professor Rustam Serikbayev",
                        "LECTURE",
                        25,
                        List.of(new StudentSchedulePlanningEngine.MeetingOption("MONDAY", "13:00", "15:00", "L-427"))
                ),
                new StudentSchedulePlanningEngine.CourseOption(
                        3L,
                        "CSCI2105",
                        "Algorithms and Data Structures",
                        "Professor Aidos Nurgaliyev",
                        "LECTURE",
                        25,
                        List.of(new StudentSchedulePlanningEngine.MeetingOption("TUESDAY", "09:00", "11:00", "L-428"))
                ),
                new StudentSchedulePlanningEngine.CourseOption(
                        4L,
                        "CSCI2105",
                        "Algorithms and Data Structures",
                        "Professor Aidos Nurgaliyev",
                        "LECTURE",
                        25,
                        List.of(new StudentSchedulePlanningEngine.MeetingOption("TUESDAY", "14:00", "16:00", "L-428"))
                )
        );

        StudentSchedulePlanningEngine.PlanningResult result = StudentSchedulePlanningEngine.plan(
                "привет можешь сделать расписание на следующий семестр я хочу чтобы уроки были после 12 часов",
                sections
        );

        assertThat(result.selectedSectionIds()).containsExactlyInAnyOrder(2L, 4L);
        assertThat(result.unsatisfiedPreferences()).isEmpty();
    }

    @Test
    @DisplayName("Russian request for free days and 3-4 study days should be recognized")
    void russianFreeDaysPreferenceIsRecognized() {
        List<StudentSchedulePlanningEngine.CourseOption> sections = List.of(
                new StudentSchedulePlanningEngine.CourseOption(
                        11L,
                        "CSCI2104",
                        "Databases",
                        "Professor Rustam Serikbayev",
                        "LECTURE",
                        25,
                        List.of(new StudentSchedulePlanningEngine.MeetingOption("MONDAY", "08:00", "10:00", "L-427"))
                ),
                new StudentSchedulePlanningEngine.CourseOption(
                        12L,
                        "CSCI2105",
                        "Algorithms and Data Structures",
                        "Professor Aidos Nurgaliyev",
                        "LECTURE",
                        25,
                        List.of(new StudentSchedulePlanningEngine.MeetingOption("MONDAY", "10:00", "12:00", "L-428"))
                ),
                new StudentSchedulePlanningEngine.CourseOption(
                        13L,
                        "INFT2102",
                        "IT Infrastructure and Computer Networks",
                        "Professor Aidos Nurgaliyev",
                        "LECTURE",
                        25,
                        List.of(new StudentSchedulePlanningEngine.MeetingOption("TUESDAY", "08:00", "10:00", "L-418"))
                ),
                new StudentSchedulePlanningEngine.CourseOption(
                        14L,
                        "INFT2205",
                        "Web Development",
                        "Professor Rustam Serikbayev",
                        "LECTURE",
                        25,
                        List.of(new StudentSchedulePlanningEngine.MeetingOption("THURSDAY", "12:00", "14:00", "L-382"))
                )
        );

        StudentSchedulePlanningEngine.PlanningResult result = StudentSchedulePlanningEngine.plan(
                "сможешь мне сделать расписание на следующий семестр так чтобы несколько дней были свободны сможешь умести все уроки на 3 дня или на 4 дня",
                sections
        );

        assertThat(result.satisfiedPreferences()).contains("Не больше 4 учебных дней");
        assertThat(result.unsatisfiedPreferences()).doesNotContain("Не больше 4 учебных дней");
    }
}
