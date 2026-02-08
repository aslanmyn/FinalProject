package ru.kors.finalproject.model;

import org.springframework.web.util.HtmlUtils;

import java.util.List;

public record PortalSection(String slug, String title, String description) {

    public static final List<PortalSection> ALL = List.of(
        new PortalSection("add-drop-courses", "Add/Drop Courses",
            "Management of changes in the curriculum within the permitted time frame.\n\n" +
            "Functions: show available subjects for the current semester (by program, prerequisites, place limits), add a discipline (checks: the add/drop period is open, there is no schedule conflict, prerequisites are completed, the credit limit is not exceeded, there are no debts/locks, the group is not overcrowded), delete a discipline (checks: the period is open, there is no prohibition after a certain date), recording the result in \"registrations\", logging actions, notifying the student."),
        new PortalSection("academic-mobility", "Academic Mobility",
            "Applications for exchange/transfer of subjects from another university.\n\n" +
            "Functions: creating an application for mobility, uploading documents (invitation, learning agreement), selecting disciplines \"there\" and mapping to disciplines \"with us\", approval (workflow: student → coordinator/office), statuses and comments, deadline calendar, final credit after completion (credits transfer)."),
        new PortalSection("surveys", "Surveys",
            "Survey of the quality of teaching/courses/services.\n\n" +
            "Functions: a list of available surveys (usually for current courses), completion (scale/text questions), restrictions: once, anonymity, access only during the period, sending and storing answers, confirmation of participation, uploading aggregated results to the administration (without personal data)."),
        new PortalSection("student-journal", "Student Journal",
            "Diary/journal of academic performance in disciplines: grades by components, deadlines, attendance (if conducted), teacher comments.\n\n" +
            "Functions: viewing by semester, detailing by discipline (all categories: quizzes, midterm, final, lab), calculation of the current percentage, notifications of new grades, export to PDF (optional)."),
        new PortalSection("student-requests", "Student Requests",
            "A single application cabinet: references, academic questions, retakes, data changes, payment issues.\n\n" +
            "Functions: create an appeal (category + description), attach files, appointment to the responsible department, statuses (new/in review/need info/approved/rejected/done), correspondence in the thread, SLA/deadlines, response templates, request history."),
        new PortalSection("student-information", "Student Information",
            "Summary home page: student status, program, faculty, course, group, current semester, credits, academic status (active/on leave), important locks/holds (finances/documents), office contacts.\n\n" +
            "Features: read-only card + sitelinks, notifications and announcements."),
        new PortalSection("student-financial-account", "Student Financial Account",
            "Financial part: charges, payments, arrears, discounts/grants.\n\n" +
            "Functions: list of invoices/charges by period, details of \"for what\", balance, payment history, statuses (paid/partial/overdue), download receipts, integration with the payment gateway (online payment), notifications of debt and registration locks."),
        new PortalSection("student-personal-data", "Student Personal Data",
            "Profile: passport/ID, address, contacts, emergency contact, photo, language, bank details (if necessary).\n\n" +
            "Functions: viewing and requesting changes to data (often via approval), validations, revision history, uploading documents for confirmation (if required)."),
        new PortalSection("news", "News",
            "University/faculty news feed.\n\n" +
            "Functions: news list, categories/filters, search, news viewing, attachments, \"read\" mark, push/email notifications for important announcements (optional)."),
        new PortalSection("clearance-sheet", "Clearance Sheet",
            "Release/discharge/transfer workaround: library, dormitory, finance, department, etc.\n\n" +
            "Functions: creating a process (when a student meets the condition), checkpoints with those responsible, statuses for each item (pending/approved/rejected), comments, requirements to \"close debts\", final status \"cleared\", PDF generation."),
        new PortalSection("class-attendance", "Class Attendance",
            "Attendance by class.\n\n" +
            "Functions: view attendance by discipline/date, marks (present/late/absent), reasons (if any), percentages of attendance, warnings in case of low attendance, integration: either the teacher marks or the student marks by QR/GEO (if provided)."),
        new PortalSection("assessment-results", "Assessment Results",
            "Final certification results: midterm/final, final grade, GPA/credits earned.\n\n" +
            "Functions: viewing by semester, detailing grades and scales, statuses (passed/failed/incomplete), applications for appeal/retake (if business rules allow), export to PDF."),
        new PortalSection("course-schedule", "Course Schedule",
            "Schedule of disciplines (by groups/streams): when and where the subject takes place.\n\n" +
            "Functions: search by discipline/teacher/audience, view by week, data on the type of lesson (lecture/practice/lab), links to LMS/materials (optional), notifications of changes."),
        new PortalSection("student-schedule", "Student Schedule",
            "The student's personal schedule, compiled from his registrations.\n\n" +
            "Functions: week/day calendar, conflicts and cancellations, adding reminders, exporting to iCal/Google Calendar (optional), \"today's activities\", notifications."),
        new PortalSection("student-exam-schedule", "Student Exam Schedule",
            "Separate exam schedule.\n\n" +
            "Functions: Exam list (date/time/audience/format), admission rules (e.g. financial/academic hold), notifications, export/print."),
        new PortalSection("fx-registration", "FX Registration",
            "Registration for FX (usually \"Final Exam/Final attestation/retake\", depends on your university).\n\n" +
            "Functions: show the available FX options (by disciplines where allowed), creating an application for FX, checks (right to FX, deadlines, payment/penalty, attempts), bill/payment generation, registration confirmation, reflection in the exam schedule."),
        new PortalSection("course-registration", "Course Registration",
            "Basic registration for the semester's disciplines.\n\n" +
            "Functions: selection of disciplines and sections, checking the credit limit, prerequisites, schedule conflicts, program restrictions, confirmation of registration, formation of an individual plan, \"draft/submitted/confirmed\" statuses, possible queues/waiting for places."),
        new PortalSection("social-transcript", "Social Transcript",
            "The social transcript is usually about extracurricular activities: volunteering, clubs, achievements, events.\n\n" +
            "Functions: activity portfolio, requests for adding an activity, confirmation by the organizer, points/hours, categories, document/certificate generation."),
        new PortalSection("transcript", "Transcript",
            "Official Academic Transcript: all semesters, disciplines, grades, GPA, credits.\n\n" +
            "Functions: view, filter by period, download PDF, request the \"official version\" (signed/stamped) via Student Requests, access protection (student only), download audit log."),
        new PortalSection("student-files", "Student Files",
            "Student's file storage: references, documents, applications, confirmations, contracts.\n\n" +
            "Functions: upload/download, categories, role access (the student sees his own), retention periods, secure links, antivirus/checks (if necessary)."),
        new PortalSection("student-financial-cabinet", "Student Financial Cabinet",
            "Expanded \"financial dashboard\": not only accruals, but also a contract, installment plan, scholarship, grant, bonuses, refund requests.\n\n" +
            "Functions: contract/statuses, payment plan, payment documents, applications for discounts/benefits, refunds, financial transaction history, integration with accounting."),
        new PortalSection("checklist", "Checklist",
            "Checklist of student's tasks for the semester: what needs to be done (register, pay, take surveys, submit documents).\n\n" +
            "Functions: a list of tasks with deadlines, automatic generation of tasks based on events (registration opened — a task appeared), progress, reminders, links to relevant sections.")
    );

    public static PortalSection findBySlug(String slug) {
        return ALL.stream()
            .filter(s -> s.slug().equalsIgnoreCase(slug))
            .findFirst()
            .orElse(null);
    }

    public String getDescriptionHtml() {
        if (description == null) return "";
        return HtmlUtils.htmlEscape(description)
            .replace("\n", "<br>")
            .replace("Functions:", "<strong>Functions:</strong>")
            .replace("Features:", "<strong>Features:</strong>");
    }
}
