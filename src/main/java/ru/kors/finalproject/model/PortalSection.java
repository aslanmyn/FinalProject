package ru.kors.finalproject.model;

import java.util.List;

public record PortalSection(String slug, String title) {

    public static final List<PortalSection> ALL = List.of(
        new PortalSection("add-drop-courses", "Add/Drop Courses"),
        new PortalSection("academic-mobility", "Academic Mobility"),
        new PortalSection("surveys", "Surveys"),
        new PortalSection("student-journal", "Student Journal"),
        new PortalSection("student-requests", "Student Requests"),
        new PortalSection("student-information", "Student Information"),
        new PortalSection("student-financial-account", "Student Financial Account"),
        new PortalSection("student-personal-data", "Student Personal Data"),
        new PortalSection("news", "News"),
        new PortalSection("clearance-sheet", "Clearance Sheet"),
        new PortalSection("class-attendance", "Class Attendance"),
        new PortalSection("assessment-results", "Assessment Results"),
        new PortalSection("course-schedule", "Course Schedule"),
        new PortalSection("student-schedule", "Student Schedule"),
        new PortalSection("student-exam-schedule", "Student Exam Schedule"),
        new PortalSection("fx-registration", "FX Registration"),
        new PortalSection("course-registration", "Course Registration"),
        new PortalSection("social-transcript", "Social Transcript"),
        new PortalSection("transcript", "Transcript"),
        new PortalSection("student-files", "Student Files"),
        new PortalSection("student-financial-cabinet", "Student Financial Cabinet"),
        new PortalSection("checklist", "Checklist")
    );

    public static PortalSection findBySlug(String slug) {
        return ALL.stream()
            .filter(s -> s.slug().equalsIgnoreCase(slug))
            .findFirst()
            .orElse(null);
    }
}
