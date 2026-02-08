package ru.kors.finalproject.service;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import ru.kors.finalproject.entity.*;
import ru.kors.finalproject.repository.*;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PortalDataService {
    private final SessionService sessionService;
    private final NewsRepository newsRepository;
    private final StudentRequestRepository studentRequestRepository;
    private final SurveyRepository surveyRepository;
    private final SurveyResponseRepository surveyResponseRepository;
    private final GradeRepository gradeRepository;
    private final AttendanceRepository attendanceRepository;
    private final FinancialService financialService;
    private final ChargeRepository chargeRepository;
    private final PaymentRepository paymentRepository;
    private final RegistrationRepository registrationRepository;
    private final MobilityApplicationRepository mobilityApplicationRepository;
    private final ClearanceSheetRepository clearanceSheetRepository;
    private final ChecklistItemRepository checklistItemRepository;
    private final StudentFileRepository studentFileRepository;
    private final ExamScheduleRepository examScheduleRepository;
    private final SocialActivityRepository socialActivityRepository;
    private final FxRegistrationRepository fxRegistrationRepository;
    private final SubjectOfferingRepository subjectOfferingRepository;
    private final SemesterRepository semesterRepository;

    public boolean loadData(String slug, HttpSession session, Model model) {
        var student = sessionService.getCurrentStudent(session);
        if (student.isEmpty()) return false;
        Student s = student.get();

        model.addAttribute("userEmail", sessionService.getEmail(session));
        model.addAttribute("userRole", sessionService.getRole(session));
        model.addAttribute("student", s);
        model.addAttribute("financialLock", financialService.hasRegistrationLock(s));

        return switch (slug) {
            case "student-information" -> loadStudentInfo(s, model);
            case "news" -> false; // redirect
            case "student-requests" -> loadStudentRequests(s, model);
            case "surveys" -> loadSurveys(s, model);
            case "student-journal" -> loadStudentJournal(s, model);
            case "student-financial-account" -> loadFinancialAccount(s, model);
            case "student-schedule" -> loadStudentSchedule(s, model);
            case "course-schedule" -> loadCourseSchedule(model);
            case "assessment-results" -> loadAssessmentResults(s, model);
            case "class-attendance" -> loadAttendance(s, model);
            case "student-exam-schedule" -> loadExamSchedule(model);
            case "academic-mobility" -> loadMobility(s, model);
            case "clearance-sheet" -> loadClearance(s, model);
            case "checklist" -> loadChecklist(s, model);
            case "student-files" -> loadStudentFiles(s, model);
            case "transcript" -> loadTranscript(s, model);
            case "social-transcript" -> loadSocialTranscript(s, model);
            case "fx-registration" -> loadFxRegistration(s, model);
            case "course-registration" -> loadCourseRegistration(s, model);
            case "student-personal-data" -> true;
            case "student-financial-cabinet" -> loadStudentFinancialCabinet(s, model);
            default -> true;
        };
    }

    private boolean loadStudentInfo(Student s, Model model) {
        model.addAttribute("balance", financialService.getBalance(s));
        return true;
    }

    private boolean loadStudentRequests(Student s, Model model) {
        model.addAttribute("requests", studentRequestRepository.findByStudentIdOrderByCreatedAtDesc(s.getId()));
        return true;
    }

    private boolean loadSurveys(Student s, Model model) {
        var surveys = surveyRepository.findAll().stream()
                .filter(surv -> java.time.LocalDate.now().isBefore(surv.getEndDate()) && java.time.LocalDate.now().isAfter(surv.getStartDate().minusDays(1)))
                .toList();
        var completedIds = surveys.stream()
                .filter(surv -> surveyResponseRepository.findBySurveyIdAndStudentId(surv.getId(), s.getId()).isPresent())
                .map(Survey::getId)
                .toList();
        model.addAttribute("surveys", surveys);
        model.addAttribute("completedSurveyIds", completedIds);
        return true;
    }

    private boolean loadStudentJournal(Student s, Model model) {
        if (s.getCurrentSemester() == null) return true;
        model.addAttribute("grades", gradeRepository.findByStudentIdAndSubjectOffering_SemesterId(s.getId(), s.getCurrentSemester().getId()));
        return true;
    }

    private boolean loadFinancialAccount(Student s, Model model) {
        model.addAttribute("charges", chargeRepository.findByStudentIdOrderByDueDateDesc(s.getId()));
        model.addAttribute("payments", paymentRepository.findByStudentIdOrderByDateDesc(s.getId()));
        model.addAttribute("balance", financialService.getBalance(s));
        return true;
    }

    private boolean loadStudentSchedule(Student s, Model model) {
        model.addAttribute("registrations", registrationRepository.findByStudentIdWithDetails(s.getId()));
        return true;
    }

    private boolean loadCourseSchedule(Model model) {
        var sem = semesterRepository.findByCurrentTrue();
        if (sem.isPresent()) {
            model.addAttribute("offerings", subjectOfferingRepository.findBySemesterIdWithDetails(sem.get().getId()));
        }
        return true;
    }

    private boolean loadAssessmentResults(Student s, Model model) {
        if (s.getCurrentSemester() == null) return true;
        model.addAttribute("grades", gradeRepository.findByStudentIdAndSubjectOffering_SemesterId(s.getId(), s.getCurrentSemester().getId()));
        return true;
    }

    private boolean loadAttendance(Student s, Model model) {
        model.addAttribute("registrations", registrationRepository.findByStudentIdWithDetails(s.getId()));
        return true;
    }

    private boolean loadExamSchedule(Model model) {
        var sem = semesterRepository.findByCurrentTrue();
        if (sem.isPresent()) {
            model.addAttribute("exams", examScheduleRepository.findBySubjectOffering_SemesterIdOrderByExamDateAsc(sem.get().getId()));
        }
        return true;
    }

    private boolean loadMobility(Student s, Model model) {
        model.addAttribute("applications", mobilityApplicationRepository.findByStudentIdOrderByCreatedAtDesc(s.getId()));
        return true;
    }

    private boolean loadClearance(Student s, Model model) {
        clearanceSheetRepository.findByStudentId(s.getId()).ifPresent(cs -> model.addAttribute("clearanceSheet", cs));
        return true;
    }

    private boolean loadChecklist(Student s, Model model) {
        model.addAttribute("items", checklistItemRepository.findByStudentIdOrderByDeadlineAsc(s.getId()));
        return true;
    }

    private boolean loadStudentFiles(Student s, Model model) {
        model.addAttribute("files", studentFileRepository.findByStudentIdOrderByUploadedAtDesc(s.getId()));
        return true;
    }

    private boolean loadTranscript(Student s, Model model) {
        model.addAttribute("registrations", registrationRepository.findByStudentIdWithDetails(s.getId()));
        model.addAttribute("grades", gradeRepository.findByStudentId(s.getId()));
        return true;
    }

    private boolean loadSocialTranscript(Student s, Model model) {
        model.addAttribute("activities", socialActivityRepository.findByStudentIdOrderByDateDesc(s.getId()));
        return true;
    }

    private boolean loadFxRegistration(Student s, Model model) {
        model.addAttribute("fxRegistrations", fxRegistrationRepository.findByStudentIdOrderByCreatedAtDesc(s.getId()));
        return true;
    }

    private boolean loadCourseRegistration(Student s, Model model) {
        model.addAttribute("registrations", registrationRepository.findByStudentIdWithDetails(s.getId()));
        return true;
    }

    private boolean loadStudentFinancialCabinet(Student s, Model model) {
        model.addAttribute("balance", financialService.getBalance(s));
        return true;
    }
}
